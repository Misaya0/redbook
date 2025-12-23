package com.itcast.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.client.UserClient;
import com.itcast.constant.RedisConstant;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.pojo.Note;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.strategy.GetNotesStrategy;
import com.itcast.strategy.NoteStrategyContext;
import com.itcast.strategy.NoteStrategyType;
import com.itcast.context.UserContext;
import com.itcast.util.BloomFilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GetNoteList implements GetNotesStrategy {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    @Override
    public NoteStrategyType getStrategyType() {
        return NoteStrategyType.DEFAULT;
    }

    @Override
    public Result<List<NoteVo>> getNotes(NoteStrategyContext context) {
        // 兜底分页参数，避免传入非法值导致分页插件异常
        Integer page = context.getPage();
        Integer pageSize = context.getPageSize();
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 构造查询条件：按类型筛选
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        if (context.getType() != null && !context.getType().isEmpty()) {
            queryWrapper.eq(Note::getType, context.getType());
        }

        // 固定排序，保证多页连续拉取时结果稳定，避免重复/跳过
        queryWrapper.orderByDesc(Note::getTime).orderByDesc(Note::getId);

        // 登录用户ID：用于布隆过滤；未登录则不做过滤（全部视为未读）
        Integer loginUserId = UserContext.getUserId();

        // selectedNotes：最终选中的笔记集合，目标尽量补齐到 pageSize
        List<Note> selectedNotes = new ArrayList<>(pageSize);
        int currentPage = page;

        // “补齐 pageSize”逻辑：每次查一页 -> 过滤 -> 追加，直到凑满或数据耗尽
        while (selectedNotes.size() < pageSize) {
            // 每轮按固定 pageSize 拉取一页数据
            Page<Note> notePage = noteMapper.selectPage(new Page<>(currentPage, pageSize), queryWrapper);
            if (notePage == null) {
                break;
            }
            List<Note> records = notePage.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }

            // 对当前页记录进行布隆过滤：过滤掉可能已读的笔记（保持 mightContain == true 为已读语义）
            for (Note note : records) {
                if (selectedNotes.size() >= pageSize) {
                    break;
                }
                if (loginUserId != null) {
                    boolean isRead = bloomFilterUtil.mightContain(
                            RedisConstant.USER_BLOOM_FILTER + loginUserId,
                            String.valueOf(note.getId()));
                    if (isRead) {
                        continue;
                    }
                }
                selectedNotes.add(note);
            }

            if (selectedNotes.size() >= pageSize) {
                break;
            }

            // 明确退出条件，防止死循环：当前页不足一页/已到最后一页时停止继续拉取
            long totalPages = notePage.getPages();
            if (records.size() < pageSize) {
                break;
            }
            if (totalPages > 0 && currentPage >= totalPages) {
                break;
            }

            currentPage++;
        }

        if (selectedNotes.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 批量填充用户信息：避免对每条 Note 进行 N+1 远程调用
        Map<Integer, User> userMap = getUserMap(selectedNotes);
        List<NoteVo> noteVoList = selectedNotes.stream()
                .limit(pageSize)
                .map(note -> {
                    NoteVo noteVo = new NoteVo();
                    BeanUtils.copyProperties(note, noteVo);
                    // 远程获取用户信息失败时允许为空，不抛异常中断
                    if (note.getUserId() != null) {
                        noteVo.setUser(userMap.get(note.getUserId()));
                    }
                    return noteVo;
                })
                .collect(Collectors.toList());

        return Result.success(noteVoList);
    }

    /**
     * 批量获取用户信息并构建 userId -> User 的映射。
     * 约束：
     * 1) 单次请求尽量只调用一次用户服务批量接口；
     * 2) 远程调用失败时不中断流程，返回空 Map（noteVo.user 允许为空）。
     */
    private Map<Integer, User> getUserMap(List<Note> notes) {
        List<Integer> userIds = notes.stream()
                .map(Note::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // 通过批量接口一次性获取所有用户信息（消除 N+1）
            Result<List<User>> result = userClient.getUsersByIds(userIds);
            List<User> users = result == null ? null : result.getData();
            if (users == null || users.isEmpty()) {
                return new HashMap<>();
            }
            return users.stream()
                    .filter(Objects::nonNull)
                    .filter(u -> u.getId() != null)
                    .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            // 日志输出遵循规范，不使用 System.err.println，且不中断主流程
            String preview = userIds.size() <= 20 ? userIds.toString() : userIds.subList(0, 20).toString();
            log.error("批量获取用户信息失败, userIdsSize: {}, userIdsPreview: {}", userIds.size(), preview, e);
            return new HashMap<>();
        }
    }
}

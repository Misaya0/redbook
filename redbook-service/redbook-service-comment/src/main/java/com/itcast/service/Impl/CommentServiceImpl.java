package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcast.client.NoteClient;
import com.itcast.client.UserClient;
import com.itcast.mapper.CommentMapper;
import com.itcast.model.dto.CommentDto;
import com.itcast.model.event.MessageEvent;
import com.itcast.model.pojo.Comment;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.CommentVo;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.service.CommentService;
import com.itcast.context.UserContext;
import com.itcast.util.DealTimeUtil;
import com.itcast.util.DiffDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.itcast.mapper.CommentLikeMapper;
import com.itcast.model.pojo.CommentLike;
import java.util.Set;
import java.util.HashSet;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private NoteClient noteClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Result<Void> postComment(CommentDto commentDto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("获取用户信息失败，请确认是否登录");
        }
        Comment comment = new Comment();
        BeanUtils.copyProperties(commentDto, comment);
        comment.setUserId(userId);
        comment.setTime(LocalDateTime.now());
        comment.setLikeCount(0);

        // 处理回复逻辑
        if (comment.getParentId() != null && comment.getParentId() != 0) {
            Comment parentComment = commentMapper.selectById(comment.getParentId());
            if (parentComment != null) {
                // 设置回复目标用户
                comment.setTargetUserId(parentComment.getUserId());

                // 如果回复的是子评论，则将新评论挂在根评论下，实现扁平化
                if (parentComment.getParentId() != 0) {
                    comment.setParentId(parentComment.getParentId());
                }
            }
        }

        commentMapper.insert(comment);

        // 发送通知消息
        try {
            MessageEvent event = new MessageEvent();
            event.setEventId(java.util.UUID.randomUUID().toString());
            event.setActorId(userId);
            event.setTimestamp(System.currentTimeMillis());
            event.setContentBrief(comment.getContent().length() > 50 ? comment.getContent().substring(0, 50) : comment.getContent());

            if (comment.getParentId() != null && comment.getParentId() != 0) {
                // 回复评论
                if (comment.getTargetUserId() != null && !comment.getTargetUserId().equals(userId)) {
                    event.setRecipientId(comment.getTargetUserId());
                    event.setType("REPLY");
                    event.setTargetType("COMMENT");
                    event.setTargetId(comment.getId());
                    rabbitTemplate.convertAndSend(com.itcast.constant.MqConstant.MESSAGE_NOTICE_EXCHANGE, "comment.replied", event);
                    log.info("发送回复评论消息");
                }
            } else {
                // 评论笔记
                event.setType("COMMENT");
                event.setTargetType("NOTE");
                event.setTargetId(comment.getNoteId());
                // 查询笔记作者
                Result<NoteVo> noteResult = noteClient.getNote(comment.getNoteId());
                if (noteResult != null && noteResult.getData() != null) {
                    Long noteAuthorId = noteResult.getData().getUserId();
                    if (!noteAuthorId.equals(userId)) {
                        event.setRecipientId(noteAuthorId);
                        rabbitTemplate.convertAndSend(com.itcast.constant.MqConstant.MESSAGE_NOTICE_EXCHANGE, "note.commented", event);
                        log.info("发送评论笔记消息");
                    }
                }
            }
        } catch (Exception e) {
            log.error("发送评论通知失败", e);
        }

        return Result.success(null);
    }

    @Override
    public Result<List<CommentVo>> getCommentList(Long noteId) {
        List<CommentVo> commentVoList = new ArrayList<>();
        
        // 获取当前登录用户点赞过的评论ID集合
        Set<Long> likedCommentIds = new HashSet<>();
        Long userId = UserContext.getUserId();
        if (userId != null) {
            List<CommentLike> likes = commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getUserId, userId));
            likes.forEach(like -> likedCommentIds.add(like.getCommentId()));
        }

        // 1.根据笔记id获取所有评论列表
        List<Comment> commentList = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getNoteId, noteId)
                .orderByDesc(Comment::getId));

        Map<Long, User> userCache = new HashMap<>();
        Set<Long> needQueryUserIds = new HashSet<>();
        for (Comment comment : commentList) {
            if (comment.getUserId() != null) {
                needQueryUserIds.add(comment.getUserId());
            }
            if (comment.getTargetUserId() != null) {
                needQueryUserIds.add(comment.getTargetUserId());
            }
        }
        for (Long needQueryUserId : needQueryUserIds) {
            try {
                Result<User> userResult = userClient.getUserById(needQueryUserId);
                if (userResult != null && userResult.getData() != null) {
                    userCache.put(needQueryUserId, userResult.getData());
                } else {
                    userCache.put(needQueryUserId, null);
                }
            } catch (Exception e) {
                log.error("获取用户信息失败 userId:{}", needQueryUserId, e);
                userCache.put(needQueryUserId, null);
            }
        }

        // 2.根据parentId=0获取所有一级评论列表
        List<Comment> parentList = commentList.stream()
                .filter(comment -> comment.getParentId() != null && comment.getParentId() == 0)
                .collect(Collectors.toList());
        // 3.构建树模型
        int totalCount = commentList.size();
        for (Comment comment : parentList) {
            CommentVo commentVo = convertToCommentVo(comment, likedCommentIds, userCache);
            commentVo.setChildrenList(buildChildrenList(comment.getId(), commentList, likedCommentIds, userCache));
            commentVo.setTotalCount(totalCount);
            commentVoList.add(commentVo);
        }
        return Result.success(commentVoList);
    }

    @Override
    public Result<Integer> getCommentCount(Long noteId) {
        Integer count = Math.toIntExact(commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getNoteId, noteId)));
        return Result.success(count);
    }

    @Override
    public Result<Void> likeComment(Long commentId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }

        // 检查是否已经点赞
        CommentLike existLike = commentLikeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getUserId, userId)
                .eq(CommentLike::getCommentId, commentId));

        if (existLike != null) {
            return Result.success(null); // 已经点赞过了
        }

        // 插入点赞记录
        CommentLike commentLike = new CommentLike();
        commentLike.setUserId(userId);
        commentLike.setCommentId(commentId);
        commentLikeMapper.insert(commentLike);

        // 更新评论点赞数
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
        }

        return Result.success(null);
    }

    @Override
    public Result<Void> unlikeComment(Long commentId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }

        // 删除点赞记录
        commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getUserId, userId)
                .eq(CommentLike::getCommentId, commentId));

        // 更新评论点赞数
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentMapper.updateById(comment);
        }

        return Result.success(null);
    }

    /**
     * 实体转换成vo
     * @param comment
     * @param likedCommentIds
     * @return
     * @throws ParseException
     */
    private CommentVo convertToCommentVo(Comment comment, Set<Long> likedCommentIds, Map<Long, User> userCache) {
        String dealTime = "";
        try {
            if (comment.getTime() != null) {
                long days = ChronoUnit.DAYS.between(comment.getTime().toLocalDate(), LocalDateTime.now().toLocalDate());
                dealTime = DealTimeUtil.dealTime((int) days);
            }
        } catch (Exception e) {
            dealTime = comment.getTime() != null ? comment.getTime().toString() : "";
        }
        
        CommentVo commentVo = new CommentVo();
        BeanUtils.copyProperties(comment, commentVo);
        commentVo.setDealTime(dealTime);
        commentVo.setCommentVoId(comment.getId().toString());
        
        // 设置点赞状态
        if (likedCommentIds != null && likedCommentIds.contains(comment.getId())) {
            commentVo.setIsLiked(true);
        } else {
            commentVo.setIsLiked(false);
        }

        // 设置用户信息
        if (comment.getUserId() != null) {
            User user = userCache.get(comment.getUserId());
            commentVo.setUser(user);
        }

        // 设置回复目标用户昵称
        if (comment.getTargetUserId() != null) {
            User targetUser = userCache.get(comment.getTargetUserId());
            if (targetUser != null) {
                commentVo.setReplyToUser(targetUser.getNickname());
            }
        }

        return commentVo;
    }

    /**
     * 递归构建评论列表
     * @param parentId
     * @param commentList
     * @param likedCommentIds
     * @return
     * @throws ParseException
     */
    private List<CommentVo> buildChildrenList(Long parentId, List<Comment> commentList, Set<Long> likedCommentIds, Map<Long, User> userCache) {
        List<CommentVo> childrenVoList = new ArrayList<>();
        for (Comment comment : commentList) {
            if (parentId.equals(comment.getParentId())) {
                CommentVo commentVo = convertToCommentVo(comment, likedCommentIds, userCache);
                commentVo.setChildrenList(buildChildrenList(comment.getId(), commentList, likedCommentIds, userCache));
                childrenVoList.add(commentVo);
            }
        }
        return childrenVoList;
    }

}

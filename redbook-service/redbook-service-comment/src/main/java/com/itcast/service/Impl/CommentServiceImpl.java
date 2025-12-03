package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcast.client.UserClient;
import com.itcast.mapper.CommentMapper;
import com.itcast.model.dto.CommentDto;
import com.itcast.model.pojo.Comment;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.CommentVo;
import com.itcast.result.Result;
import com.itcast.service.CommentService;
import com.itcast.context.UserContext;
import com.itcast.util.DealTimeUtil;
import com.itcast.util.DiffDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    @Override
    public Result<Void> postComment(CommentDto commentDto) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("获取用户信息失败，请确认是否登录");
        }
        Comment comment = new Comment();
        BeanUtils.copyProperties(commentDto, comment);
        comment.setUserId(userId);
        comment.setTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
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
        return Result.success(null);
    }

    @Override
    public Result<List<CommentVo>> getCommentList(Long noteId) throws ParseException {
        List<CommentVo> commentVoList = new ArrayList<>();
        
        // 获取当前登录用户点赞过的评论ID集合
        Set<Long> likedCommentIds = new HashSet<>();
        Integer userId = UserContext.getUserId();
        if (userId != null) {
            List<CommentLike> likes = commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getUserId, userId));
            likes.forEach(like -> likedCommentIds.add(like.getCommentId()));
        }

        // 1.根据笔记id获取所有评论列表
        List<Comment> commentList = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getNoteId, noteId)
                .orderByDesc(Comment::getId));
        // 2.根据parentId=0获取所有一级评论列表
        List<Comment> parentList = commentList.stream()
                .filter(comment -> comment.getParentId() != null && comment.getParentId() == 0)
                .collect(Collectors.toList());
        // 3.构建树模型
        for (Comment comment : parentList) {
            CommentVo commentVo = convertToCommentVo(comment, likedCommentIds);
            commentVo.setChildrenList(buildChildrenList(comment.getId(), commentList, likedCommentIds));
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
        Integer userId = UserContext.getUserId();
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
        Integer userId = UserContext.getUserId();
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
    private CommentVo convertToCommentVo(Comment comment, Set<Long> likedCommentIds) throws ParseException {
        String dealTime = "";
        try {
             dealTime = DealTimeUtil.dealTime(
                    DiffDayUtil.diffDays(
                            new SimpleDateFormat("yyyy-MM-dd").parse(comment.getTime()), new Date()));
        } catch (Exception e) {
             dealTime = comment.getTime();
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
            try {
                Result<User> userResult = userClient.getUserById(comment.getUserId());
                if (userResult != null && userResult.getData() != null) {
                    commentVo.setUser(userResult.getData());
                }
            } catch (Exception e) {
                log.error("获取用户信息失败 userId:{}", comment.getUserId(), e);
            }
        }

        // 设置回复目标用户昵称
        if (comment.getTargetUserId() != null) {
            try {
                Result<User> targetUserResult = userClient.getUserById(comment.getTargetUserId());
                if (targetUserResult != null && targetUserResult.getData() != null) {
                    commentVo.setReplyToUser(targetUserResult.getData().getNickname());
                }
            } catch (Exception e) {
                log.error("获取回复目标用户信息失败 targetUserId:{}", comment.getTargetUserId(), e);
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
    private List<CommentVo> buildChildrenList(Long parentId, List<Comment> commentList, Set<Long> likedCommentIds) throws ParseException {
        List<CommentVo> childrenVoList = new ArrayList<>();
        for (Comment comment : commentList) {
            if (parentId.equals(comment.getParentId())) {
                CommentVo commentVo = convertToCommentVo(comment, likedCommentIds);
                commentVo.setChildrenList(buildChildrenList(comment.getId(), commentList, likedCommentIds));
                childrenVoList.add(commentVo);
            }
        }
        return childrenVoList;
    }

}

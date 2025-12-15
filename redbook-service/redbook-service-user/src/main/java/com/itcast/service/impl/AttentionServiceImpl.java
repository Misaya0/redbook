package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcast.constant.MqConstant;
import com.itcast.constant.RedisConstant;
import com.itcast.enums.AttentionTypeEnum;
import com.itcast.enums.MessageTypeEnum;
import com.itcast.mapper.AttentionMapper;
import com.itcast.mapper.UserMapper;
import com.itcast.model.pojo.Message;
import com.itcast.model.pojo.Attention;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.AttentionVo;
import com.itcast.result.Result;
import com.itcast.service.AttentionService;
import com.itcast.context.UserContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttentionServiceImpl implements AttentionService {

    @Autowired
    private AttentionMapper attentionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Result<Integer> isAttention(Integer otherId) {
        int flag;
        // 1.获取登录用户id
        Integer ownId = UserContext.getUserId();
        // 2.判断是否是自己
        if (otherId.intValue() == ownId.intValue()) {
            flag = AttentionTypeEnum.OWN.getCode();
        } else if(Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(RedisConstant.ATTENTION_CACHE + ownId, otherId))) {
            flag = AttentionTypeEnum.ATTENTION.getCode();
        } else {
            flag = AttentionTypeEnum.INATTENTION.getCode();
        }
        return Result.success(flag);
    }

    @Override
    public Result<Void> attention(Integer otherId) {
        // 1.获取登录用户id
        Integer ownId = UserContext.getUserId();
        // 2.设置pojo
        Attention attention = new Attention();
        attention.setOtherId(otherId);
        attention.setOwnId(ownId);
        // 3.判断是否关注过
        Boolean isAttention = redisTemplate.opsForSet().isMember(RedisConstant.ATTENTION_CACHE + ownId, otherId);

        // 4.关注或取关
        if(Boolean.TRUE.equals(isAttention)){
            // 4.1 取关
            LambdaQueryWrapper<Attention> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Attention::getOwnId, ownId).eq(Attention::getOtherId, otherId);
            attentionMapper.delete(queryWrapper);
            redisTemplate.opsForSet().remove(RedisConstant.ATTENTION_CACHE + ownId, otherId);
        }else{
            // 4.2 关注
            attention.setTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            attentionMapper.insert(attention);
            redisTemplate.opsForSet().add(RedisConstant.ATTENTION_CACHE + ownId, otherId);

            // 用户关注，通知消息
            com.itcast.model.event.MessageEvent event = new com.itcast.model.event.MessageEvent();
            event.setEventId(java.util.UUID.randomUUID().toString());
            event.setActorId(ownId.longValue());
            event.setRecipientId(otherId.longValue());
            event.setType("FOLLOW");
            event.setTargetType("USER");
            event.setTargetId(otherId.longValue());
            event.setTimestamp(System.currentTimeMillis());
            event.setContentBrief("关注了你");
            
            rabbitTemplate.convertAndSend(MqConstant.MESSAGE_NOTICE_EXCHANGE, "user.followed", event);
        }
        return Result.success(null);
    }

    @Override
    public Result<List<AttentionVo>> getAttention(Integer userId) {
        LambdaQueryWrapper<Attention> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attention::getOwnId, userId);
        List<Attention> attentionList = attentionMapper.selectList(queryWrapper);
        return getAttentionVos(attentionList, true);
    }

    @Override
    public Result<List<AttentionVo>> getFans(Integer userId) {
        LambdaQueryWrapper<Attention> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attention::getOtherId, userId);
        List<Attention> attentionList = attentionMapper.selectList(queryWrapper);
        return getAttentionVos(attentionList, false);
    }

    private Result<List<AttentionVo>> getAttentionVos(List<Attention> attentionList, boolean isAttentionList) {
        if (attentionList == null || attentionList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<Integer> userIds = attentionList.stream()
                .map(a -> isAttentionList ? a.getOtherId() : a.getOwnId())
                .collect(Collectors.toList());

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<AttentionVo> vos = attentionList.stream().map(a -> {
            AttentionVo vo = new AttentionVo();
            Integer targetId = isAttentionList ? a.getOtherId() : a.getOwnId();
            User user = userMap.get(targetId);
            if (user != null) {
                vo.setUserId(user.getId());
                vo.setNickname(user.getNickname());
                vo.setImage(user.getImage());
            } else {
                vo.setUserId(targetId);
                vo.setNickname("用户已注销");
            }
            vo.setTime(a.getTime());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(vos);
    }
}

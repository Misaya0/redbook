package com.itcast.mq.consumer;

import com.google.gson.Gson;
import com.itcast.constant.MqConstant;
import com.itcast.model.pojo.Message;
import com.itcast.model.pojo.Collection;
import com.itcast.model.pojo.Like;
import com.itcast.model.pojo.Attention;
import com.itcast.session.Session;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageNoticeConsumer {

    @RabbitListener(queues = {MqConstant.LOGIN_NOTICE_QUEUE})
    public void loginNotice(Long userId) {
        log.info("登录用户的ID：{}", userId);
    }

    @RabbitListener(queues = MqConstant.ATTENTION_NOTICE_QUEUE)
    public void attentionNotice(Message<Attention> message) {
        Attention attention = message.getObj();
        log.info("关注用户的ID：{}, 关注的人ID：{}",attention.getOwnId(), attention.getOtherId());
        Channel channel = Session.getChannel(message.getNoticeId());
        if (channel != null) {
            log.info("用户{}的channel不为空,执行关注通知操作", message.getNoticeId());
            String json = new Gson().toJson(message);
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }

    @RabbitListener(queues = MqConstant.LIKE_NOTICE_QUEUE)
    public void likeNotice(Message<Like> message) {
        Like like = message.getObj();
        log.info("点赞用户的ID：{}, 点赞的笔记ID：{}", like.getUserId(), like.getNoteId());
        Channel channel = Session.getChannel(message.getNoticeId());
        if (channel != null) {
            log.info("用户{}的channel不为空,执行点赞通知操作", message.getNoticeId());
            String json = new Gson().toJson(message);
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }

    @RabbitListener(queues = MqConstant.COLLECTION_NOTICE_QUEUE)
    public void collectionNotice(Message<Collection> message) {
        Collection collection = message.getObj();
        log.info("收藏用户的ID：{}, 收藏的笔记ID：{}", collection.getUserId(), collection.getNoteId());
        Channel channel = Session.getChannel(message.getNoticeId());
        if (channel != null) {
            log.info("用户{}的channel不为空,执行收藏通知操作", message.getNoticeId());
            String json = new Gson().toJson(message);
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }
}

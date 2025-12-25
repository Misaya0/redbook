package com.itcast.session;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private static final ConcurrentHashMap<Long, Channel> userIdChannelMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Channel, Long> channelUserIdMap = new ConcurrentHashMap<>();

    public static void bind(Long userId, Channel channel) {
        userIdChannelMap.put(userId, channel);
        channelUserIdMap.put(channel, userId);
    }

    public static void unbind(Long userId, Channel channel) {
        userIdChannelMap.remove(userId);
        channelUserIdMap.remove(channel);
    }

    public static Long getUserId(Channel channel) {
        return channelUserIdMap.get(channel);
    }

    public static Channel getChannel(Long userId) {
        return userIdChannelMap.get(userId);
    }
}

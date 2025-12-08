package com.itcast.model.vo;

import com.itcast.entity.Conversation;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationVo extends Conversation {
    /**
     * 目标用户昵称
     */
    private String targetName;

    /**
     * 目标用户头像
     */
    private String targetAvatar;
}

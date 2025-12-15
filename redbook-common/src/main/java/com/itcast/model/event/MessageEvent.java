package com.itcast.model.event;

import lombok.Data;
import java.io.Serializable;

@Data
public class MessageEvent implements Serializable {
    private String eventId;
    private Long actorId;
    private Long recipientId;
    private String type; // FOLLOW, LIKE, COLLECT, COMMENT, REPLY
    private String targetType; // USER, NOTE, COMMENT
    private Long targetId;
    private String contentBrief;
    private Long timestamp;
}

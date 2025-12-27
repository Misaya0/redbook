package com.itcast.model.pojo;

import com.itcast.enums.LogType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class BehaviorMessage implements Serializable {
    private Long userId;
    private Long noteId;
    private LocalDateTime viewTime;
    private LocalDateTime leaveTime;
    private LocalDateTime likeTime;
    private LogType logType;
}

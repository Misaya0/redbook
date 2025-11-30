package com.itcast.controller;

import com.itcast.model.pojo.Topic;
import com.itcast.result.Result;
import com.itcast.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "话题模块", description = "笔记话题相关接口")
@RestController
@RequestMapping("/note")
public class TopicController {

    @Autowired
    private TopicService topicService;

    @Operation(summary = "获取话题列表", description = "获取所有可用的话题列表")
    @GetMapping("/getTopics")
    public Result<List<Topic>> getTopics() {
        return topicService.getTopics();
    }
}

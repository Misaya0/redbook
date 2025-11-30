package com.itcast.controller;

import com.itcast.result.Result;
import com.itcast.service.HotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "热搜模块", description = "热搜榜单相关接口")
@RestController
@RequestMapping("/search")
public class HotController {

    @Autowired
    private HotService hotService;


    @Operation(summary = "获取热搜榜", description = "获取当前热门搜索关键词榜单")
    @GetMapping("/getHotList")
    public Result<List<Map<String, Object>>> getHotList() {
        return hotService.getHotList();
    }
}

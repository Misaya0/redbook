package com.itcast.controller;

import com.itcast.result.Result;
import com.itcast.model.pojo.History;
import com.itcast.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "搜索历史模块", description = "搜索历史管理相关接口")
@RestController
@RequestMapping("/search")
public class HistoryController {

    @Autowired
    private HistoryService historyService;


    @Operation(summary = "获取搜索历史", description = "获取当前用户的搜索历史列表")
    @GetMapping("/getHistoryList")
    public Result<List<History>> getHistoryList() {
        return historyService.getHistoryList();
    }

    @Operation(summary = "删除搜索历史", description = "清空当前用户的搜索历史")
    @DeleteMapping("/deleteHistory")
    public Result<Void> deleteHistory() {
        return historyService.deleteHistory();
    }
}

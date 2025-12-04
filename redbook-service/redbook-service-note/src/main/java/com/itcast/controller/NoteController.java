package com.itcast.controller;

import com.itcast.model.dto.NoteDto;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.service.NoteService;
import com.itcast.strategy.NoteStrategyContext;
import com.itcast.strategy.NoteStrategyType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@Tag(name = "笔记模块", description = "笔记增删改查相关接口")
@RestController
@RequestMapping("/note")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @Operation(summary = "获取笔记列表", description = "分页获取笔记列表（默认策略）")
    @GetMapping("/getNoteList")
    public Result<List<NoteVo>> getNoteList(
            @Parameter(description = "页码", required = true) @RequestParam("page") Integer page,
            @Parameter(description = "每页数量", required = true) @RequestParam("pageSize") Integer pageSize,
            @Parameter(description = "笔记类型", required = false) @RequestParam(value = "type", required = false) String type) {
        NoteStrategyContext context = NoteStrategyContext.builder()
                .page(page)
                .pageSize(pageSize)
                .type(type)
                .build();
        return noteService.getNotes(NoteStrategyType.DEFAULT, context);
    }

    @Operation(summary = "按地理位置获取笔记", description = "根据经纬度获取附近的笔记列表")
    @GetMapping("/getNoteListByLocation")
    public Result<List<NoteVo>> getNoteListByLocation(
            @Parameter(description = "经度", required = true) @RequestParam("longitude") String longitude,
            @Parameter(description = "纬度", required = true) @RequestParam("latitude") String latitude) {
        NoteStrategyContext context = NoteStrategyContext.builder()
                .longitude(longitude)
                .latitude(latitude)
                .build();
        return noteService.getNotes(NoteStrategyType.BY_LOCATION, context);
    }

    @Operation(summary = "获取自己的笔记", description = "获取当前用户发布的所有笔记")
    @GetMapping("/getNoteListByOwn")
    public Result<List<NoteVo>> getNoteListByOwn() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_OWN, context);
    }

    @Operation(summary = "按浏览量获取笔记", description = "获取热门笔记列表（按浏览量排序）")
    @GetMapping("/getNoteListByScan")
    public Result<List<NoteVo>> getNoteListByScan() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_SCAN, context);
    }

    @Operation(summary = "获取关注人的笔记", description = "获取当前用户关注的人发布的笔记")
    @GetMapping("/getNoteListByAttention")
    public Result<List<NoteVo>> getNoteListByAttention() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_ATTENTION, context);
    }

    @Operation(summary = "获取收藏的笔记", description = "获取当前用户收藏的所有笔记")
    @GetMapping("/getNoteByCollection")
    public Result<List<NoteVo>> getNoteByCollection() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_COLLECTION, context);
    }

    @Operation(summary = "获取点赞的笔记", description = "获取当前用户点赞过的所有笔记")
    @GetMapping("/getNoteByLike")
    public Result<List<NoteVo>> getNoteByLike() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_LIKE, context);
    }

    @Operation(summary = "获取笔记详情", description = "根据笔记ID获取笔记的详细信息")
    @GetMapping("/getNote/{noteId}")
    public Result<NoteVo> getNote(
            @Parameter(description = "笔记ID", required = true) @PathVariable("noteId") Long noteId) throws ParseException {
        return noteService.getNote(noteId);
    }

    @Operation(summary = "发布笔记", description = "发布一篇新的笔记，包含图片、标题、内容和位置信息")
    @PostMapping("/postNote")
    public Result<Void> postNote(
            @Parameter(description = "笔记图片", required = true) @RequestParam("image") MultipartFile file,
            @Parameter(description = "笔记标题", required = true) @RequestParam("title") String title,
            @Parameter(description = "笔记内容", required = true) @RequestParam("content") String content,
            @Parameter(description = "经度", required = true) @RequestParam("longitude") String longitude,
            @Parameter(description = "纬度", required = true) @RequestParam("latitude") String latitude,
            @Parameter(description = "笔记类型", required = false) @RequestParam(value = "type", required = false) String type) throws IOException, InterruptedException {
        NoteDto dto = new NoteDto();
        dto.setFile(file);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setType(type);
        dto.setLongitude(Double.valueOf(longitude));
        dto.setLatitude(Double.valueOf(latitude));
        dto.setLike(0);
        dto.setCollection(0);
        return noteService.postNote(dto);
    }

    @Operation(summary = "更新笔记", description = "更新现有笔记的内容")
    @PutMapping("/updateNote")
    public Result<Void> updateNote(
            @Parameter(description = "笔记ID", required = true) @RequestParam("id") Long id,
            @Parameter(description = "笔记图片", required = false) @RequestParam(value = "image", required = false) MultipartFile file,
            @Parameter(description = "笔记标题", required = true) @RequestParam("title") String title,
            @Parameter(description = "笔记内容", required = true) @RequestParam("content") String content,
            @Parameter(description = "笔记类型", required = false) @RequestParam(value = "type", required = false) String type,
            @Parameter(description = "经度", required = false) @RequestParam(value = "longitude", required = false) String longitude,
            @Parameter(description = "纬度", required = false) @RequestParam(value = "latitude", required = false) String latitude) throws IOException {
        NoteDto dto = new NoteDto();
        dto.setId(id);
        dto.setFile(file);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setType(type);
        if (longitude != null && !longitude.isEmpty()) dto.setLongitude(Double.valueOf(longitude));
        if (latitude != null && !latitude.isEmpty()) dto.setLatitude(Double.valueOf(latitude));
        return noteService.updateNote(dto);
    }

    @Operation(summary = "删除笔记", description = "根据笔记ID删除笔记")
    @DeleteMapping("/deleteNote/{noteId}")
    public Result<Void> deleteNote(
            @Parameter(description = "笔记ID", required = true) @PathVariable("noteId") Long noteId) {
        return noteService.deleteNote(noteId);
    }

    @Operation(summary = "批量删除笔记", description = "根据笔记ID列表批量删除笔记")
    @DeleteMapping("/batchDeleteNotes")
    public Result<Void> batchDeleteNotes(
            @Parameter(description = "笔记ID列表", required = true) @RequestBody List<Long> noteIds) {
        return noteService.batchDeleteNotes(noteIds);
    }
}





















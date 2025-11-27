package com.itcast.controller;

import com.itcast.model.dto.NoteDto;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.service.NoteService;
import com.itcast.strategy.NoteStrategyContext;
import com.itcast.strategy.NoteStrategyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/note")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @GetMapping("/getNoteList")
    public Result<List<NoteVo>> getNoteList(
            @RequestParam("page") Integer page, @RequestParam("pageSize") Integer pageSize) {
        NoteStrategyContext context = NoteStrategyContext.builder()
                .page(page)
                .pageSize(pageSize)
                .build();
        return noteService.getNotes(NoteStrategyType.DEFAULT, context);
    }

    @GetMapping("/getNoteListByLocation")
    public Result<List<NoteVo>> getNoteListByLocation(
            @RequestParam("longitude") String longitude, @RequestParam("latitude") String latitude) {
        NoteStrategyContext context = NoteStrategyContext.builder()
                .longitude(longitude)
                .latitude(latitude)
                .build();
        return noteService.getNotes(NoteStrategyType.BY_LOCATION, context);
    }

    @GetMapping("/getNoteListByOwn")
    public Result<List<NoteVo>> getNoteListByOwn() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_OWN, context);
    }

    @GetMapping("/getNoteListByScan")
    public Result<List<NoteVo>> getNoteListByScan() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_SCAN, context);
    }

    @GetMapping("/getNoteListByAttention")
    public Result<List<NoteVo>> getNoteListByAttention() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_ATTENTION, context);
    }

    @GetMapping("/getNoteByCollection")
    public Result<List<NoteVo>> getNoteByCollection() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_COLLECTION, context);
    }

    @GetMapping("/getNoteByLike")
    public Result<List<NoteVo>> getNoteByLike() {
        NoteStrategyContext context = NoteStrategyContext.builder().build();
        return noteService.getNotes(NoteStrategyType.BY_LIKE, context);
    }

    @GetMapping("/getNote/{noteId}")
    public Result<NoteVo> getNote(@PathVariable("noteId") Long noteId) throws ParseException {
        return noteService.getNote(noteId);
    }

    @PostMapping("/postNote")
    public Result<Void> postNote(@RequestParam("image") MultipartFile file,
                                 @RequestParam("title") String title,
                                 @RequestParam("content") String content,
                                 @RequestParam("longitude") String longitude,
                                 @RequestParam("latitude") String latitude) throws IOException, InterruptedException {
        // 构造dto
        NoteDto dto = new NoteDto();
        dto.setFile(file);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setLongitude(Double.valueOf(longitude));
        dto.setLatitude(Double.valueOf(latitude));
        dto.setLike(0);
        dto.setCollection(0);
        return noteService.postNote(dto);
    }
}





















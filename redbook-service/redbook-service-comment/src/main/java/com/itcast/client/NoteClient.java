package com.itcast.client;

import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("redbook-service-note")
public interface NoteClient {

    @GetMapping("/note/getNote/{noteId}")
    Result<NoteVo> getNote(@PathVariable("noteId") Long noteId);
}

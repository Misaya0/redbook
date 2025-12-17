package com.itcast.client;

import com.itcast.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "redbook-service-note")
public interface NoteClient {

    @GetMapping("/note/getNote/{noteId}")
    Result<Map<String, Object>> getNote(@PathVariable("noteId") Long noteId);
}

package com.itcast.client;

import com.itcast.model.vo.NoteSimpleVo;
import com.itcast.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("redbook-service-note")
public interface NoteClient {

    @GetMapping("/note/getRelatedNotes/{productId}")
    Result<List<NoteSimpleVo>> getRelatedNotes(@PathVariable("productId") Long productId);
}

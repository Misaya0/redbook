package com.itcast.service;

import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    Result<List<NoteVo>> search(String key) throws IOException;

    Result<Object> searchAll(String keyword, Integer type, Integer page, Integer size) throws IOException;

    Result<List<String>> suggest(String keyword) throws IOException;
}

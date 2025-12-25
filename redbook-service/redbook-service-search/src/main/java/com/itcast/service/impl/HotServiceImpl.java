package com.itcast.service.impl;

import com.itcast.constant.RedisConstant;
import com.itcast.result.Result;
import com.itcast.service.HotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class HotServiceImpl implements HotService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<List<Map<String, Object>>> getHotList(Integer type) {
        String hotKey = RedisConstant.NOTE_SCORE;
        if (type != null && type == 1) {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            hotKey = RedisConstant.NOTE_SCORE_TODAY_PREFIX + dateStr;
        }
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(hotKey, 0, 9);
        if (CollectionUtils.isEmpty(tuples)) {
            return Result.success(null);
        }

        List<Map<String, Object>> hotList = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) {
                continue;
            }
            Map<String, Object> map = new HashMap<>(4);
            map.put("key", tuple.getValue());
            map.put("score", tuple.getScore());
            hotList.add(map);
        }
        log.info("热搜前十名是:{}", hotList);
        return Result.success(hotList);
    }
}

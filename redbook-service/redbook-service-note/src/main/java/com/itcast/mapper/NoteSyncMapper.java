package com.itcast.mapper;

import com.itcast.model.pojo.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteSyncMapper {

    @Select("SELECT id, title, content, image, time, type, address, " +
            "       longitude, latitude, `like`, collection, user_id AS userId " +
            "FROM rb_note_0 " +
            "UNION ALL " +
            "SELECT id, title, content, image, time, type, address, " +
            "       longitude, latitude, `like`, collection, user_id AS userId " +
            "FROM rb_note_1 " +
            "UNION ALL " +
            "SELECT id, title, content, image, time, type, address, " +
            "       longitude, latitude, `like`, collection, user_id AS userId " +
            "FROM rb_note_2")
    List<Note> selectAllNotesFromAllTables();
}

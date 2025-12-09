package com.itcast.model.dto;

import com.itcast.model.pojo.Note;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoteEsSyncMessage implements Serializable {
    private String type; // INSERT, UPDATE, DELETE
    private Long noteId;
    private Note note;
}

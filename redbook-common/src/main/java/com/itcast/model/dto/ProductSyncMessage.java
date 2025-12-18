package com.itcast.model.dto;

import lombok.Data;

@Data
public class ProductSyncMessage {
    private String type; // "save" or "delete"
    private Long id;
    private ProductEsDTO data;
}

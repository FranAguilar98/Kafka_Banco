package com.duocuc.bankbff.mscuentas.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponseDTO<T>(List<T> content, int pageNumber, int pageSize, long totalElements) {
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
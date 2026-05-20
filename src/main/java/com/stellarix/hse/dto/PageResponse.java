package com.stellarix.hse.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    public PageResponse(Page<T> source) {
        this.content = source.getContent();
        this.page = source.getNumber();
        this.size = source.getSize();
        this.totalElements = source.getTotalElements();
        this.totalPages = source.getTotalPages();
        this.last = source.isLast();
    }
}

package com.plazoleta.plazoleta.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Pagina de resultados del dominio (no depende de Spring Data).
 */
public class PageResult<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PageResult(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }

    public List<T> getContent() { return content; }

    public int getPage() { return page; }

    public int getSize() { return size; }

    public long getTotalElements() { return totalElements; }

    public int getTotalPages() { return totalPages; }
}

package com.prompttraining.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应体（用于游标分页）
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private Long nextCursor;
    private Boolean hasMore;

    public PageResult(List<T> list, Long nextCursor, Boolean hasMore) {
        this.list = list;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }
}

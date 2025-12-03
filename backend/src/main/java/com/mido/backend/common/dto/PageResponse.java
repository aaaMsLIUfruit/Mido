package com.mido.backend.common.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import lombok.Getter;

@Getter
public class PageResponse<T> {

    private final long page;
    private final long size;
    private final long total;
    private final List<T> records;

    public PageResponse(long page, long size, long total, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.records = records;
    }

    public static <T> PageResponse<T> from(IPage<T> page) {
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }
}


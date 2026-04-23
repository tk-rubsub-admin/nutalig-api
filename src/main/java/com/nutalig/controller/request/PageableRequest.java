package com.nutalig.controller.request;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@Accessors(chain = true)
public class PageableRequest {
    private Integer page;
    private Integer size;
    private String sortBy;
    private Sort.Direction sortDirection;

    public Pageable build() {

        if (sortBy != null && sortDirection != null) {
            return PageRequest.of(
                    page - 1,
                    size,
                    Sort.by(sortDirection, sortBy)
            );
        }

        // ไม่มี sort → ส่ง Pageable แบบ unsorted
        return PageRequest.of(
                page - 1,
                size,
                Sort.unsorted()
        );
    }
}

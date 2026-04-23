package com.nutalig.utils;

import com.nutalig.controller.response.Pagination;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationUtils {

    public static Pagination createPagination(Page<?> page) {
        Pagination pagination = new Pagination();
        pagination.setPage(page.getPageable().getPageNumber());
        pagination.setSize(page.getSize());
        pagination.setTotalPage(page.getTotalPages());
        pagination.setTotalRecords(page.getTotalElements());

        return pagination;
    }

}

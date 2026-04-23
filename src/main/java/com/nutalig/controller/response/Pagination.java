package com.nutalig.controller.response;

import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class Pagination {

    private Integer page;
    private Integer size;
    private Integer totalPage;
    private Long totalRecords;

    public static Pagination build(Page<?> pageData) {
        final Pagination pagination = new Pagination();
        pagination.setPage(pageData.getNumber() + 1);
        pagination.setSize(pageData.getSize());
        pagination.setTotalPage(pageData.getTotalPages());
        pagination.setTotalRecords(pageData.getTotalElements());
        return pagination;
    }

}

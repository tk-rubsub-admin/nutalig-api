package com.nutalig.utils;

import com.nutalig.controller.response.Pagination;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationUtilsTest {

    @Test
    void createPagination_shouldReturnCorrectData() {
        // When
        Pageable pageable = PageRequest.of(11, 22);
        final Page<String> page = new PageImpl<>(List.of("mock1", "mock2", "mock3"), pageable, 3);
        final Pagination pagination = PaginationUtils.createPagination(page);

        // Then
        assertEquals(11, pagination.getPage());
        assertEquals(22, pagination.getSize());
        assertEquals(12, pagination.getTotalPage());
        assertEquals((11 * 22) + 3, pagination.getTotalRecords());
    }
}
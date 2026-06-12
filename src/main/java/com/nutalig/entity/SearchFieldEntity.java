package com.nutalig.entity;

import com.nutalig.constant.SearchFieldInputType;
import com.nutalig.entity.id.SearchFieldId;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "search_field")
@Entity(name = "SearchField")
@IdClass(SearchFieldId.class)
public class SearchFieldEntity extends AuditDateEntity {

    @Id
    @Column(name = "screen_code")
    private String screenCode;

    @Id
    @Column(name = "field_code")
    private String fieldCode;

    @Column(name = "label_th")
    private String labelTh;

    @Column(name = "label_en")
    private String labelEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type")
    private SearchFieldInputType inputType;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active")
    private Boolean active;
}

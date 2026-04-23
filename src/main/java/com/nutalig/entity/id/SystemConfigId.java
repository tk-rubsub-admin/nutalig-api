package com.nutalig.entity.id;

import com.nutalig.constant.SystemConstant;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
@Embeddable
public class SystemConfigId {

    @Column(name = "group_code")
    @Enumerated(EnumType.STRING)
    private SystemConstant groupCode;

    @Column(name = "code")
    private String code;

}

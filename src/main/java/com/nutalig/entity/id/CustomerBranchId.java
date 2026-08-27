package com.nutalig.entity.id;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomerBranchId implements Serializable {
    private String customer;
    private String branchCode;
}

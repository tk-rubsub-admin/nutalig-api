package com.nutalig.entity.id;

import lombok.Data;
import java.io.Serializable;

@Data
public class ProductFamilyQcPolicyId implements Serializable {
    private String taxonomyRevision;
    private String productFamilyCode;
}

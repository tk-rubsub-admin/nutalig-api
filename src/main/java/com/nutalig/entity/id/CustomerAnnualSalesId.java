package com.nutalig.entity.id;

import com.nutalig.constant.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class CustomerAnnualSalesId implements Serializable {

    @Column(name = "customer_id", nullable = false, length = 255)
    private String customerId;

    @Column(name = "sales_year", nullable = false)
    private Integer salesYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    public CustomerAnnualSalesId(String customerId, Integer salesYear, Currency currency) {
        this.customerId = customerId;
        this.salesYear = salesYear;
        this.currency = currency;
    }
}

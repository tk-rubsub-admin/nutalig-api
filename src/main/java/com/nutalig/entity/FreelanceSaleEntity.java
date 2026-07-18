package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@Entity(name = "FreelanceSale")
@Table(name = "freelance_sale")
public class FreelanceSaleEntity {

    @Id
    @GeneratedValue(generator = "freelanceSaleIdGenerator")
    @GenericGenerator(name = "freelanceSaleIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "NTL-FS"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%04d")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "sale_coverage", length = 255)
    private String saleCoverage;

    @Column(name = "additional", length = 1000)
    private String additional;
}

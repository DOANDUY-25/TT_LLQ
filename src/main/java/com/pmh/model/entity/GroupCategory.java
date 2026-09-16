package com.pmh.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PMH_GROUP_CATEGORY")
public class GroupCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PARAM_NAME", nullable = false, length = 255)
    private String paramName;

    @Column(name = "PARAM_VALUE", nullable = false, length = 255)
    private String paramValue;

    @Column(name = "PARAM_TYPE", nullable = false, length = 255)
    private String paramType;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "COMPONENT_CODE", nullable = false, length = 255)
    private String componentCode;

    @Temporal(TemporalType.DATE)
    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private Date effectiveDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "END_EFFECTIVE_DATE")
    private Date endEffectiveDate;
}

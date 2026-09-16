package com.pmh.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PMH_ROLES")
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ROLE_CODE", nullable = false, length = 255, unique = true)
    private String roleCode;

    @Column(name = "ROLE_NAME", nullable = false, length = 255)
    private String roleName;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;
}

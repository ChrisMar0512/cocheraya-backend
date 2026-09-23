package com.cocheraya.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "feature")
@Data
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @Column(unique = true, nullable = false)
    private String name;
}

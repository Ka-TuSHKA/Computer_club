package com.computer.lub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "pcs")
@Data
public class Pc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rowNumber;

    @Column(nullable = false)
    private Integer seatNumber;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, unique = true)
    private String pcNumber;

    private String processor;

    private String ram;

    private String gpu;

    @Column(length = 1000)
    private String comment;
}

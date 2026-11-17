package com.computer.lub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Table(name = "pcs")
@Data
public class Pc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Укажите номер ряда")
    @Column(nullable = false)
    private Integer rowNumber;

    @NotNull(message = "Укажите номер места")
    @Column(nullable = false)
    private Integer seatNumber;

    // FREE/BOOKED/IN_USE вычисляются автоматически, админ задает только REPAIR
    @NotNull(message = "Не указан статус")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PcStatus status;

    @NotBlank(message = "Номер ПК не может быть пустым")
    @Column(nullable = false)
    private String pcNumber;

    private String processor;

    private String ram;

    private String gpu;

    @Column(length = 1000)
    private String comment;

    @Column
    private Boolean deleted = false;
}

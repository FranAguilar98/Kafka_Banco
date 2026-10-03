package com.duocuc.bankbff.mstransacciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transacciones", uniqueConstraints = @UniqueConstraint(columnNames = {"transaccion_origen_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaccion_origen_id", nullable = false)
    private Long transaccionOrigenId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Boolean anomalia;

    @Column(length = 200)
    private String motivoAnomalia;
}
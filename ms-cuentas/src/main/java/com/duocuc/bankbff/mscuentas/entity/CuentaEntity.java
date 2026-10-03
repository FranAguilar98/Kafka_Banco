package com.duocuc.bankbff.mscuentas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cuentas_interes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_origen_id", nullable = false, unique = true)
    private Long cuentaOrigenId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoInicial;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal tasaAplicada;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interesCalculado;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoFinal;

    private Integer edad;
}
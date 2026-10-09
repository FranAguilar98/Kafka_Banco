package cl.duocuc.bancoxyz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "cuentas_intereses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaInteres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private Integer edad;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "saldo_inicial", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "tasa_interes_aplicada", nullable = false, precision = 6, scale = 4)
    private BigDecimal tasaInteresAplicada;

    @Column(name = "interes_generado", nullable = false, precision = 15, scale = 2)
    private BigDecimal interesGenerado;

    @Column(name = "saldo_final", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "fecha_calculo", nullable = false)
    private LocalDate fechaCalculo;
}

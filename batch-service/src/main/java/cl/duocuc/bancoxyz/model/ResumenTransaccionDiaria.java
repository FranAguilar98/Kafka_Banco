package cl.duocuc.bancoxyz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "resumen_transacciones_diarias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenTransaccionDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "total_debito", precision = 15, scale = 2)
    private BigDecimal totalDebito;

    @Column(name = "total_credito", precision = 15, scale = 2)
    private BigDecimal totalCredito;

    @Column(name = "cantidad_transacciones")
    private Integer cantidadTransacciones;

    @Column(name = "cantidad_anomalias")
    private Integer cantidadAnomalias;
}

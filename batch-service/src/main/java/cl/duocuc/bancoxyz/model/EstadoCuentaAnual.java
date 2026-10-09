package cl.duocuc.bancoxyz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "estados_cuenta_anuales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoCuentaAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "total_depositos", precision = 15, scale = 2)
    private BigDecimal totalDepositos;

    @Column(name = "total_egresos", precision = 15, scale = 2)
    private BigDecimal totalEgresos;

    @Column(name = "saldo_neto", precision = 15, scale = 2)
    private BigDecimal saldoNeto;

    @Column(name = "cantidad_movimientos")
    private Integer cantidadMovimientos;

    @Column(name = "cantidad_anomalias")
    private Integer cantidadAnomalias;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;
}

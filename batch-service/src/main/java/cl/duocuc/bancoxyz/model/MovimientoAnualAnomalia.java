package cl.duocuc.bancoxyz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_anuales_anomalias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoAnualAnomalia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Column(name = "dato_original", length = 500)
    private String datoOriginal;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "fecha_deteccion", nullable = false)
    private LocalDateTime fechaDeteccion;
}

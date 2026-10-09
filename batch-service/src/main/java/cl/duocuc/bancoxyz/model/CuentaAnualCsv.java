package cl.duocuc.bancoxyz.model;

import lombok.Data;


@Data
public class CuentaAnualCsv {
    private String cuentaId;
    private String fecha;
    private String transaccion;
    private String monto;
    private String descripcion;
}

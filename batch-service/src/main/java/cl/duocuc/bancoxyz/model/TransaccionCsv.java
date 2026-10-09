package cl.duocuc.bancoxyz.model;

import lombok.Data;

@Data
public class TransaccionCsv {
    private String id;
    private String fecha;
    private String monto;
    private String tipo;
}

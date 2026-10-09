package cl.duocuc.bancoxyz.exception;

public class ConexionTransitoriaException extends RuntimeException{

    public ConexionTransitoriaException(String mensaje) {
        super(mensaje);
    }
 
    public ConexionTransitoriaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

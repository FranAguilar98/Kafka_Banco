package com.duocuc.bankbff.core.exception;

import org.springframework.web.client.RestClientException;

public class ServicioNoDisponibleException extends RestClientException {
    public ServicioNoDisponibleException(String servicio) {
        super("El servicio " + servicio + " no esta disponible en este momento. Intenta nuevamente en unos segundos.");
    }
}

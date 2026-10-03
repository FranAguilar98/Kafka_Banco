package com.duocuc.bankbff.mstransacciones.listener;

import com.duocuc.bankbff.mstransacciones.entity.TransaccionEntity;
import com.duocuc.bankbff.mstransacciones.event.MovimientoRegistradoEvent;
import com.duocuc.bankbff.mstransacciones.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
@RequiredArgsConstructor
@Slf4j
public class MovimientoEventListener {

    private static final long OFFSET_ORIGEN_EVENTOS = 900_000_000L;
    private static final BigDecimal UMBRAL_ANOMALIA = new BigDecimal("3000000");

    private final TransaccionRepository transaccionRepository;

    @KafkaListener(topics = "${app.kafka.topics.movimiento-registrado}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMovimientoRegistrado(MovimientoRegistradoEvent evento) {
        long transaccionOrigenId = OFFSET_ORIGEN_EVENTOS + evento.movimientoId();

        if (transaccionRepository.existsByTransaccionOrigenId(transaccionOrigenId)) {
            log.info("Evento movimientoId={} ya procesado anteriormente, se ignora (idempotencia)", evento.movimientoId());
            return;
        }

        boolean anomalia = evento.monto() != null && evento.monto().compareTo(UMBRAL_ANOMALIA) > 0;

        TransaccionEntity transaccion = TransaccionEntity.builder()
                .transaccionOrigenId(transaccionOrigenId)
                .fecha(evento.fecha())
                .monto(evento.monto())
                .tipo(evento.transaccion())
                .anomalia(anomalia)
                .motivoAnomalia(anomalia ? "Monto elevado detectado por el motor de eventos (> " + UMBRAL_ANOMALIA + ")" : null)
                .build();

        transaccionRepository.save(transaccion);
        log.info("Transaccion generada desde evento: movimientoId={}, cuenta={}, monto={}, anomalia={}",
                evento.movimientoId(), evento.cuentaOrigenId(), evento.monto(), anomalia);
    }
}

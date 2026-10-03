package com.duocuc.bankbff.msmovimientos.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class MovimientoEventPublisher {

    private final KafkaTemplate<String, MovimientoRegistradoEvent> kafkaTemplate;
    private final String topic;

    public MovimientoEventPublisher(KafkaTemplate<String, MovimientoRegistradoEvent> kafkaTemplate,
                                     @Value("${app.kafka.topics.movimiento-registrado}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicar(MovimientoRegistradoEvent evento) {
        String key = String.valueOf(evento.cuentaOrigenId());
        kafkaTemplate.send(topic, key, evento).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("No se pudo publicar evento movimiento-registrado (movimientoId={}, cuenta={}): {}",
                        evento.movimientoId(), evento.cuentaOrigenId(), ex.toString());
            } else {
                log.info("Evento movimiento-registrado publicado: movimientoId={}, cuenta={}, topic-partition={}, offset={}",
                        evento.movimientoId(), evento.cuentaOrigenId(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}

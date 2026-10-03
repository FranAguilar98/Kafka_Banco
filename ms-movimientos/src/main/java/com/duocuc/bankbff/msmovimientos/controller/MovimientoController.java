package com.duocuc.bankbff.msmovimientos.controller;

import com.duocuc.bankbff.msmovimientos.dto.MovimientoDTO;
import com.duocuc.bankbff.msmovimientos.dto.NuevoMovimientoRequest;
import com.duocuc.bankbff.msmovimientos.entity.MovimientoEntity;
import com.duocuc.bankbff.msmovimientos.event.MovimientoEventPublisher;
import com.duocuc.bankbff.msmovimientos.event.MovimientoRegistradoEvent;
import com.duocuc.bankbff.msmovimientos.repository.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoRepository movimientoRepository;
    private final MovimientoEventPublisher movimientoEventPublisher;

    @GetMapping("/cuenta/{cuentaOrigenId}")
    public List<MovimientoDTO> obtenerMovimientos(@PathVariable Long cuentaOrigenId) {
        return movimientoRepository.findAllByCuentaOrigenIdOrderByFechaDesc(cuentaOrigenId)
                .stream()
                .map(this::aDTO)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoDTO registrarMovimiento(@RequestBody NuevoMovimientoRequest request) {
        MovimientoEntity guardado = movimientoRepository.save(
                MovimientoEntity.builder()
                        .cuentaOrigenId(request.cuentaOrigenId())
                        .fecha(LocalDate.now())
                        .transaccion(request.transaccion())
                        .monto(request.monto())
                        .descripcion(request.descripcion())
                        .build());

        movimientoEventPublisher.publicar(new MovimientoRegistradoEvent(
                guardado.getId(), guardado.getCuentaOrigenId(), guardado.getFecha(),
                guardado.getTransaccion(), guardado.getMonto(), guardado.getDescripcion()));

        return aDTO(guardado);
    }

    private MovimientoDTO aDTO(MovimientoEntity entity) {
        return new MovimientoDTO(
                entity.getCuentaOrigenId(), entity.getFecha(), entity.getTransaccion(),
                entity.getMonto(), entity.getDescripcion());
    }
}
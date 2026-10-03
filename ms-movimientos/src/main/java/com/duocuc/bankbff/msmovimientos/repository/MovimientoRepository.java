package com.duocuc.bankbff.msmovimientos.repository;

import com.duocuc.bankbff.msmovimientos.entity.MovimientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<MovimientoEntity, Long> {

    List<MovimientoEntity> findAllByCuentaOrigenIdOrderByFechaDesc(Long cuentaOrigenId);
}
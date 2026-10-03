package com.duocuc.bankbff.mstransacciones.repository;

import com.duocuc.bankbff.mstransacciones.entity.TransaccionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransaccionRepository extends JpaRepository<TransaccionEntity, Long> {

    Page<TransaccionEntity> findByFechaBetween(LocalDate desde, LocalDate hasta, Pageable pageable);

    Page<TransaccionEntity> findByTipo(String tipo, Pageable pageable);

    List<TransaccionEntity> findTop10ByOrderByFechaDescIdDesc();

    boolean existsByTransaccionOrigenId(Long transaccionOrigenId);
}
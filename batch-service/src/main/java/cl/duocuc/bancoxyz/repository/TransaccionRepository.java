package cl.duocuc.bancoxyz.repository;

import cl.duocuc.bancoxyz.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    List<Transaccion> findByFecha(LocalDate fecha);

    @org.springframework.data.jpa.repository.Query(
        "select distinct t.fecha from Transaccion t order by t.fecha")
    List<LocalDate> findDistinctFechas();
}

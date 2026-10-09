package cl.duocuc.bancoxyz.repository;

import cl.duocuc.bancoxyz.model.TransaccionAnomalia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransaccionAnomaliaRepository extends JpaRepository<TransaccionAnomalia, Long> {
}

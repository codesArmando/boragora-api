package br.com.boragora.api.grupo;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    List<Grupo> findByHorarioSaidaIda(LocalTime horarioSaidaIda);
}

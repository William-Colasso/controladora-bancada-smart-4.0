package com.tecdes.smart.app_smart_40.repository;

import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByPosicao(Integer posicao);

    List<Estoque> findByCorBloco(CorBloco corBloco);

    List<Estoque> findByCorBlocoAndQuantidadeGreaterThan(CorBloco corBloco, Integer quantidade);

    @Query("SELECT COUNT(e) FROM Estoque e WHERE e.vlCorBloco = :cor")
    Long contarDisponibilidadeCor(@Param("cor") Integer cor);

    @Query("SELECT e FROM Estoque e WHERE e.vlCorBloco = 0 ORDER BY e.nrPosicao ASC")
    List<Estoque> findPosicoesVazias();
}
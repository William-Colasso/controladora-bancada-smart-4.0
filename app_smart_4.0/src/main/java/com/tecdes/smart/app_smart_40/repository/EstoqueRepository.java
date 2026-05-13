package com.tecdes.smart.app_smart_40.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tecdes.smart.app_smart_40.model.Estoque;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByNrPosicao(Byte nrPosicao);

    List<Estoque> findByVlCorBlocoNot(Byte vlCorBloco);
}

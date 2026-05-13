package com.tecdes.smart.app_smart_40.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tecdes.smart.app_smart_40.model.Expedicao;

@Repository
public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    Optional<Expedicao> findByPedidoIdPedido(Long idPedido);

    boolean existsByPedidoIdPedido(Long idPedido);

    List<Expedicao> findAll();
}
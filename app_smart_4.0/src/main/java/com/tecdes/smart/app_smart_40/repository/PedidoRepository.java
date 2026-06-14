package com.tecdes.smart.app_smart_40.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tecdes.smart.app_smart_40.model.Pedido;
import java.util.List;


@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // No PedidoRepository
    @Query("SELECT COALESCE(MAX(p.ordemProducao), 0) + 1 FROM Pedido p")
    Integer proximaOrdemProducao();

     List<Pedido> findByOrdemProducao(Integer ordemProducao);
}

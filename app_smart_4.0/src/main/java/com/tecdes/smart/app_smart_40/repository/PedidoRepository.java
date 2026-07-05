package com.tecdes.smart.app_smart_40.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import java.util.List;


@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // No PedidoRepository
    @Query("SELECT COALESCE(MAX(p.ordemProducao), 0) + 1 FROM Pedido p")
    Integer proximaOrdemProducao();

     List<Pedido> findByOrdemProducao(Integer ordemProducao);

    // Unicidade da OP quando o usuário a escolhe manualmente (criar/atualizar).
    boolean existsByOrdemProducao(Integer ordemProducao);

    // Fonte da verdade para "há algo rodando na bancada" — sobrevive a restart
    // (a fila em memória não). A bancada executa um pedido por vez, então no
    // máximo um pedido fica em PRODUCAO.
    Optional<Pedido> findFirstByStatus(StatusPedido status);

    // Histórico da posição de expedição: Pedido.expedicao (FK) persiste mesmo após a
    // posição ser liberada, então "todos que passaram por ela" é uma consulta direta.
    List<Pedido> findByExpedicaoPosicaoOrderByDataEntradaProducaoDesc(Integer posicao);
}

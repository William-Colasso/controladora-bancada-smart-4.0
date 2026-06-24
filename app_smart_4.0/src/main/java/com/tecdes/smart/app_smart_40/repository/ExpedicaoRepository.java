package com.tecdes.smart.app_smart_40.repository;

import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    // ADICIONADO: verificar por ID evita comparação com entidade sem contexto
    // gerenciado
    boolean existsByPedidoAtualId(Long pedidoId);

    Optional<Expedicao> findFirstByPedidoAtualIsNull();

    long countByPedidoAtualIsNull();

    Optional<Expedicao> findByPosicao(Integer posicao);

    @EntityGraph(attributePaths = {
            "pedidoAtual",
            "pedidoAtual.blocos",
            "pedidoAtual.blocos.laminas"
    })
    @Query("SELECT e FROM Expedicao e")
    List<Expedicao> findAllComPedidoAtualEBlocos();

    @Query("SELECT p FROM Pedido p WHERE p.expedicao.id = :expedicaoId ORDER BY p.dataEntradaExpedicao")
    List<Pedido> findHistoricoPorExpedicao(@Param("expedicaoId") Long expedicaoId);

}
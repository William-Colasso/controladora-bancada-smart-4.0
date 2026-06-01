package com.tecdes.smart.app_smart_40.repository;

import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByPosicao(Integer posicao);

    List<Estoque> findByCorBloco(CorBloco corbloco);
    Estoque findFirstByCorBloco(CorBloco corBloco);

    // CORRIGIDO: removido findByCorBlocoAndQuantidadeGreaterThan (campo
    // 'quantidade' não existe)
    // Substituído por query que conta blocos associados à posição
    List<Estoque> findByCorBlocoNot(CorBloco corBloco);

    // CORRIGIDO: e.vlCorBloco → e.corBloco (nome do atributo Java)
    @Query("SELECT COUNT(e) FROM Estoque e WHERE e.corBloco = :cor")
    Long contarDisponibilidadeCor(@Param("cor") CorBloco cor);

    // CORRIGIDO: e.vlCorBloco → e.corBloco | e.nrPosicao → e.posicao
    @Query("SELECT e FROM Estoque e WHERE e.corBloco = com.tecdes.smart.app_smart_40.model.enums.CorBloco.VAZIO ORDER BY e.posicao ASC")
    List<Estoque> findPosicoesVazias();

    @Modifying
    @Transactional
    @Query("""
            UPDATE  Estoque e
            SET e.corBloco = 0
            WHERE e.id IN :ids
            """)
    int retirarDoEstoque(
            @Param("ids") List<Long> ids);
}
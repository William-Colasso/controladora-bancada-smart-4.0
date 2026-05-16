package com.tecdes.smart.app_smart_40.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tecdes.smart.app_smart_40.model.enums.CorTampa;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.model.enums.TipoPedido;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@Table(
    name = "T_SMT_PEDIDO",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UN_PEDIDO_ORDEM_PRODUCAO",
            columnNames = {"nr_ordem_producao"}
        )
    },
    check = {
        @CheckConstraint(
            name = "CK_PEDIDO_STATUS",
            constraint = "vl_status IN (1, 2, 3)"
        ),
        @CheckConstraint(
            name = "CK_PEDIDO_TIPO",
            constraint = "tp_pedido IN (1, 2, 3)"
        ),
        @CheckConstraint(
            name = "CK_PEDIDO_COR_TAMPA",
            constraint = "vl_cor_tampa IN (1, 2, 3)"
        )
    }
)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @Column(name = "nr_ordem_producao", nullable = false)
    private Integer ordemProducao;

    @Column(name = "vl_status", nullable = false)
    private StatusPedido status;

    @Column(name = "tp_pedido", nullable = false)
    private TipoPedido tipoPedido;

    @Column(name = "vl_cor_tampa", nullable = false)
    private CorTampa corTampa;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Bloco> blocos;

    // CORRIGIDO: nullable = true — pedido começa sem expedição
    @ManyToOne
    @JoinColumn(name = "id_expedicao", nullable = true)
    @JsonIgnore
    private Expedicao expedicao;

    @Column(name = "dt_entrada_expedicao", nullable = true)
    private LocalDateTime dataEntradaExpedicao;

}
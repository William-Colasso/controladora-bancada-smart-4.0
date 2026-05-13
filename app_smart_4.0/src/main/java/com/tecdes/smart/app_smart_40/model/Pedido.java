package com.tecdes.smart.app_smart_40.model;

import java.time.LocalDateTime;

import com.tecdes.smart.app_smart_40.model.enums.CorTampa;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.model.enums.TipoPedido;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

@Setter
@Getter
@Builder
@Table(name = "T_SMT_PEDIDO")
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nr_ordem_producao", nullable = false)
    private Integer ordemProducao;

    @Column(name = "vl_status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private StatusPedido status;

    @Column(name = "tp_pedido", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private TipoPedido tipoPedido;

    @Column(name = "vl_cor_tampa", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private CorTampa corTampa;

    @Column(name = "dt_criacao", nullable = false)
    @Default
    private LocalDateTime dataCriacao = LocalDateTime.now();
    
    
    
    @Column(name = "dt_entrada_expedicao", nullable = true)
    private LocalDateTime dataEntradaExpedicao ;
    
}

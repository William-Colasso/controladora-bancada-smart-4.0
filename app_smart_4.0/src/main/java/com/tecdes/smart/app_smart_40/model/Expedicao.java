package com.tecdes.smart.app_smart_40.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "T_SMT_EXPEDICAO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expedicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expedicao")
    private Long idExpedicao;

    @Column(name = "nr_posicao", nullable = false, unique = true)
    private Long nrPosicao; // CHECK: BETWEEN 1 AND 12

    //@ManyToOne
    //@JoinColumn(name = "id_pedido", unique = true) // UN_EXPEDICAO_PEDIDO
    //private Pedido pedido;
}
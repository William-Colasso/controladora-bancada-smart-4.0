package com.tecdes.smart.app_smart_40.model;

import jakarta.persistence.*;
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
    private Long nrPosicao;

    @ManyToOne
    @JoinColumn(name = "id_pedido", unique = true)
    private Pedido pedido;
}
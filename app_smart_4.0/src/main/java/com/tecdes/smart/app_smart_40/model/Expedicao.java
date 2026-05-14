package com.tecdes.smart.app_smart_40.model;

import java.util.ArrayList;
import java.util.List;

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
    private Long id;

    @Column(name = "nr_posicao", nullable = false, unique = true)
    private Integer posicao;

    @ManyToOne
    @JoinColumn(name = "id_pedido", unique = true)
    private Pedido pedido;

    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY)
    private List<Pedido> pedidos = new ArrayList<>();


    
}
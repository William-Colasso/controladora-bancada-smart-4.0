package com.tecdes.smart.app_smart_40.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "T_SMT_ESTOQUE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estoque")
    private Long idEstoque;

    @Column(name = "nr_posicao", nullable = false, unique = true)
    private Byte nrPosicao;

    @Column(name = "vl_cor_bloco", nullable = false)
    private Byte vlCorBloco;
}
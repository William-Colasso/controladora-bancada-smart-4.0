package com.tecdes.smart.app_smart_40.model;

import java.util.List;

import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

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
    private Long id;

    @Column(name = "nr_posicao", nullable = false, unique = true)
    private Integer posicao;

    @Column(name = "vl_cor_bloco", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private CorBloco corBloco;


    @OneToMany(mappedBy = "estoque")
    private List<Bloco> blocos; 
}
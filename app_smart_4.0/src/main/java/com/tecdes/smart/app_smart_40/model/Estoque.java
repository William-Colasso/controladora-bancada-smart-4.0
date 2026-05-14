package com.tecdes.smart.app_smart_40.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "T_SMT_ESTOQUE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estoque")
    private Long id;

    @Column(name = "nr_posicao", nullable = false, unique = true)
    private Integer posicao;

    @Column(name = "vl_cor_bloco", nullable = false)
    private CorBloco corBloco;


    @OneToMany(mappedBy = "estoque")
    @JsonIgnore
    private List<Bloco> blocos; 
}
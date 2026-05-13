package com.tecdes.smart.app_smart_40.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "T_SMT_LAMINA")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lamina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lamina")
    private Long id;

    @Column(name = "st_cor_lamina", nullable = false)
    private Integer cor;
    @Column(name = "st_padrao", nullable = false)
    private Integer padrao = 0; 

    @Column(name = "st_posicao", nullable = false)
    private Integer posicaoNoBloco; 

    @ManyToOne
    @JoinColumn(name = "id_bloco", nullable = false)
    private Bloco bloco;
 }

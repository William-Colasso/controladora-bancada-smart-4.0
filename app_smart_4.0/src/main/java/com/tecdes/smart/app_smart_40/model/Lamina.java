package com.tecdes.smart.app_smart_40.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tecdes.smart.app_smart_40.model.enums.CorLamina;
import com.tecdes.smart.app_smart_40.model.enums.PadraoLamina;
import com.tecdes.smart.app_smart_40.model.enums.PosicaoLamina;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

@Entity
@Table(
    name = "T_SMT_LAMINA",
    check = {
        @CheckConstraint(
            name = "CK_LAMINA_COR",
            constraint = "st_cor_lamina IN (1, 2, 3, 4, 5, 6)"
        ),
        @CheckConstraint(
            name = "CK_LAMINA_PADRAO",
            constraint = "st_padrao IN (0, 1, 2, 3)"
        ),
        @CheckConstraint(
            name = "CK_LAMINA_POSICAO",
            constraint = "st_posicao IN (1, 2, 3)"
        )
    }
)@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Lamina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lamina")
    private Long id;

    @Column(name = "st_cor_lamina", nullable = false)
    private CorLamina cor;
    @Column(name = "st_padrao", nullable = false)
    @Default
    private PadraoLamina padrao = PadraoLamina.NENHUM; 

    @Column(name = "st_posicao", nullable = false)
    private PosicaoLamina posicaoNoBloco; 

    @ManyToOne
    @JoinColumn(name = "id_bloco", nullable = false)
    @JsonBackReference
    private Bloco bloco;



 }

package com.tecdes.smart.app_smart_40.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "T_SMT_ESTOQUE",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UN_ESTOQUE_POSICAO",
            columnNames = {"nr_posicao"}
        )
    },
    check = {
        @CheckConstraint(
            name = "CK_ESTOQUE_POSICAO",
            constraint = "nr_posicao BETWEEN 1 AND 28"
        ),
        @CheckConstraint(
            name = "CK_ESTOQUE_COR_BLOCO",
            constraint = "vl_cor_bloco IN (0, 1, 2, 3)"
        )
    }
)@Getter
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
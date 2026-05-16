package com.tecdes.smart.app_smart_40.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

@Entity
@Table(
    name = "T_SMT_EXPEDICAO",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UN_EXPEDICAO_POSICAO",
            columnNames = {"nr_posicao"}
        ),
        @UniqueConstraint(
            name = "UN_EXPEDICAO_PEDIDO",
            columnNames = {"id_pedido"}
        )
    },
    check = {
        @CheckConstraint(
            name = "CK_EXPEDICAO_POSICAO",
            constraint = "nr_posicao BETWEEN 1 AND 12"
        )
    }
)@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expedicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expedicao")
    private Long id;

    @Column(name = "nr_posicao", nullable = false, unique = true)
    private Integer posicao;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = true)
    @JsonIgnore
    private Pedido pedido;

    @OneToMany(mappedBy = "expedicao", fetch = FetchType.LAZY)
    @JsonIgnore
    @Default
    private List<Pedido> pedidos = new ArrayList<>();

}
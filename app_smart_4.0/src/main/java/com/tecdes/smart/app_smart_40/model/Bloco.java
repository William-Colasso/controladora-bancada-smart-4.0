package com.tecdes.smart.app_smart_40.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
//import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "t_smt_bloco")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bloco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloco")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_pedido", nullable = false)
    @JsonBackReference
    private Pedido pedido;

    @Column(name = "st_cor_bloco", nullable = false)
    private CorBloco cor;

    @OneToMany(mappedBy = "bloco", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Lamina> laminas;

    @ManyToOne
    @JoinColumn(name = "id_estoque", nullable = false)
    @JsonIgnore
    private Estoque estoque;
}
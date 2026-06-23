package com.tecdes.smart.app_smart_40.model.clp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class EstacaoCLP {
    private boolean recebidoOp;
    private int numeroOP;

    private boolean finishOP;
    private boolean startOP;
    private boolean cancelOP;

    private boolean manual;
    private boolean emergencia;
    private boolean ocupado;
    private boolean aguardando;
}
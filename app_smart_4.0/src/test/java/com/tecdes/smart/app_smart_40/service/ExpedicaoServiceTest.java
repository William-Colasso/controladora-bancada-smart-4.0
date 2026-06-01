package com.tecdes.smart.app_smart_40.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
public class ExpedicaoServiceTest {

    @Mock
    private ExpedicaoRepository expedicaoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private ExpedicaoService expedicaoService;

}

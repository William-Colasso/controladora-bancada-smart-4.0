package com.tecdes.smart.app_smart_40.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tecdes.smart.app_smart_40.repository.BlocoRepository;

@ExtendWith(MockitoExtension.class)
public class BlocoServiceTest {


    @Mock 
    private BlocoRepository blocoRepository;

    @InjectMocks
    private BlocoService blocoService;
    
}

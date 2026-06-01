package com.tecdes.smart.app_smart_40.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tecdes.smart.app_smart_40.repository.LaminaRepository;

@ExtendWith(MockitoExtension.class)
public class LaminaServiceTest {

    @Mock
    private LaminaRepository laminaRepository;

    @InjectMocks
    private LaminaService laminaService;

}

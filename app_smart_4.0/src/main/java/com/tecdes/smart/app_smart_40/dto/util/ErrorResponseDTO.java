package com.tecdes.smart.app_smart_40.dto.util;

import java.time.Instant;

public record ErrorResponseDTO(Integer status, String message, String path, Instant timestamp) {

}

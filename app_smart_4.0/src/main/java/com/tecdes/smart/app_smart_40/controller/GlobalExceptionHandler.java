package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.dto.util.ErrorResponseDTO;
import com.tecdes.smart.app_smart_40.exception.EstoqueInsuficienteException;
import com.tecdes.smart.app_smart_40.exception.PedidoNotFoundException;
import com.tecdes.smart.app_smart_40.exception.PosicaoEstoqueNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.core.annotation.Order;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponseDTO error = new ErrorResponseDTO(
                status.value(),
                "Recurso não encontrado: " + request.getRequestURI(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<ErrorResponseDTO> handleEstoqueInsuficiente(
            EstoqueInsuficienteException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        ErrorResponseDTO error = new ErrorResponseDTO(
                status.value(),
                ex.getMessage() != null ? ex.getMessage() : "Estoque insuficiente para realizar a operação.",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(PedidoNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handlePedidoNotFound(
            PedidoNotFoundException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponseDTO error = new ErrorResponseDTO(
                status.value(),
                ex.getMessage() != null ? ex.getMessage() : "Pedido não encontrado.",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(PosicaoEstoqueNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handlePosicaoEstoqueNotFound(
            PosicaoEstoqueNotFoundException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponseDTO error = new ErrorResponseDTO(
                status.value(),
                ex.getMessage() != null ? ex.getMessage() : "Posição de estoque não encontrada.",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            RuntimeException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponseDTO error = new ErrorResponseDTO(
                status.value(),
                ex.getMessage() != null ? ex.getMessage() : "Requisição inválida.",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponseDTO error = new ErrorResponseDTO(
                status.value(),
                "Ocorreu um erro interno inesperado. Tente novamente mais tarde.",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(error);
    }
}
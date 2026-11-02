package uy.edu.ort.battleshipdemo.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(BatallaNavalException.class)
    public ResponseEntity<String> manejarException(BatallaNavalException ex) {
        return ResponseEntity.status(299).body(ex.getMessage());
    }
}

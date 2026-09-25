package ue.edu.co.splitbill.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Comprobacion de vida del servidor. La usa Render para saber que el despliegue arranco bien, y sirve
 * para "despertar" el servidor gratuito antes de una demostracion. No pide token.
 */
@RestController
@Tag(name = "Estado", description = "Comprobación de que el servidor está arriba.")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "¿Está vivo el servidor?")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

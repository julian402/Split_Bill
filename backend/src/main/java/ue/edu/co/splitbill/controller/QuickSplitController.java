package ue.edu.co.splitbill.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.dto.QuickSplitRequest;
import ue.edu.co.splitbill.dto.QuickSplitResponse;
import ue.edu.co.splitbill.security.CurrentUser;
import ue.edu.co.splitbill.service.QuickSplitService;

@RestController
@RequestMapping("/api/quick-splits")
@Tag(name = "Cuentas rápidas", description = "Cuentas divididas al momento, sin grupo. Cada persona ve solo las suyas.")
public class QuickSplitController {

    private final QuickSplitService quickSplitService;

    public QuickSplitController(QuickSplitService quickSplitService) {
        this.quickSplitService = quickSplitService;
    }

    @GetMapping
    @Operation(summary = "Listar mis cuentas rápidas", description = "De la más reciente a la más antigua.")
    public List<QuickSplitResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return this.quickSplitService.list(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Guardar una cuenta rápida",
            description = "Las partes deben sumar exactamente el total. Si el id ya existe, devuelve la cuenta guardada.")
    public QuickSplitResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuickSplitRequest request) {
        return this.quickSplitService.create(CurrentUser.id(jwt), request);
    }

    @DeleteMapping("/{quickSplitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar una cuenta rápida", description = "Borrado lógico.")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID quickSplitId) {
        this.quickSplitService.delete(CurrentUser.id(jwt), quickSplitId);
    }
}

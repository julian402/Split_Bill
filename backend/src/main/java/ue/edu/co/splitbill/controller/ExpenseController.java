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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.dto.ExpenseRequest;
import ue.edu.co.splitbill.dto.ExpenseResponse;
import ue.edu.co.splitbill.security.CurrentUser;
import ue.edu.co.splitbill.service.ExpenseService;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@Tag(name = "Gastos", description = "Gastos de un grupo con sus partes. Montos en centavos.")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    @Operation(summary = "Listar gastos del grupo", description = "Del más reciente al más antiguo.")
    public List<ExpenseResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
        return this.expenseService.list(CurrentUser.id(jwt), groupId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un gasto",
            description = "Las partes deben sumar exactamente el monto. Si el id ya existe, devuelve el gasto guardado.")
    public ExpenseResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                                  @Valid @RequestBody ExpenseRequest request) {
        return this.expenseService.create(CurrentUser.id(jwt), groupId, request);
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Ver un gasto")
    public ExpenseResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                               @PathVariable UUID expenseId) {
        return this.expenseService.get(CurrentUser.id(jwt), groupId, expenseId);
    }

    @PutMapping("/{expenseId}")
    @Operation(summary = "Editar un gasto", description = "Reemplaza los datos y las partes.")
    public ExpenseResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                                  @PathVariable UUID expenseId, @Valid @RequestBody ExpenseRequest request) {
        return this.expenseService.update(CurrentUser.id(jwt), groupId, expenseId, request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar un gasto", description = "Borrado lógico.")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId, @PathVariable UUID expenseId) {
        this.expenseService.delete(CurrentUser.id(jwt), groupId, expenseId);
    }
}

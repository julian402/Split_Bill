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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.dto.GroupRequest;
import ue.edu.co.splitbill.dto.GroupResponse;
import ue.edu.co.splitbill.dto.MemberRequest;
import ue.edu.co.splitbill.dto.UserResponse;
import ue.edu.co.splitbill.security.CurrentUser;
import ue.edu.co.splitbill.service.GroupService;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Grupos", description = "Grupos de gastos y sus integrantes.")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    @Operation(summary = "Listar mis grupos")
    public List<GroupResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return this.groupService.listMine(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un grupo", description = "Quien lo crea queda como dueño e integrante.")
    public GroupResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GroupRequest request) {
        return this.groupService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Ver un grupo")
    public GroupResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
        return this.groupService.get(CurrentUser.id(jwt), groupId);
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Editar un grupo", description = "Solo el dueño.")
    public GroupResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                                @Valid @RequestBody GroupRequest request) {
        return this.groupService.update(CurrentUser.id(jwt), groupId, request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar un grupo", description = "Solo el dueño. Borrado lógico.")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
        this.groupService.delete(CurrentUser.id(jwt), groupId);
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "Listar integrantes",
            description = "Con includeRemoved=true también trae a los retirados (active=false).")
    public List<UserResponse> listMembers(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                                          @RequestParam(defaultValue = "false") boolean includeRemoved) {
        return this.groupService.listMembers(CurrentUser.id(jwt), groupId, includeRemoved);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar integrante",
            description = "Con email agrega a alguien con cuenta; con nombre crea un integrante sin cuenta.")
    public UserResponse addMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                                  @Valid @RequestBody MemberRequest request) {
        return this.groupService.addMember(CurrentUser.id(jwt), groupId, request);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirar integrante", description = "Solo el dueño. Sus gastos anteriores se conservan.")
    public void removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                             @PathVariable UUID userId) {
        this.groupService.removeMember(CurrentUser.id(jwt), groupId, userId);
    }
}

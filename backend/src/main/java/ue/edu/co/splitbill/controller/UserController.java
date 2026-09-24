package ue.edu.co.splitbill.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ue.edu.co.splitbill.dto.UpdateUserRequest;
import ue.edu.co.splitbill.dto.UserResponse;
import ue.edu.co.splitbill.security.CurrentUser;
import ue.edu.co.splitbill.service.UserService;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Perfil del usuario que inició sesión.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Ver mi perfil")
    public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return this.userService.getMe(CurrentUser.id(jwt));
    }

    @PutMapping("/me")
    @Operation(summary = "Editar mi perfil")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserRequest request) {
        return this.userService.updateMe(CurrentUser.id(jwt), request);
    }
}

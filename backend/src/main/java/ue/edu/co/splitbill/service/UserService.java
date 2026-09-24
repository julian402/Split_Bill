package ue.edu.co.splitbill.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import ue.edu.co.splitbill.dto.UpdateUserRequest;
import ue.edu.co.splitbill.dto.UserResponse;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.exception.NotFoundException;
import ue.edu.co.splitbill.repository.UserRepository;

/** Perfil del usuario que inicio sesion. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        return UserResponse.from(requireActiveUser(userId));
    }

    @Transactional
    public UserResponse updateMe(UUID userId, UpdateUserRequest request) {
        User user = requireActiveUser(userId);
        user.setNames(request.names().trim());
        user.setPhone(AuthService.blankToNull(request.phone()));
        user.validar();
        //saveAndFlush para que @PreUpdate actualice la fecha antes de armar la respuesta
        return UserResponse.from(this.userRepository.saveAndFlush(user));
    }

    private User requireActiveUser(UUID userId) {
        return this.userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}

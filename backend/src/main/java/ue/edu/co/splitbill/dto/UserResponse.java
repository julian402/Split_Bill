package ue.edu.co.splitbill.dto;

import java.util.UUID;

import ue.edu.co.splitbill.entity.User;

/**
 * Lo que el servidor muestra de una persona. No tiene el hash de la contrasena: por eso los
 * controladores nunca devuelven la entidad User directamente.
 *
 * @param registered true si la persona tiene cuenta; false si es un integrante agregado por nombre
 */
public record UserResponse(UUID id, String names, String email, String phone, boolean registered) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNames(), user.getEmail(), user.getPhone(),
                user.hasAccount());
    }
}

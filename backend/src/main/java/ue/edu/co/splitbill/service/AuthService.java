package ue.edu.co.splitbill.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import ue.edu.co.splitbill.dto.LoginRequest;
import ue.edu.co.splitbill.dto.RegisterRequest;
import ue.edu.co.splitbill.dto.TokenResponse;
import ue.edu.co.splitbill.dto.UserResponse;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.exception.ConflictException;
import ue.edu.co.splitbill.exception.InvalidCredentialsException;
import ue.edu.co.splitbill.repository.UserRepository;
import ue.edu.co.splitbill.security.JwtService;

/** Registro e inicio de sesion. */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Email o contrasena incorrectos";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (this.userRepository.existsByEmail(email)) {
            throw new ConflictException("Ya existe una cuenta con ese email");
        }
        User user = new User(request.names().trim(), email, blankToNull(request.phone()),
                this.passwordEncoder.encode(request.password()));
        user.validar();
        user = this.userRepository.save(user);
        return buildToken(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        //el mismo error para "no existe" y "contrasena incorrecta": no se revela que emails estan registrados
        User user = this.userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(User::isActive)
                .filter(User::hasAccount)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        if (!this.passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }
        return buildToken(user);
    }

    private TokenResponse buildToken(User user) {
        return new TokenResponse(this.jwtService.generateToken(user), this.jwtService.getExpirationSeconds(),
                UserResponse.from(user));
    }

    /** Los emails se guardan en minusculas para que Julian@x.com y julian@x.com sean la misma cuenta. */
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}

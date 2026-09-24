package ue.edu.co.splitbill.ui;

import androidx.annotation.NonNull;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Servidor falso para las pruebas de interfaz. Responde lo minimo para que la app funcione:
 *
 * - Login: acepta cualquier email con la clave PASSWORD; con otra, 401 como el backend real.
 * - Lista de grupos: vacia, asi la app crea "Mi grupo" al iniciar sesion.
 * - Todo lo demas: 503. La sincronizacion falla y los cambios quedan pendientes en el celular, que
 *   es justo el trabajo sin conexion: las pantallas funcionan igual porque leen de Room.
 */
public class FakeBackend extends Dispatcher {

    public static final String PASSWORD = "clave-segura-123";
    public static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    public static final String USER_NAMES = "Julian";

    @NonNull
    @Override
    public MockResponse dispatch(@NonNull RecordedRequest request) {
        String path = request.getPath() == null ? "" : request.getPath();
        if ("POST".equals(request.getMethod()) && path.equals("/api/auth/login")) {
            String body = request.getBody().readUtf8();
            if (!body.contains("\"" + PASSWORD + "\"")) {
                return json(401, "{\"status\":401,\"detail\":\"Email o contraseña incorrectos\"}");
            }
            return json(200, "{\"token\":\"token-de-prueba\",\"tokenType\":\"Bearer\",\"expiresIn\":86400,"
                    + "\"user\":{\"id\":\"" + USER_ID + "\",\"names\":\"" + USER_NAMES + "\","
                    + "\"email\":\"julian@test.co\",\"registered\":true,\"active\":true}}");
        }
        if ("GET".equals(request.getMethod()) && path.equals("/api/groups")) {
            return json(200, "[]");
        }
        return json(503, "{\"status\":503,\"detail\":\"Servidor en mantenimiento\"}");
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}

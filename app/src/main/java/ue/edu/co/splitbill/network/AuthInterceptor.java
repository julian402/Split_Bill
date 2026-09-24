package ue.edu.co.splitbill.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import ue.edu.co.splitbill.session.SessionManager;

/**
 * Se mete en cada peticion HTTP antes de que salga.
 *
 * 1. Le agrega el encabezado "Authorization: Bearer (token)", asi ningun metodo de ApiService tiene
 *    que acordarse del token.
 * 2. Si el servidor responde 401 a una peticion que llevaba token, el token vencio o es invalido:
 *    se marca la sesion como vencida y la proxima pantalla que se abra manda al login.
 *
 * Las rutas de /api/auth/ no llevan token: un 401 ahi es "contrasena incorrecta", no sesion vencida.
 */
public class AuthInterceptor implements Interceptor {

    private static final String AUTH_PATH = "/api/auth/";

    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String token = this.sessionManager.getToken();
        boolean isAuthRequest = request.url().encodedPath().startsWith(AUTH_PATH);
        if (token == null || isAuthRequest) {
            return chain.proceed(request);
        }

        Response response = chain.proceed(request.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build());
        if (response.code() == 401) {
            this.sessionManager.expireSession();
        }
        return response;
    }
}

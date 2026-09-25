package ue.edu.co.splitbill.network;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ue.edu.co.splitbill.network.dto.ErrorResponse;
import ue.edu.co.splitbill.session.SessionManager;

/**
 * Arma el cliente HTTP y ejecuta las peticiones.
 *
 * Las peticiones se ejecutan de forma sincrona con execute(): quien llama ya esta en el hilo de red
 * (AppExecutors.network()), asi que no hace falta otro mecanismo de callbacks. El resultado vuelve a
 * la pantalla por el DataCallback de siempre.
 */
public final class ApiClient {

    private static final String TAG = "ApiClient";
    /**
     * Un servidor gratuito (Render) se duerme tras un rato sin uso y tarda hasta un minuto en
     * despertar: se le da ese tiempo antes de dar la peticion por perdida.
     */
    private static final int TIMEOUT_SECONDS = 60;

    private ApiClient() {
        //impide crear objetos de esta clase
    }

    public static ApiService create(String baseUrl, SessionManager sessionManager) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(sessionManager))
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    /**
     * Ejecuta la peticion y devuelve el cuerpo de la respuesta.
     *
     * @throws ApiException si el servidor respondio con un error; trae el mensaje del ProblemDetail
     * @throws IOException  si no hubo respuesta (sin red, servidor apagado, tiempo agotado)
     */
    public static <T> T execute(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        if (response.isSuccessful()) {
            return response.body();
        }
        throw new ApiException(response.code(), readErrorMessage(response));
    }

    /**
     * Igual que execute, pero devuelve la respuesta completa: sirve cuando ademas del cuerpo hace falta
     * un encabezado, como la hora del servidor (Date).
     */
    public static <T> Response<T> executeForResponse(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        if (response.isSuccessful()) {
            return response;
        }
        throw new ApiException(response.code(), readErrorMessage(response));
    }

    /** Saca el campo detail del ProblemDetail; si no se puede leer, usa el codigo HTTP. */
    private static String readErrorMessage(Response<?> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                ErrorResponse error = new Gson().fromJson(errorBody.charStream(), ErrorResponse.class);
                if (error != null && error.getDetail() != null) {
                    return error.getDetail();
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "ERROR AL LEER EL MENSAJE DE ERROR DEL SERVIDOR", e);
            }
        }
        return "El servidor respondió con el código " + response.code();
    }
}

package ue.edu.co.splitbill.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ue.edu.co.splitbill.network.dto.ExpenseDto;
import ue.edu.co.splitbill.network.dto.GroupDto;
import ue.edu.co.splitbill.network.dto.LinkMemberRequest;
import ue.edu.co.splitbill.network.dto.LoginRequest;
import ue.edu.co.splitbill.network.dto.MemberRequest;
import ue.edu.co.splitbill.network.dto.MessageDto;
import ue.edu.co.splitbill.network.dto.QuickSplitDto;
import ue.edu.co.splitbill.network.dto.RegisterRequest;
import ue.edu.co.splitbill.network.dto.TokenResponse;
import ue.edu.co.splitbill.network.dto.UpdateProfileRequest;
import ue.edu.co.splitbill.network.dto.UserDto;

/**
 * Los endpoints del backend, declarados como metodos de Java.
 *
 * Retrofit genera la implementacion: cada anotacion dice el metodo HTTP y la ruta, y Gson convierte
 * los objetos a JSON y de vuelta. El token no aparece aqui porque lo agrega AuthInterceptor a todas
 * las peticiones.
 */
public interface ApiService {

    @POST("api/auth/login")
    Call<TokenResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<TokenResponse> register(@Body RegisterRequest request);

    /** Nombre y telefono de quien inicio sesion. */
    @PUT("api/users/me")
    Call<UserDto> updateMe(@Body UpdateProfileRequest request);

    @GET("api/groups")
    Call<List<GroupDto>> getGroups();

    @POST("api/groups")
    Call<GroupDto> createGroup(@Body GroupDto group);

    /** Solo el dueno del grupo puede cambiarle el nombre. */
    @PUT("api/groups/{groupId}")
    Call<GroupDto> updateGroup(@Path("groupId") String groupId, @Body GroupDto group);

    @GET("api/groups/{groupId}/members")
    Call<List<UserDto>> getMembers(@Path("groupId") String groupId, @Query("includeRemoved") boolean includeRemoved);

    @POST("api/groups/{groupId}/members")
    Call<UserDto> addMember(@Path("groupId") String groupId, @Body MemberRequest member);

    @POST("api/groups/{groupId}/members/{memberId}/claim")
    Call<UserDto> claimMember(@Path("groupId") String groupId, @Path("memberId") String memberId);

    /** Une a un integrante agregado por nombre con la cuenta del email: sus gastos pasan a esa cuenta. */
    @POST("api/groups/{groupId}/members/{memberId}/link")
    Call<UserDto> linkMember(@Path("groupId") String groupId, @Path("memberId") String memberId,
                             @Body LinkMemberRequest request);

    @DELETE("api/groups/{groupId}/members/{userId}")
    Call<Void> removeMember(@Path("groupId") String groupId, @Path("userId") String userId);

    /**
     * @param updatedSince si llega (ISO-8601), el servidor devuelve solo lo que cambio desde esa fecha,
     *                     incluidos los borrados; si es null, todos los gastos activos
     */
    @GET("api/groups/{groupId}/expenses")
    Call<List<ExpenseDto>> getExpenses(@Path("groupId") String groupId, @Query("updatedSince") String updatedSince);

    @POST("api/groups/{groupId}/expenses")
    Call<ExpenseDto> createExpense(@Path("groupId") String groupId, @Body ExpenseDto expense);

    @PUT("api/groups/{groupId}/expenses/{expenseId}")
    Call<ExpenseDto> updateExpense(@Path("groupId") String groupId, @Path("expenseId") String expenseId,
                                   @Body ExpenseDto expense);

    @DELETE("api/groups/{groupId}/expenses/{expenseId}")
    Call<Void> deleteExpense(@Path("groupId") String groupId, @Path("expenseId") String expenseId);

    /**
     * @param since si llega (ISO-8601), solo los mensajes que llegaron al servidor despues; si es null,
     *              todo el chat
     */
    @GET("api/groups/{groupId}/messages")
    Call<List<MessageDto>> getMessages(@Path("groupId") String groupId, @Query("since") String since);

    @POST("api/groups/{groupId}/messages")
    Call<MessageDto> sendMessage(@Path("groupId") String groupId, @Body MessageDto message);

    /** Las cuentas rapidas de quien inicio sesion. Son pocas: siempre llega la lista completa. */
    @GET("api/quick-splits")
    Call<List<QuickSplitDto>> getQuickSplits();

    @POST("api/quick-splits")
    Call<QuickSplitDto> createQuickSplit(@Body QuickSplitDto quickSplit);

    @DELETE("api/quick-splits/{quickSplitId}")
    Call<Void> deleteQuickSplit(@Path("quickSplitId") String quickSplitId);
}

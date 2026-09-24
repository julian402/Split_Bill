package ue.edu.co.splitbill.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ue.edu.co.splitbill.network.dto.ExpenseDto;
import ue.edu.co.splitbill.network.dto.GroupDto;
import ue.edu.co.splitbill.network.dto.LoginRequest;
import ue.edu.co.splitbill.network.dto.MemberRequest;
import ue.edu.co.splitbill.network.dto.RegisterRequest;
import ue.edu.co.splitbill.network.dto.TokenResponse;
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

    @GET("api/groups")
    Call<List<GroupDto>> getGroups();

    @POST("api/groups")
    Call<GroupDto> createGroup(@Body GroupDto group);

    @GET("api/groups/{groupId}/members")
    Call<List<UserDto>> getMembers(@Path("groupId") String groupId, @Query("includeRemoved") boolean includeRemoved);

    @POST("api/groups/{groupId}/members")
    Call<UserDto> addMember(@Path("groupId") String groupId, @Body MemberRequest member);

    @DELETE("api/groups/{groupId}/members/{userId}")
    Call<Void> removeMember(@Path("groupId") String groupId, @Path("userId") String userId);

    @GET("api/groups/{groupId}/expenses")
    Call<List<ExpenseDto>> getExpenses(@Path("groupId") String groupId);

    @POST("api/groups/{groupId}/expenses")
    Call<ExpenseDto> createExpense(@Path("groupId") String groupId, @Body ExpenseDto expense);

    @DELETE("api/groups/{groupId}/expenses/{expenseId}")
    Call<Void> deleteExpense(@Path("groupId") String groupId, @Path("expenseId") String expenseId);
}

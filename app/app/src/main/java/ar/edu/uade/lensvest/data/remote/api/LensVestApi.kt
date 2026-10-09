package ar.edu.uade.lensvest.data.remote.api

import ar.edu.uade.lensvest.data.remote.dto.AssistantConfirmRequest
import ar.edu.uade.lensvest.data.remote.dto.AssistantMessageRequest
import ar.edu.uade.lensvest.data.remote.dto.AssistantTurnResponse
import ar.edu.uade.lensvest.data.remote.dto.BerryCallbackRequest
import ar.edu.uade.lensvest.data.remote.dto.BerryConnectResponse
import ar.edu.uade.lensvest.data.remote.dto.BerryStatusResponse
import ar.edu.uade.lensvest.data.remote.dto.LoginRequest
import ar.edu.uade.lensvest.data.remote.dto.LoginResponse
import ar.edu.uade.lensvest.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** LensVest backend (Loco). The Authorization header is added by AppContainer's interceptor. */
interface LensVestApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest)

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/berry/connect")
    suspend fun berryConnect(): BerryConnectResponse

    @POST("api/berry/callback")
    suspend fun berryCallback(@Body body: BerryCallbackRequest): BerryStatusResponse

    @GET("api/berry/status")
    suspend fun berryStatus(): BerryStatusResponse

    @POST("api/assistant/messages")
    suspend fun assistantMessage(@Body body: AssistantMessageRequest): AssistantTurnResponse

    @POST("api/assistant/confirm")
    suspend fun assistantConfirm(@Body body: AssistantConfirmRequest): AssistantTurnResponse
}

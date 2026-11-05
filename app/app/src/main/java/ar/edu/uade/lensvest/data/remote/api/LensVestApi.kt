package ar.edu.uade.lensvest.data.remote.api

import ar.edu.uade.lensvest.data.remote.dto.AssistantConfirmRequest
import ar.edu.uade.lensvest.data.remote.dto.AssistantMessageRequest
import ar.edu.uade.lensvest.data.remote.dto.AssistantTurnResponse
import ar.edu.uade.lensvest.data.remote.dto.BerryCallbackRequest
import ar.edu.uade.lensvest.data.remote.dto.BerryConnectResponse
import ar.edu.uade.lensvest.data.remote.dto.BerryStatusResponse
import ar.edu.uade.lensvest.data.remote.dto.LoginRequest
import ar.edu.uade.lensvest.data.remote.dto.LoginResponse
import ar.edu.uade.lensvest.data.remote.dto.RecognizeBrandRequest
import ar.edu.uade.lensvest.data.remote.dto.RecognizedBrandResponse
import ar.edu.uade.lensvest.data.remote.dto.RegisterRequest
import ar.edu.uade.lensvest.data.remote.dto.AssetResponse
import ar.edu.uade.lensvest.data.remote.dto.CurrentUserResponse
import ar.edu.uade.lensvest.data.remote.dto.FavoriteRequest
import ar.edu.uade.lensvest.data.remote.dto.NewScanRequest
import ar.edu.uade.lensvest.data.remote.dto.PortfolioResponse
import ar.edu.uade.lensvest.data.remote.dto.QuotesResponse
import ar.edu.uade.lensvest.data.remote.dto.ScanDto
import ar.edu.uade.lensvest.data.remote.dto.SearchResponse
import ar.edu.uade.lensvest.data.remote.dto.StrategyDto
import ar.edu.uade.lensvest.data.remote.dto.StrategyStatusRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("api/lens/recognize")
    suspend fun recognizeBrand(@Body body: RecognizeBrandRequest): RecognizedBrandResponse

    @GET("api/auth/current")
    suspend fun currentUser(): CurrentUserResponse

    // Market data comes from the user's Berry account through the data agent: each call can take ~10 s.
    @GET("api/portfolio")
    suspend fun portfolio(): PortfolioResponse

    @GET("api/assets/{ticker}")
    suspend fun asset(@Path("ticker") ticker: String): AssetResponse

    @GET("api/quotes")
    suspend fun quotes(@Query("tickers") tickers: String): QuotesResponse

    @GET("api/search")
    suspend fun search(@Query("q") query: String): SearchResponse

    @GET("api/strategies")
    suspend fun strategies(): List<StrategyDto>

    @PATCH("api/strategies/{id}")
    suspend fun updateStrategy(@Path("id") id: Long, @Body body: StrategyStatusRequest): StrategyDto

    @DELETE("api/strategies/{id}")
    suspend fun deleteStrategy(@Path("id") id: Long)

    @GET("api/scans")
    suspend fun scans(): List<ScanDto>

    @POST("api/scans")
    suspend fun recordScan(@Body body: NewScanRequest): ScanDto

    @POST("api/scans/{id}/favorite")
    suspend fun setScanFavorite(@Path("id") id: Long, @Body body: FavoriteRequest): ScanDto
}

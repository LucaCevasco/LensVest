package ar.edu.uade.lensvest.presentation.di

import ar.edu.uade.lensvest.BuildConfig
import ar.edu.uade.lensvest.data.local.SessionStore
import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.repository.AssistantRepositoryImpl
import ar.edu.uade.lensvest.data.repository.AuthRepositoryImpl
import ar.edu.uade.lensvest.data.repository.BerryRepositoryImpl
import ar.edu.uade.lensvest.data.repository.ProfileRepositoryImpl
import ar.edu.uade.lensvest.domain.repository.AssistantRepository
import ar.edu.uade.lensvest.domain.repository.AuthRepository
import ar.edu.uade.lensvest.domain.repository.BerryRepository
import ar.edu.uade.lensvest.domain.repository.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Adds the session JWT to every backend request once the user is signed in. */
    @Provides
    @Singleton
    fun provideOkHttpClient(session: SessionStore): OkHttpClient =
        OkHttpClient.Builder()
            // The assistant can take a while: it may call several Berry tools in one turn.
            .readTimeout(150, TimeUnit.SECONDS)
            .callTimeout(160, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = session.token.value
                val request = if (token == null) {
                    chain.request()
                } else {
                    chain.request().newBuilder().header("Authorization", "Bearer $token").build()
                }
                chain.proceed(request)
            }
            .build()

    @Provides
    @Singleton
    fun provideLensVestApi(client: OkHttpClient): LensVestApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(LensVestApi::class.java)

    @Provides
    @Singleton
    fun provideAuthRepository(impl: AuthRepositoryImpl): AuthRepository = impl

    @Provides
    @Singleton
    fun provideBerryRepository(impl: BerryRepositoryImpl): BerryRepository = impl

    @Provides
    @Singleton
    fun provideAssistantRepository(impl: AssistantRepositoryImpl): AssistantRepository = impl

    @Provides
    @Singleton
    fun provideProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository = impl
}

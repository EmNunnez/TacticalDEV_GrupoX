package cl.duoc.tacticaldev.data.remote

import cl.duoc.tacticaldev.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Un Retrofit por servicio: el backend propio (en AWS) y la API externa no se mezclan.
 * Las URLs vienen de gradle.properties a través de BuildConfig.
 */
object RetrofitInstance {

    /** En debug se registran las peticiones en Logcat; en release no. */
    internal fun nivelLog(debug: Boolean): HttpLoggingInterceptor.Level =
        if (debug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE

    internal val clienteHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = nivelLog(BuildConfig.DEBUG) })
            .build()
    }

    /** Público para poder probar las interfaces contra un servidor falso (MockWebServer). */
    fun crearRetrofit(baseUrl: String, cliente: OkHttpClient = clienteHttp): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiUsuarios: ApiUsuarios by lazy { crearRetrofit(BuildConfig.API_USUARIOS_URL).create(ApiUsuarios::class.java) }
    val apiOperaciones: ApiOperaciones by lazy { crearRetrofit(BuildConfig.API_OPERACIONES_URL).create(ApiOperaciones::class.java) }
    val apiClima: ApiClima by lazy { crearRetrofit(BuildConfig.API_CLIMA_URL).create(ApiClima::class.java) }
}

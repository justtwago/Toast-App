package pl.droidsonroids.toast.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pl.droidsonroids.toast.BuildConfig
import pl.droidsonroids.toast.repositories.contact.ContactRepository
import pl.droidsonroids.toast.repositories.contact.ContactRepositoryImpl
import pl.droidsonroids.toast.repositories.event.EventsRepository
import pl.droidsonroids.toast.repositories.event.EventsRepositoryImpl
import pl.droidsonroids.toast.repositories.speaker.SpeakersRepository
import pl.droidsonroids.toast.repositories.speaker.SpeakersRepositoryImpl
import pl.droidsonroids.toast.services.ContactService
import pl.droidsonroids.toast.services.EventService
import pl.droidsonroids.toast.services.LocalContactService
import pl.droidsonroids.toast.services.SpeakerService
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


private const val ACCEPT = "Accept"
private const val APPLICATION_JSON = "application/json"
private const val SHARED_PREF_NAME = "pl.droidsonroids.toast.prefs"

@Module(includes = [ViewModelModule::class])
class AppModule {
    @Singleton
    @Provides
    fun provideContext(application: Application): Context = application

    @Singleton
    @Provides
    fun provideSharedPreference(context: Context): SharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideEventsRepository(eventService: EventService): EventsRepository = EventsRepositoryImpl(eventService)

    @Singleton
    @Provides
    fun provideSpeakersRepository(speakerService: SpeakerService): SpeakersRepository = SpeakersRepositoryImpl(speakerService)

    @Singleton
    @Provides
    fun provideContactRepository(contactService: ContactService, localContactService: LocalContactService): ContactRepository = ContactRepositoryImpl(contactService, localContactService)

    @Singleton
    @Provides
    fun provideEventService(httpClient: OkHttpClient): EventService =
            getRetrofitBuilder(httpClient)
                    .create(EventService::class.java)

    @Singleton
    @Provides
    fun provideSpeakersService(httpClient: OkHttpClient): SpeakerService =
            getRetrofitBuilder(httpClient)
                    .create(SpeakerService::class.java)

    @Singleton
    @Provides
    fun provideContactService(httpClient: OkHttpClient): ContactService =
            getRetrofitBuilder(httpClient)
                    .create(ContactService::class.java)

    private fun getRetrofitBuilder(httpClient: OkHttpClient) =
            Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                    .client(httpClient)
                    .build()

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                    .addHttpHeaders()
                    .addHttpLoggingInterceptorIfDebugBuildConfig()
                    .build()

    private fun OkHttpClient.Builder.addHttpHeaders() =
            addInterceptor {
                it.proceed(it.request().newBuilder().header(ACCEPT, APPLICATION_JSON).build())
            }

    private fun OkHttpClient.Builder.addHttpLoggingInterceptorIfDebugBuildConfig(): OkHttpClient.Builder {
        if (BuildConfig.DEBUG) {
            addNetworkInterceptor(getHttpLoggingInterceptor())
        }
        return this
    }

    private fun getHttpLoggingInterceptor() =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
}
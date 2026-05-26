package com.tvanime.app.di

import android.content.Context
import com.tvanime.app.data.local.dao.ContentDao
import com.tvanime.app.data.local.dao.FavoriteDao
import com.tvanime.app.data.local.dao.HistoryDao
import com.tvanime.app.data.local.database.TVAnimeDatabase
import com.tvanime.app.data.remote.api.SourceApi
import com.tvanime.app.data.remote.interceptor.UserAgentInterceptor
import com.tvanime.app.data.repository.ContentsRepositoryImpl
import com.tvanime.app.data.repository.FavoritesRepositoryImpl
import com.tvanime.app.data.repository.HistoryRepositoryImpl
import com.tvanime.app.data.repository.ContentsRepository
import com.tvanime.app.domain.usecase.GetCatalogUseCase
import com.tvanime.app.domain.usecase.GetDetailUseCase
import com.tvanime.app.domain.usecase.GetHistoryUseCase
import com.tvanime.app.domain.usecase.SaveProgressUseCase
import com.tvanime.app.domain.usecase.ToggleFavoriteUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor(UserAgentInterceptor())
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://placeholder.api/")
        .client(client)
        .build()

    @Provides
    @Singleton
    fun provideSourceApi(retrofit: Retrofit): SourceApi =
        retrofit.create(SourceApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TVAnimeDatabase =
        TVAnimeDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideContentDao(db: TVAnimeDatabase) = db.contentDao()

    @Provides
    @Singleton
    fun provideHistoryDao(db: TVAnimeDatabase) = db.historyDao()

    @Provides
    @Singleton
    fun provideFavoritesDao(db: TVAnimeDatabase) = db.favoriteDao()

    @Provides
    @Singleton
    fun provideContentRepository(
        api: SourceApi,
        contentDao: ContentDao,
        historyDao: HistoryDao,
        favoriteDao: FavoriteDao
    ): ContentsRepository =
        ContentsRepositoryImpl(api = api, contentDao = contentDao, historyDao = historyDao, favoriteDao = favoriteDao)

    @Provides
    @Singleton
    fun provideHistoryRepository(
        historyDao: HistoryDao
    ): com.tvanime.app.data.repository.HistoryRepository =
        com.tvanime.app.data.repository.HistoryRepositoryImpl(historyDao)

    @Provides
    @Singleton
    fun provideFavoritesRepository(
        favoritesDao: FavoriteDao
    ): com.tvanime.app.data.repository.FavoritesRepository =
        com.tvanime.app.data.repository.FavoritesRepositoryImpl(favoritesDao)

    @Provides
    fun provideGetCatalogUseCase(repo: ContentsRepository) =
        GetCatalogUseCase(repo)

    @Provides
    fun provideGetDetailUseCase(repo: ContentsRepository) =
        GetDetailUseCase(repo)

    @Provides
    fun provideSaveProgressUseCase(
        historyRepo: com.tvanime.app.data.repository.HistoryRepository
    ) = SaveProgressUseCase(historyRepo)

    @Provides
    fun provideGetHistoryUseCase(
        historyRepo: com.tvanime.app.data.repository.HistoryRepository
    ) = GetHistoryUseCase(historyRepo)

    @Provides
    fun provideToggleFavoriteUseCase(
        favoritesRepo: com.tvanime.app.data.repository.FavoritesRepository
    ) = ToggleFavoriteUseCase(favoritesRepo)
}

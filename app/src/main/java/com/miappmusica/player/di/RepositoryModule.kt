package com.miappmusica.player.di

import com.miappmusica.player.data.repository.LibraryRepositoryImpl
import com.miappmusica.player.data.repository.MetadataRepositoryImpl
import com.miappmusica.player.data.repository.ModeRepositoryImpl
import com.miappmusica.player.data.repository.PlaylistRepositoryImpl
import com.miappmusica.player.data.repository.TransferRepositoryImpl
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.domain.repository.MetadataRepository
import com.miappmusica.player.domain.repository.ModeRepository
import com.miappmusica.player.domain.repository.PlaylistRepository
import com.miappmusica.player.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindModeRepository(impl: ModeRepositoryImpl): ModeRepository

    @Binds
    @Singleton
    abstract fun bindMetadataRepository(impl: MetadataRepositoryImpl): MetadataRepository

    @Binds
    @Singleton
    abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository
}

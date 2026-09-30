package com.jarvis.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun networkEvents(): NetworkEventBus = NetworkEventBus()
    @Provides @Singleton fun networkInventory(): NetworkInventory = NetworkInventory()
}

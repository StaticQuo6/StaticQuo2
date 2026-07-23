package com.staticquo.di

import com.lambdapioneer.argon2kt.Argon2Kotlin
import com.lambdapioneer.argon2kt.Argon2KotlinSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideArgon2(): Argon2Kotlin {
        return Argon2Kotlin(Argon2KotlinSettings.builder().build())
    }
}

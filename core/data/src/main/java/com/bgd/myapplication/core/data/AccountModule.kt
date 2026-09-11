package com.bgd.myapplication.core.data

import com.bgd.myapplication.core.domain.AuthenticationRepository
import com.bgd.myapplication.core.domain.CollectionRepository
import com.bgd.myapplication.core.domain.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {
    @Binds abstract fun banners(repository: BannerRepository): com.bgd.myapplication.core.domain.BannerRepository
    @Binds abstract fun articles(repository: ArticleRepository): com.bgd.myapplication.core.domain.ArticleRepository
    @Binds abstract fun projects(repository: ProjectRepository): com.bgd.myapplication.core.domain.ProjectRepository
    @Binds abstract fun authentication(repository: DefaultAuthenticationRepository): AuthenticationRepository
    @Binds abstract fun collections(repository: DefaultCollectionRepository): CollectionRepository
    @Binds abstract fun session(repository: SessionManager): SessionRepository
}

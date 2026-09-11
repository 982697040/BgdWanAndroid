package com.bgd.myapplication.core.data

import com.bgd.myapplication.core.model.Banner
import com.bgd.myapplication.core.network.BannerApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BannerRepository @Inject constructor(private val api: BannerApi) : com.bgd.myapplication.core.domain.BannerRepository {
    override suspend fun getBanners(): List<Banner> = api.getBanners()
        .filter { it.isVisible == 1 && it.imagePath.isNotBlank() }
        .distinctBy { it.id }
        .map { Banner(it.id, it.title, it.imagePath, it.url) }
}

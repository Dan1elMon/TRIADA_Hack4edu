package com.example.triada.domain.repository

import com.example.triada.domain.model.LearningCatalog

interface CatalogRepository {
    suspend fun load(profileId: String): LearningCatalog
}

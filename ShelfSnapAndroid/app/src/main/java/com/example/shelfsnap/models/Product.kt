package com.example.shelfsnap.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a product in the catalogue. This mirrors the `products` table
 * defined in the Supabase schema and can be deserialized directly from
 * PostgREST responses using KotlinX serialization.
 */
@Serializable
data class Product(
    val id: String,
    val sku: String,
    val name: String,
    val upc: String? = null,
    val brand: String? = null,
    val category: String? = null,
    @SerialName("width_mm") val widthMm: Int,
    @SerialName("height_mm") val heightMm: Int? = null,
    @SerialName("depth_mm") val depthMm: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null
)
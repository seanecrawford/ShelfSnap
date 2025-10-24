package com.example.shelfsnap.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a planogram configuration. Each planogram has a unique ID and metadata
 * describing the layout such as the number of shelves and shelf dimensions. It may
 * optionally reference an image of the planogram and the store or section where it
 * applies. This data model corresponds to the `planograms` table in Supabase.
 */
@Serializable
data class Planogram(
    val id: String,
    val name: String,
    val section: String? = null,
    @SerialName("store_id") val storeId: String? = null,
    @SerialName("shelves_count") val shelvesCount: Int,
    @SerialName("shelf_width_mm") val shelfWidthMm: Int,
    @SerialName("shelf_height_mm") val shelfHeightMm: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
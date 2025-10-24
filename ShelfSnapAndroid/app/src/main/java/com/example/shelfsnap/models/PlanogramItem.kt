package com.example.shelfsnap.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a product placement within a planogram. Each item references a product and
 * specifies its location on a particular shelf via normalized coordinates (x offset and
 * width). The facing count indicates how many units of the product should appear
 * consecutively in that slot. This aligns with the `planogram_items` table in Supabase.
 */
@Serializable
data class PlanogramItem(
    val id: String,
    @SerialName("planogram_id") val planogramId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("shelf_index") val shelfIndex: Int,
    @SerialName("x_mm") val xMm: Int,
    @SerialName("width_mm") val widthMm: Int,
    val facings: Int = 1,
    val notes: String? = null
)
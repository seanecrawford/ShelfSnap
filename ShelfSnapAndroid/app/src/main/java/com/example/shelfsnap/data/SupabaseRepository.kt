package com.example.shelfsnap.data

import com.example.shelfsnap.models.Planogram
import com.example.shelfsnap.models.PlanogramItem
import com.example.shelfsnap.models.Product
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json

/**
 * A repository that encapsulates access to Supabase tables via the Supabase-kt client. This
 * abstraction decouples the rest of the application from the specifics of the Supabase API
 * and enables easier testing/mocking. Each method performs a simple PostgREST query and
 * deserializes the result into the corresponding data classes using KotlinX serialization.
 */
class SupabaseRepository(private val manager: SupabaseManager = SupabaseManager) {
    private val client = manager.client

    /**
     * Fetches all planograms from the database. Returns an empty list on error or if no
     * planograms are found. You can extend this method to filter by store or section by
     * adding query parameters to the request.
     */
    suspend fun getPlanograms(): List<Planogram> {
        return try {
            client.postgrest.from("planograms").select().decodeList<Planogram>()
        } catch (e: Exception) {
            // In production you may want to log this exception and propagate a more useful error
            emptyList()
        }
    }

    /**
     * Fetches all products from the database. Returns an empty list on failure.
     */
    suspend fun getProducts(): List<Product> {
        return try {
            client.postgrest.from("products").select().decodeList<Product>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches all planogram items for a given planogram ID. Returns an empty list on failure.
     */
    suspend fun getPlanogramItems(planogramId: String): List<PlanogramItem> {
        return try {
            client.postgrest.from("planogram_items").select {
                filter { eq("planogram_id", planogramId) }
            }.decodeList<PlanogramItem>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
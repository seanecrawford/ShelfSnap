package com.example.shelfsnap.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime

/**
 * A singleton provider for the Supabase client. It encapsulates the initialization
 * of the Supabase client using the project URL and public anonymous key defined
 * in [Constants]. The client is configured with the PostgREST and realtime modules
 * to enable database access and realtime subscriptions. Modify this object if you
 * need to install additional modules (e.g. authentication, storage) or customize
 * client behaviour.
 */
object SupabaseManager {
    /**
     * Lazily initialized Supabase client. The client is created on first access using
     * the Supabase project URL and API key defined in [Constants].
     */
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = Constants.SUPABASE_URL,
            supabaseKey = Constants.SUPABASE_ANON_KEY
        ) {
            install(io.github.jan.supabase.postgrest.Postgrest)
            install(io.github.jan.supabase.realtime.Realtime)
        }
    }
}
package com.example.shelfsnap.data

/**
 * Holds configuration constants for connecting to Supabase. Replace the placeholder values
 * with your actual Supabase project URL and public anonymous API key. These values are
 * intentionally stored in code for demonstration purposes; in a real application you should
 * inject them via a secure configuration mechanism (e.g. build config fields or environment
 * variables) rather than hardcoding them.
 */
object Constants {
    /**
     * The base URL of your Supabase project (e.g. "https://xyzcompany.supabase.co").
     */
    const val SUPABASE_URL: String = "https://YOUR_SUPABASE_URL"

    /**
     * The public anonymous key for your Supabase project. This key provides read/write
     * access governed by your Row Level Security policies. Do not embed a service role key
     * in your mobile app, as it bypasses RLS and could expose sensitive data【22†L189-L198】.
     */
    const val SUPABASE_ANON_KEY: String = "YOUR_SUPABASE_ANON_KEY"
}
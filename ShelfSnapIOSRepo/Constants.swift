//
//  Constants.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import Foundation

/// Holds shared configuration values.  Replace the placeholder values
/// with your Supabase project’s URL and anon key.  These values are
/// required to initialize the Supabase client and should not be
/// committed to public repositories.
enum Constants {
    /// Your Supabase project URL, e.g. "https://xyzcompany.supabase.co"
    static let supabaseURL: String = "https://YOUR_SUPABASE_URL.supabase.co"

    /// Your Supabase anon (public) key.  Find this under `API → Project API keys`.
    static let supabaseAnonKey: String = "YOUR_SUPABASE_ANON_KEY"
}
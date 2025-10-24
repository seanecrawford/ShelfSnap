//
//  SupabaseManager.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import Foundation
import Supabase

/// A singleton that manages the Supabase client.  It exposes simple
/// methods for fetching products and planograms from your Supabase
/// Postgres database.  All requests use the anonymous API key defined
/// in `Constants`.
class SupabaseManager {
    static let shared = SupabaseManager()

    /// The underlying Supabase client.  This client will be configured
    /// with Postgres (PostgREST) and Realtime capabilities.  Add other
    /// modules as needed.
    let client: SupabaseClient

    private init() {
        let url = URL(string: Constants.supabaseURL)!
        let key = Constants.supabaseAnonKey
        self.client = SupabaseClient(supabaseURL: url, supabaseKey: key)
    }

    // MARK: - Products

    /// Fetches all products from the `products` table.  This call is
    /// asynchronous and returns an array of `Product` records.  If the
    /// network request fails or decoding fails, the returned array
    /// will be empty.
    func fetchProducts() async -> [Product] {
        do {
            let response: [Product] = try await client
                .from("products")
                .select().execute().value
            return response
        } catch {
            print("Supabase fetchProducts error: \(error)")
            return []
        }
    }

    // MARK: - Planograms

    /// Fetches all planograms from the `planograms` table.  See
    /// `Models.swift` for the Planogram struct definition.  This
    /// function returns an array of planograms or an empty array if
    /// any error occurs.
    func fetchPlanograms() async -> [Planogram] {
        do {
            let response: [Planogram] = try await client
                .from("planograms")
                .select().execute().value
            return response
        } catch {
            print("Supabase fetchPlanograms error: \(error)")
            return []
        }
    }

    /// Fetches all planogram items for a given planogram ID.  This is
    /// useful when editing an existing planogram.  The returned list is
    /// sorted by shelf index and x position so that items appear left
    /// to right on each shelf.
    func fetchPlanogramItems(planogramId: UUID) async -> [PlanogramItem] {
        do {
            let response: [PlanogramItem] = try await client
                .from("planogram_items")
                .select()
                .eq("planogram_id", value: planogramId.uuidString)
                .order("shelf_index", ascending: true)
                .order("x_mm", ascending: true)
                .execute().value
            return response
        } catch {
            print("Supabase fetchPlanogramItems error: \(error)")
            return []
        }
    }

    /// Inserts a new planogram and its items into the database.  This
    /// method uses two separate requests: one to insert the planogram
    /// record, and a second to insert the associated planogram items.  In
    /// practice you might wrap these calls in an RPC or use database
    /// transactions for atomicity.  Returns `true` on success.
    func insertPlanogram(name: String, section: String?, shelvesCount: Int, shelfWidthMm: Int, items: [(productId: UUID, shelfIndex: Int, xMm: Int, widthMm: Int, facings: Int)]) async -> Bool {
        do {
            // Insert planogram
            let planogramId = UUID()
            let planogramRecord: [String: Any] = [
                "id": planogramId.uuidString,
                "name": name,
                "section": section as Any?,
                "shelves_count": shelvesCount,
                "shelf_width_mm": shelfWidthMm,
                "created_at": Date().iso8601String()
            ].compactMapValues { $0 }
            _ = try await client.from("planograms").insert(values: planogramRecord).execute()

            // Insert items
            let itemRecords = items.map { item -> [String: Any] in
                return [
                    "id": UUID().uuidString,
                    "planogram_id": planogramId.uuidString,
                    "product_id": item.productId.uuidString,
                    "shelf_index": item.shelfIndex,
                    "x_mm": item.xMm,
                    "width_mm": item.widthMm,
                    "facings": item.facings
                ]
            }
            _ = try await client.from("planogram_items").insert(values: itemRecords).execute()

            return true
        } catch {
            print("Supabase insertPlanogram error: \(error)")
            return false
        }
    }
}

private extension Date {
    /// Formats the date as an ISO8601 string accepted by Supabase.
    func iso8601String() -> String {
        let formatter = ISO8601DateFormatter()
        return formatter.string(from: self)
    }
}
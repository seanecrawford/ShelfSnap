//
//  Models.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import Foundation

/// Represents a product in the catalog.  This mirrors the `products` table
/// defined in your Supabase database.  You can extend this model
/// with additional fields as needed.
struct Product: Identifiable, Codable, Hashable {
    let id: UUID
    let sku: String
    let name: String
    let upc: String?
    let brand: String?
    let category: String?
    let widthMm: Int
    let heightMm: Int?
    let depthMm: Int?
    let imageUrl: String?
}

/// Represents a planogram record.  Each planogram has a number of shelves
/// and a physical width in millimetres.  The `items` relationship is
/// represented by the `PlanogramItem` model.
struct Planogram: Identifiable, Codable, Hashable {
    let id: UUID
    let name: String
    let section: String?
    let shelvesCount: Int
    let shelfWidthMm: Int
    let shelfHeightMm: Int?
    let createdAt: Date?
}

/// Represents a product placement within a planogram.  Each item
/// corresponds to a shelf index and a horizontal offset (x) from the
/// left edge in millimetres.  The `facings` field indicates how many
/// facings (columns) the product occupies.
struct PlanogramItem: Identifiable, Codable, Hashable {
    let id: UUID
    let planogramId: UUID
    let productId: UUID
    let shelfIndex: Int
    let xMm: Int
    let widthMm: Int
    let facings: Int
}

/// A detection result returned by ML Kit.  We store the bounding box
/// relative to the image as a normalized CGRect (x, y, width, height all
/// between 0 and 1).  Category and confidence are optional and may
/// contain coarse classification values provided by ML Kit.
struct Detection: Identifiable, Hashable {
    let id = UUID()
    let rect: CGRect
    let category: String?
    let confidence: Float?
}

/// Represents an editable object on the screen.  The bounding box is
/// stored in normalized coordinates and the offset represents the drag
/// translation applied by the user.  When `offset` is non‑zero, the
/// object has been moved from its original detection position.
struct EditableObject: Identifiable, Hashable {
    let id = UUID()
    var rect: CGRect
    var offset: CGSize = .zero
}
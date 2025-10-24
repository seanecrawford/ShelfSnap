//
//  PlanogramBuilderView.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import SwiftUI

/// A builder that lets the user assemble a planogram layout.  The
/// grid defaults to three rows and four columns but can be changed at
/// runtime.  Each cell of the grid contains a picker to choose a
/// product from the provided list.  When the user saves, the
/// callback is invoked with the constructed planogram details.
struct PlanogramBuilderView: View {
    let existingPlanogram: Planogram?
    let onSave: (Planogram, [(productId: UUID, shelfIndex: Int, xMm: Int, widthMm: Int, facings: Int)]) -> Void
    let onCancel: () -> Void

    @State private var products: [Product] = []
    @State private var shelvesCount: Int
    @State private var columnsCount: Int = 4
    @State private var selections: [[UUID?]]
    @State private var isLoading = true

    init(existingPlanogram: Planogram? = nil,
         onSave: @escaping (Planogram, [(productId: UUID, shelfIndex: Int, xMm: Int, widthMm: Int, facings: Int)]) -> Void,
         onCancel: @escaping () -> Void) {
        self.existingPlanogram = existingPlanogram
        self.onSave = onSave
        self.onCancel = onCancel
        // Initialize shelves count and selections based on existing planogram
        let rows = existingPlanogram?.shelvesCount ?? 3
        _shelvesCount = State(initialValue: rows)
        let cols = 4
        // Create an empty grid of optional UUIDs
        let emptyGrid = Array(repeating: Array(repeating: UUID?.none, count: cols), count: rows)
        _selections = State(initialValue: emptyGrid)
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Dimensions")) {
                    Stepper(value: $shelvesCount, in: 1...6) {
                        Text("Shelves: \(shelvesCount)")
                    }
                    Stepper(value: $columnsCount, in: 1...8) {
                        Text("Columns: \(columnsCount)")
                    }
                }
                Section(header: Text("Layout")) {
                    if isLoading {
                        ProgressView("Loading products…")
                    } else {
                        // Ensure selections array matches current dimensions
                        let _ = ensureGridSize()
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(0..<shelvesCount, id: \ .self) { row in
                                HStack {
                                    ForEach(0..<columnsCount, id: \ .self) { col in
                                        Menu(content: {
                                            ForEach(products, id: \ .id) { product in
                                                Button(product.name) {
                                                    selections[row][col] = product.id
                                                }
                                            }
                                            Button("Empty") {
                                                selections[row][col] = nil
                                            }
                                        }) {
                                            let productId = selections[row][col]
                                            let name = products.first(where: { $0.id == productId })?.name ?? "Select"
                                            Text(name)
                                                .font(.footnote)
                                                .frame(maxWidth: .infinity)
                                                .padding(4)
                                                .background(Color.gray.opacity(0.2))
                                                .cornerRadius(4)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(existingPlanogram != nil ? "Edit Planogram" : "New Planogram")
            .navigationBarItems(leading: Button("Cancel", action: onCancel),
                                trailing: Button("Save", action: save))
            .onAppear {
                Task {
                    await loadProducts()
                }
            }
        }
    }

    /// Loads the list of products from Supabase.  If no products are
    /// returned (e.g. the database is empty), a few sample products are
    /// inserted to allow the builder to function.
    private func loadProducts() async {
        let fetched = await SupabaseManager.shared.fetchProducts()
        DispatchQueue.main.async {
            if fetched.isEmpty {
                // Provide some placeholder products for testing
                self.products = [
                    Product(id: UUID(), sku: "milk", name: "Milk", upc: nil, brand: nil, category: nil, widthMm: 100, heightMm: nil, depthMm: nil, imageUrl: nil),
                    Product(id: UUID(), sku: "bread", name: "Bread", upc: nil, brand: nil, category: nil, widthMm: 120, heightMm: nil, depthMm: nil, imageUrl: nil),
                    Product(id: UUID(), sku: "cereal", name: "Cereal", upc: nil, brand: nil, category: nil, widthMm: 150, heightMm: nil, depthMm: nil, imageUrl: nil),
                    Product(id: UUID(), sku: "juice", name: "Orange Juice", upc: nil, brand: nil, category: nil, widthMm: 200, heightMm: nil, depthMm: nil, imageUrl: nil)
                ]
            } else {
                self.products = fetched
            }
            self.isLoading = false
        }
    }

    /// Ensures the selections grid matches the current shelves/columns count.
    /// If the user changes the number of shelves or columns, this
    /// function resizes the selections array accordingly.
    private func ensureGridSize() {
        // Adjust rows
        if selections.count < shelvesCount {
            for _ in selections.count..<shelvesCount {
                selections.append(Array(repeating: nil, count: columnsCount))
            }
        } else if selections.count > shelvesCount {
            selections = Array(selections.prefix(shelvesCount))
        }
        // Adjust columns
        for row in 0..<selections.count {
            if selections[row].count < columnsCount {
                selections[row] += Array(repeating: nil, count: columnsCount - selections[row].count)
            } else if selections[row].count > columnsCount {
                selections[row] = Array(selections[row].prefix(columnsCount))
            }
        }
    }

    /// Constructs a new `Planogram` and associated items from the
    /// selections and invokes the `onSave` callback.  The planogram
    /// width is computed by multiplying the number of columns by an
    /// arbitrary width (400 mm per column) for demonstration purposes.
    private func save() {
        let planogramId = existingPlanogram?.id ?? UUID()
        let name = existingPlanogram?.name ?? "Untitled"
        let planogram = Planogram(id: planogramId,
                                  name: name,
                                  section: nil,
                                  shelvesCount: shelvesCount,
                                  shelfWidthMm: columnsCount * 400,
                                  shelfHeightMm: nil,
                                  createdAt: Date())
        var items: [(UUID, Int, Int, Int, Int)] = []
        for row in 0..<shelvesCount {
            for col in 0..<columnsCount {
                if let productId = selections[row][col] {
                    let xMm = col * 400
                    // For now each item occupies one column and one facing
                    items.append((productId, row, xMm, 400, 1))
                }
            }
        }
        onSave(planogram, items)
    }
}

struct PlanogramBuilderView_Previews: PreviewProvider {
    static var previews: some View {
        PlanogramBuilderView(existingPlanogram: nil, onSave: { _, _ in }, onCancel: {})
    }
}
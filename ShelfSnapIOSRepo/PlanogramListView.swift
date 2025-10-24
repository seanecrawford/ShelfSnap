//
//  PlanogramListView.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import SwiftUI

/// Displays a list of planograms fetched from Supabase.  When a
/// planogram is tapped, the `onSelect` callback is invoked with the
/// selected planogram.  The list is loaded asynchronously on
/// appearance.
struct PlanogramListView: View {
    @State private var planograms: [Planogram] = []
    var onSelect: (Planogram) -> Void

    var body: some View {
        NavigationView {
            List(planograms) { planogram in
                Button(action: {
                    onSelect(planogram)
                }) {
                    VStack(alignment: .leading) {
                        Text(planogram.name)
                            .font(.headline)
                        Text("Shelves: \(planogram.shelvesCount)" )
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Planograms")
            .task {
                await loadPlanograms()
            }
        }
    }

    private func loadPlanograms() async {
        let fetched = await SupabaseManager.shared.fetchPlanograms()
        DispatchQueue.main.async {
            self.planograms = fetched
        }
    }
}

struct PlanogramListView_Previews: PreviewProvider {
    static var previews: some View {
        PlanogramListView { _ in }
    }
}
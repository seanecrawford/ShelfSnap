//
//  ContentView.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import SwiftUI

/// The root view of the ShelfSnap iOS app.  Presents primary
/// navigation options: scanning a shelf, viewing planograms, and
/// creating a new planogram.  Uses sheets to present secondary
/// screens.
struct ContentView: View {
    @State private var isShowingScan = false
    @State private var isShowingPlanogramList = false
    @State private var isShowingPlanogramBuilder = false
    @State private var selectedPlanogram: Planogram? = nil

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                Button(action: { isShowingScan = true }) {
                    Text("Capture Shelf")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                Button(action: { isShowingPlanogramList = true }) {
                    Text("View Planograms")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                Button(action: {
                    selectedPlanogram = nil
                    isShowingPlanogramBuilder = true
                }) {
                    Text("New Planogram")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.orange)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                Spacer()
            }
            .padding()
            .navigationTitle("ShelfSnap")
            // Present scan view full screen
            .fullScreenCover(isPresented: $isShowingScan) {
                ScanView()
                    .onDisappear {
                        // Nothing to do
                    }
            }
            // Present planogram list in a sheet
            .sheet(isPresented: $isShowingPlanogramList) {
                PlanogramListView { planogram in
                    // When a planogram is selected, dismiss list and show builder
                    selectedPlanogram = planogram
                    isShowingPlanogramList = false
                    isShowingPlanogramBuilder = true
                }
            }
            // Present planogram builder
            .sheet(isPresented: $isShowingPlanogramBuilder) {
                PlanogramBuilderView(existingPlanogram: selectedPlanogram,
                                     onSave: { planogram, items in
                    // Save the planogram to Supabase asynchronously
                    Task {
                        let success = await SupabaseManager.shared.insertPlanogram(
                            name: planogram.name,
                            section: planogram.section,
                            shelvesCount: planogram.shelvesCount,
                            shelfWidthMm: planogram.shelfWidthMm,
                            items: items
                        )
                        print("Insert planogram success: \(success)")
                    }
                    // Dismiss builder
                    selectedPlanogram = nil
                    isShowingPlanogramBuilder = false
                }, onCancel: {
                    selectedPlanogram = nil
                    isShowingPlanogramBuilder = false
                })
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
//
//  CameraView.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import SwiftUI
import AVFoundation

/// A SwiftUI wrapper around `PreviewView` that starts and stops the
/// camera via the provided `ScanViewModel`.  When this view appears
/// it begins the video capture and detection loop; when it disappears
/// it stops the camera session to free up resources.
struct CameraView: UIViewRepresentable {
    @ObservedObject var viewModel: ScanViewModel

    func makeUIView(context: Context) -> PreviewView {
        let preview = PreviewView()
        // Configure camera after a short delay to ensure layout is ready
        DispatchQueue.main.async {
            viewModel.startCamera(in: preview)
        }
        return preview
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {
        // Nothing to update on each state change
    }

    static func dismantleUIView(_ uiView: PreviewView, coordinator: ()) {
        // Stop camera when the view is removed
        uiView.videoPreviewLayer.session = nil
    }
}
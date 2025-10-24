//
//  ScanView.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import SwiftUI

/// Displays the camera feed with either live detection overlays or
/// editable objects when detection is frozen.  Provides a button
/// to toggle between detection and editing.  Drag gestures allow the
/// user to move objects when frozen.
struct ScanView: View {
    @StateObject var viewModel = ScanViewModel()

    /// Tracks the size of the preview for converting normalized
    /// coordinates into actual points on screen.  The camera view
    /// fills the available space.
    @State private var previewSize: CGSize = .zero

    var body: some View {
        ZStack {
            GeometryReader { geo in
                CameraView(viewModel: viewModel)
                    .onAppear {
                        previewSize = geo.size
                    }
            }
            // Overlay detections or editable objects
            if viewModel.isFrozen {
                ForEach($viewModel.editableObjects) { $object in
                    // Compute the rectangle's frame based on normalized rect
                    let x = object.rect.origin.x * previewSize.width + object.offset.width
                    let y = object.rect.origin.y * previewSize.height + object.offset.height
                    let width = object.rect.size.width * previewSize.width
                    let height = object.rect.size.height * previewSize.height
                    Rectangle()
                        .stroke(Color.green, lineWidth: 2)
                        .frame(width: width, height: height)
                        .position(x: x + width / 2, y: y + height / 2)
                        .gesture(
                            DragGesture()
                                .onChanged { gesture in
                                    object.offset = CGSize(width: gesture.translation.width,
                                                           height: gesture.translation.height)
                                }
                        )
                }
            } else {
                ForEach(viewModel.detections) { detection in
                    let x = detection.rect.origin.x * previewSize.width
                    let y = detection.rect.origin.y * previewSize.height
                    let width = detection.rect.size.width * previewSize.width
                    let height = detection.rect.size.height * previewSize.height
                    Rectangle()
                        .stroke(Color.red, lineWidth: 2)
                        .frame(width: width, height: height)
                        .position(x: x + width / 2, y: y + height / 2)
                }
            }
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Button(action: toggleFreeze) {
                        Text(viewModel.isFrozen ? "Resume Detection" : "Freeze & Edit")
                            .padding(8)
                            .background(Color.blue.opacity(0.8))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .padding()
                }
            }
        }
        .ignoresSafeArea()
        .onChange(of: viewModel.isFrozen) { newValue in
            if newValue {
                // When freezing, convert detections into editable objects
                viewModel.editableObjects = viewModel.detections.map { det in
                    EditableObject(rect: det.rect)
                }
            } else {
                // Clear editable objects when resuming detection
                viewModel.editableObjects = []
            }
        }
    }

    /// Toggles detection on and off.  When turning off detection we
    /// capture the current detections and convert them to editable
    /// objects.  When turning on detection we clear all edits and
    /// allow new detections to stream in.
    private func toggleFreeze() {
        viewModel.isFrozen.toggle()
    }
}

struct ScanView_Previews: PreviewProvider {
    static var previews: some View {
        ScanView()
    }
}
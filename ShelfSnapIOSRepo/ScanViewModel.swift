//
//  ScanViewModel.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import Foundation
import AVFoundation
import MLKitObjectDetection
import MLKitVision
import Combine

/// View model responsible for configuring the camera pipeline and running
/// ML Kit’s on‑device object detector on each frame.  It maintains a
/// list of detection results that are published to the UI.  When
/// detection is frozen, new frames are ignored and existing detections
/// are converted to editable objects by the view.
class ScanViewModel: NSObject, ObservableObject {
    /// Published list of detections.  Each detection contains a
    /// normalized CGRect representing the bounding box and optional
    /// classification information.  This array is updated on the main
    /// thread after each processed frame.
    @Published var detections: [Detection] = []

    /// Published list of editable objects.  When not empty, the UI
    /// should ignore the `detections` array and instead allow the user
    /// to drag these objects around.  Each object stores its own
    /// offset to account for user interaction.
    @Published var editableObjects: [EditableObject] = []

    /// Whether detection is currently frozen.  When true, the
    /// `captureOutput(_:didOutput:from:)` delegate will return early
    /// without processing the frame.
    @Published var isFrozen: Bool = false

    /// The underlying camera session.  The session is configured to
    /// deliver video frames to this object via the
    /// `AVCaptureVideoDataOutputSampleBufferDelegate` protocol.
    private let session = AVCaptureSession()

    /// ML Kit object detector.  Configured for streaming mode with
    /// multiple object detection and classification disabled.  These
    /// options follow Google’s best practices for streaming mode,
    /// including disabling multiple objects if performance is an issue【732326118131143†L1499-L1508】.
    private lazy var objectDetector: ObjectDetector = {
        let options = ObjectDetectorOptions()
        options.detectorMode = .stream
        // Enable classification so that ML Kit will return coarse labels such as
        // "Food", "Fashion", etc.  This is necessary for misplacement detection.
        options.shouldEnableClassification = true
        options.shouldEnableMultipleObjects = true
        return ObjectDetector.objectDetector(options: options)
    }()

    /// Queue on which video frames are processed.  Using a serial queue
    /// ensures frames are handled one at a time and prevents overload.
    private let processingQueue = DispatchQueue(label: "com.shelfsnap.scan.processing")

    /// Configures the camera session and assigns the preview layer to
    /// the provided `PreviewView`.  You should call this from the
    /// `makeUIView` implementation of your `UIViewRepresentable`.
    func startCamera(in previewView: PreviewView) {
        session.beginConfiguration()
        session.sessionPreset = .high

        // Setup camera input
        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera,
                                                   for: .video,
                                                   position: .back),
              let input = try? AVCaptureDeviceInput(device: camera),
              session.canAddInput(input) else {
            print("Failed to configure camera input")
            return
        }
        session.addInput(input)

        // Setup video data output
        let output = AVCaptureVideoDataOutput()
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32BGRA)]
        output.setSampleBufferDelegate(self, queue: processingQueue)
        guard session.canAddOutput(output) else {
            print("Failed to add video output")
            return
        }
        session.addOutput(output)
        // Orient video for portrait
        output.connections.first?.videoOrientation = .portrait

        session.commitConfiguration()
        previewView.videoPreviewLayer.session = session
        session.startRunning()
    }

    /// Stops the camera session.  Call this when leaving the scan
    /// screen to release the camera resource.
    func stopCamera() {
        session.stopRunning()
    }
}

// MARK: - AVCaptureVideoDataOutputSampleBufferDelegate
extension ScanViewModel: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard !isFrozen else { return }
        processSampleBuffer(sampleBuffer)
    }

    /// Converts the sample buffer to a `VisionImage` with orientation
    /// metadata and runs the ML Kit object detector.  The resulting
    /// bounding boxes are normalized relative to the image size and
    /// published on the main thread【732326118131143†L1668-L1692】.
    private func processSampleBuffer(_ sampleBuffer: CMSampleBuffer) {
        // Determine image orientation based on device orientation
        let cameraPosition: AVCaptureDevice.Position = .back
        let orientation = imageOrientation(deviceOrientation: UIDevice.current.orientation,
                                           cameraPosition: cameraPosition)

        // Create a VisionImage from the sample buffer
        let visionImage = VisionImage(buffer: sampleBuffer)
        visionImage.orientation = orientation

        // Obtain pixel buffer dimensions for normalization
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            return
        }
        let width = CGFloat(CVPixelBufferGetWidth(pixelBuffer))
        let height = CGFloat(CVPixelBufferGetHeight(pixelBuffer))

        objectDetector.process(visionImage) { [weak self] detectedObjects, error in
            guard error == nil else {
                print("Object detector error: \(error!.localizedDescription)")
                return
            }
            guard let objects = detectedObjects, !objects.isEmpty else {
                DispatchQueue.main.async {
                    self?.detections = []
                }
                return
            }
            // Map detections to normalized rects
            var results: [Detection] = []
            for obj in objects {
                let frame = obj.frame
                // Normalize by the image dimensions
                let rect = CGRect(x: frame.origin.x / width,
                                  y: frame.origin.y / height,
                                  width: frame.size.width / width,
                                  height: frame.size.height / height)
                let category = obj.labels.first?.text
                let confidence = obj.labels.first?.confidence
                let detection = Detection(rect: rect,
                                          category: category,
                                          confidence: confidence)
                results.append(detection)
            }
            DispatchQueue.main.async {
                self?.detections = results
            }
        }
    }

    /// Converts the device orientation and camera position into a
    /// `UIImage.Orientation` accepted by ML Kit.  This helper
    /// implements the orientation mapping recommended by Google【732326118131143†L1576-L1593】.
    private func imageOrientation(deviceOrientation: UIDeviceOrientation, cameraPosition: AVCaptureDevice.Position) -> UIImage.Orientation {
        switch deviceOrientation {
        case .portrait:
            return cameraPosition == .front ? .leftMirrored : .right
        case .landscapeLeft:
            return cameraPosition == .front ? .downMirrored : .up
        case .portraitUpsideDown:
            return cameraPosition == .front ? .rightMirrored : .left
        case .landscapeRight:
            return cameraPosition == .front ? .upMirrored : .down
        default:
            return .right
        }
    }
}
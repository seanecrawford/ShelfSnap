//
//  PreviewView.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import UIKit
import AVFoundation

/// A UIView subclass whose backing layer is an AVCaptureVideoPreviewLayer.
/// This view displays the live camera feed.  You assign the
/// `session` on the `videoPreviewLayer` property when starting the
/// camera in your `ScanViewModel`.
class PreviewView: UIView {
    override class var layerClass: AnyClass {
        return AVCaptureVideoPreviewLayer.self
    }

    /// Convenience accessor for the view’s underlying layer as an
    /// AVCaptureVideoPreviewLayer.  This property is used by the
    /// view model to assign the camera session.
    var videoPreviewLayer: AVCaptureVideoPreviewLayer {
        return layer as! AVCaptureVideoPreviewLayer
    }
}
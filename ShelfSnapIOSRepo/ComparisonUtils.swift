//
//  ComparisonUtils.swift
//  ShelfSnapIOS
//
//  Created by ChatGPT on 2025-10-12.
//

import Foundation
import CoreGraphics

/// Represents a discrepancy between a planogram item and a detected object.  A discrepancy
/// can be `.missing` if a planogram item has no corresponding detection, `.overstock` if
/// a detection lies outside any defined planogram cell, or `.misplaced` if a detection is
/// found in a cell with a different product (not currently implemented).  The
/// `planogramItemId` refers to the missing item (for `.missing`) and the
/// `detectionId` refers to the offending detection (for `.overstock` or `.misplaced`).
struct Discrepancy: Identifiable {
    enum DiscrepancyType {
        case missing
        case overstock
        case misplaced
    }
    let id = UUID()
    let type: DiscrepancyType
    let planogramItemId: UUID?
    let detectionId: UUID?
}

/// Provides functions to compare detections against a planogram layout and report
/// discrepancies.  This implementation mirrors the algorithm used on Android: it
/// subdivides the shelf into rows based on `shelvesCount` and uses each item's
/// `xMm` and `widthMm` to calculate horizontal boundaries.  Detection centers are
/// checked against these boundaries to determine matches.  Items without a
/// detection are marked as missing; detections without a matching cell are
/// considered overstock.  Misplacement is not yet implemented.
enum ComparisonUtils {
    static func comparePlanogram(planogram: Planogram, items: [PlanogramItem], detections: [Detection]) -> [Discrepancy] {
        var discrepancies: [Discrepancy] = []
        var matchedDetectionIDs: Set<UUID> = []
        let shelvesCount = planogram.shelvesCount
        let shelfWidth = Double(planogram.shelfWidthMm)
        for item in items {
            let rowIndex = item.shelfIndex
            let cellLeftNorm = Double(item.xMm) / shelfWidth
            let cellWidthNorm = Double(item.widthMm) / shelfWidth
            let cellRightNorm = cellLeftNorm + cellWidthNorm
            let cellTopNorm = Double(rowIndex) / Double(shelvesCount)
            let cellHeightNorm = 1.0 / Double(shelvesCount)
            let cellBottomNorm = cellTopNorm + cellHeightNorm
            // Find a detection whose center lies in this cell
            var found: Detection? = nil
            for det in detections {
                let centerX = det.rect.midX
                let centerY = det.rect.midY
                if centerX >= CGFloat(cellLeftNorm) && centerX <= CGFloat(cellRightNorm) &&
                    centerY >= CGFloat(cellTopNorm) && centerY <= CGFloat(cellBottomNorm) {
                    found = det
                    break
                }
            }
            if let detection = found {
                matchedDetectionIDs.insert(detection.id)
                // If the detection has a label and it does not match the planogram item's
                // productId, then mark this as a misplaced item.  This simplistic check
                // assumes that the ML Kit classification label corresponds to a product ID.
                // In practice you may want to compare against product names or categories.
                if let detLabel = detection.category, detLabel != item.productId.uuidString {
                    discrepancies.append(Discrepancy(type: .misplaced, planogramItemId: item.id, detectionId: detection.id))
                }
            } else {
                // Missing planogram item
                discrepancies.append(Discrepancy(type: .missing, planogramItemId: item.id, detectionId: nil))
            }
        }
        // Any detection not matched to a cell is overstock
        for det in detections where !matchedDetectionIDs.contains(det.id) {
            discrepancies.append(Discrepancy(type: .overstock, planogramItemId: nil, detectionId: det.id))
        }
        return discrepancies
    }
}
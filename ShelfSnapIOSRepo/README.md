# ShelfSnap iOS App

This directory contains a simple SwiftUI iOS application that mirrors the core
functionality of the Android ShelfSnap prototype.  The goal of this app is to
allow a user to scan a retail shelf section, detect objects on the shelf using
**ML Kit** on‑device object detection, and then turn those detected boxes into
draggable objects for manual adjustment.  It also includes screens for
viewing and building planograms backed by a **Supabase** database.

## Getting started

1. **Install dependencies:**  This project uses the [CocoaPods](https://cocoapods.org/)
   package manager to include Google ML Kit.  From the `ShelfSnapIOS` folder run:

   ```sh
   pod init
   # Replace the generated Podfile with the contents of the provided Podfile
   cp Podfile Podfile.bak # backup just in case
   pod install
   ```

   CocoaPods will create an `ShelfSnapIOS.xcworkspace` file.  Always open
   the `.xcworkspace` file in Xcode (not the `.xcodeproj`).

2. **Add Supabase:**  The Supabase Swift client is distributed via Swift
   Package Manager.  In Xcode, open your workspace and choose
   **File → Add Packages…** then paste the following URL into the search field:

   ```
   https://github.com/supabase-community/supabase-swift.git
   ```

   Choose the latest version and add the `Supabase` product to the
   `ShelfSnapIOS` target.  This will allow you to query and update your
   Supabase project from Swift.

3. **Configure your Supabase credentials:**  Update the values in
   `Constants.swift` with your project URL and anon key.  These strings are
   available in the **API** section of your Supabase project settings.

4. **Open the workspace:**  Double‑click `ShelfSnapIOS.xcworkspace` to open
   the project in Xcode.  Build and run it on a device or simulator.  The
   first time you access the camera, iOS will ask for permission.

5. **Running on device:**  To run on your physical iPhone, you will need
   a valid provisioning profile or a free Apple developer account.  Connect
   your phone via USB, select it in the Xcode toolbar, and press **Run**.

## Important notes

* **Object detection:**  The included `ScanViewModel` uses ML Kit’s on‑device
  object detection API in **stream** mode to process video frames.  It
  converts each frame from the camera into a `VisionImage`, runs
  `objectDetector.process(image)` and returns bounding boxes with
  classifications.  The code is based on Google’s official documentation,
  which explains how to configure `VisionObjectDetectorOptions` and process
  frames asynchronously【732326118131143†L1499-L1508】【732326118131143†L1668-L1692】.

* **Editing detected objects:**  When the user taps the **Freeze & Edit**
  button, the app stops real‑time detection and converts each bounding box
  into a draggable square using SwiftUI’s `DragGesture`.  Tapping **Resume
  Detection** discards edits and resumes live detection.

* **Planogram builder:**  The planogram builder is a simple grid where
  each cell contains a drop‑down list of products fetched from Supabase.
  When saved, the builder constructs a `Planogram` and associated
  `PlanogramItem` objects that could be persisted via the `SupabaseManager`.

* **Supabase security:**  This sample assumes your database tables allow
  anonymous reads/writes via Row Level Security policies.  Never embed a
  service role key in a mobile app; always use the **anon** key and
  secure your tables appropriately【22†L175-L183】.

This codebase is intended to serve as a starting point.  You can extend it
with additional features such as product classification, planogram
compliance reports, or AI recommendations.  See the Android project for a
reference implementation of these concepts.
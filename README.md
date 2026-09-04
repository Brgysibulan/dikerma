# Barangay ID Maker

Offline Android ID maker for Barangay Sibulan.

## Current Status
The app is already able to manage employee ID data offline and generate a print-ready A4 PDF. The latest build also includes the corrected paper-slot layout based on the actual `ID SIZE.pub` print template plus fully offline photo and signature cleanup.

## Offline-Only Policy
This app is intentionally designed to work without internet access after installation.

- No `android.permission.INTERNET` permission is granted to the app.
- The Android manifest explicitly removes the INTERNET permission if a future library/dependency tries to add it during manifest merging.
- GitHub Actions checks the merged Android manifests after every build and fails the build if INTERNET permission appears.
- Employee records, photos, signatures, logos, templates, settings, QR data, image processing, and PDF generation are stored/processed locally on the Android device.
- Android application backup is disabled so the app stays aligned with the local-only data policy.
- GitHub is only used to develop and build new APK versions; the installed app itself does not need GitHub or a network connection to operate.

## Current Features
- Single-user Android app
- Offline employee records using Room database
- Employee photo and signature per record
- Camera or Gallery input for employee photo and signature
- Existing ID-photo upload from Gallery/files; taking a new picture is not required
- Uploaded ID photos and camera photos use the same fully offline white-background cleanup workflow
- Fully offline plain-background cleanup for ID photos
- Fully offline paper/background removal for signatures
- Processed image preview before saving the employee record
- Automatic correction of common image-orientation metadata before processing
- Safe cleanup of obsolete app-generated processed images when replacing, clearing, cancelling or deleting records
- WEBV3LITE QR image can be uploaded per employee and placed on the printed ID
- Stored Logo 1 and Logo 2
- Stored Punong Barangay name, position and signature
- Uploadable front and back ID design templates
- Permission-safe Android document/image pickers
- System camera capture without granting the app a permanent camera permission
- No broad storage, camera, contacts, location, microphone, notification, or internet permission
- A4 portrait PDF generation
- Supports 1 or 2 persons per A4 sheet
- Print instruction: use **Actual Size / 100%** and do not use **Fit to Page**

## ID Photo Input: Camera or Existing Photo Upload
The user does not need to take every employee photo inside the app.

There are two supported ways to provide an employee ID photo:

1. **Camera** — take a new picture using the device camera.
2. **Gallery / File Upload** — choose an existing ID photo already saved on the device.

Both sources use the same offline processing pipeline before the photo is saved to the employee record. This means an already-existing employee ID photo can be uploaded directly and the app will still attempt to remove its plain background, replace that background with pure white, auto-crop the subject, and show a processed preview.

If the uploaded image already has a clean white background, it can still be selected from Gallery/files and processed locally without requiring a new camera capture.

## Offline ID Photo Cleanup
The employee editor supports **Camera** and **Gallery / Upload** for the ID photo.

Recommended capture/upload setup:

- Use a photo where the person is in front of a **plain solid-color background** whenever background replacement is needed.
- Use even lighting and avoid strong shadows on the background.
- Use a background color that contrasts with the person's hair and clothing.
- Keep the head and shoulders clearly separated from the background.
- Existing ID photos from the device may be uploaded instead of taking a new picture.

Processing happens fully on-device:

1. The app reads the image and corrects common orientation metadata when needed.
2. It estimates the plain background color from the image edges.
3. It removes the edge-connected plain background.
4. It replaces the removed background with **pure white**.
5. It detects the remaining subject area.
6. It auto-crops and centers the result into a square ID-photo image.
7. It stores only the processed result in the employee record.
8. A processed preview is shown so the user can retake or choose another image if the edges do not look clean.

This uses deterministic local image processing and does not upload the photo to any cloud service.

## Offline Signature Cleanup
The employee editor also supports **Camera** and **Gallery / Upload** for the employee signature.

Recommended capture setup:

- Use **clean white or very light paper**.
- Write using **black or dark ink**.
- Use even lighting and avoid strong paper shadows.
- Keep other marks away from the signature area.

Processing happens fully on-device:

1. The app corrects common image orientation when needed.
2. It estimates the light paper/background from the image edges.
3. Light paper pixels are removed.
4. Dark signature strokes are kept.
5. The signature is automatically cropped around the detected ink.
6. The result is saved as a **transparent PNG**.
7. A processed preview is shown before the employee record is saved.

The transparent signature can then be placed cleanly over the ID design during PDF generation.

## Local Processed-File Cleanup
The app stores processed photos and signatures in its own private local folder.

To avoid unnecessary storage buildup:

- Replacing a processed employee photo removes the old app-generated processed photo after the replacement is ready.
- Replacing a processed employee signature removes the old app-generated processed signature after the replacement is ready.
- Clearing a processed photo or signature removes that app-generated processed file.
- Cancelling an edit removes newly generated files that were not saved.
- Deleting an employee also removes that employee's app-generated processed photo and signature when applicable.
- Original images selected from the user's Gallery/files are **not deleted** by this cleanup logic.

## QR Workflow
QR generation is intentionally not required inside the app. The preferred workflow is to upload the employee's existing WEBV3LITE QR image. The uploaded QR image is stored with the employee record and used in the generated ID PDF.

## ID Size
The actual employee ID remains CR80 portrait size:

- Width: **53.98 mm**
- Height: **85.60 mm**

The CR80 ID is centered inside the larger printable paper slot.

## A4 Paper Slot Layout
The PDF uses the physical layout measured from the actual Publisher template.

- PDF page: **A4 Portrait — 210 × 297 mm**
- Total paper slots: **4**
- Each paper slot: approximately **85.01 × 115.05 mm**
- Each slot has a visible cutting border / cut guide

### Slot Mapping
One A4 sheet is for a maximum of **2 people**, because each person uses one front slot and one back slot.

| Slot | Position | Content |
| --- | --- | --- |
| 1 | Top-left | Person 1 — Front |
| 2 | Top-right | Person 1 — Back |
| 3 | Bottom-left | Person 2 — Front |
| 4 | Bottom-right | Person 2 — Back |

So:

- **Slot 1 + Slot 2 = one complete front/back ID for Person 1**
- **Slot 3 + Slot 4 = one complete front/back ID for Person 2**

If only one person is selected, Slots 3 and 4 remain blank but the cutting layout remains fixed.

## Publisher-Matched Placement
Current slot positions on the A4 page:

- Left column start: **19.97 mm**
- Right column start: **105.02 mm**
- Top row start: **14.77 mm**
- Bottom row start: **168.62 mm**

These values are intentionally fixed so the generated PDF stays consistent with the paper template used for cutting.

## Cutting Guides
Each 85.01 × 115.05 mm paper slot has a visible rectangular guide line. This is included so the printed sheet can be cut more easily and consistently using scissors, a cutter or a paper trimmer.

## App Icon
The Android launcher icon now uses:

- Green background
- White capital **B**
- Standard and round launcher icon support

## Main Screens
- **Home** — quick access to employee records, generation and settings
- **Records** — manage employee information, offline-cleaned photo, transparent signature and QR data
- **Generate ID** — select Person 1 and optional Person 2 and generate the A4 PDF
- **Settings** — save front/back templates, logos, barangay heading and Punong Barangay/signatory details

## Stability & Maintenance Fixes — September 4, 2026
- Added orientation correction for photos and signatures whose camera metadata would otherwise make the processed image appear sideways or rotated.
- Added safe cleanup helpers for app-generated processed images.
- Added cleanup when processed employee photos/signatures are replaced, cleared, cancelled before saving, or removed with an employee record.
- Cleanup is restricted to the app's own processed-image directory; original Gallery/file uploads are not deleted.
- Standardized the shared-preference keys used by the settings helper so they match the active Settings and PDF-generation flow.
- Disabled Android application backup to better enforce the intended local-only data policy.
- Kept the QR workflow upload-based; no in-app QR generator was added.
- Kept all Publisher-matched A4 slot coordinates and cutting guides unchanged.
- GitHub Actions successfully compiled the updated app and passed the offline-only manifest verification after these fixes.

## Latest Adjustments — September 4, 2026
- Corrected the PDF layout from a tight CR80 2×2 block to the actual Publisher-based paper-slot layout
- Changed the print logic to use **4 fixed 85.01 × 115.05 mm paper slots**
- Kept the actual ID at **53.98 × 85.60 mm CR80 portrait size**
- Centered each CR80 ID inside its paper slot
- Added visible cutting lines around all four paper slots
- Confirmed the correct front/back pairing for two people per sheet
- Added green launcher icon with a white **B**
- Connected normal and round launcher icons in the Android manifest
- Locked the application to **offline-only** operation by explicitly removing `android.permission.INTERNET`
- Added a GitHub Actions build check that fails if INTERNET permission appears in the final merged Android manifests
- Added Camera and Gallery actions for ID photos and signatures
- Documented that an existing ID photo may be uploaded instead of taking a new camera photo
- Confirmed that camera photos and uploaded photos use the same offline white-background cleanup pipeline
- Added offline plain-background detection for ID photos
- Added automatic white-background replacement and square auto-crop for ID photos
- Added offline light-paper removal and transparent PNG generation for signatures
- Added processed previews and retake/replace workflow
- Added private local file storage and FileProvider support for processed images and temporary camera captures
- Kept QR handling simple: upload the existing WEBV3LITE QR image instead of requiring in-app QR generation

## Build and APK
GitHub Actions automatically builds a debug APK on every push to `main`.

To get the latest APK:

1. Open the repository on GitHub
2. Open **Actions**
3. Open **Build Android APK**
4. Select the latest successful run
5. Open **Artifacts**
6. Download **BarangayIDMaker-debug-apk**

## Current Direction
The app remains a fully offline Barangay ID Maker. The print layout is locked to the real paper template so future ID-design changes should not alter the physical A4 slot positions or cutting guides unless the paper template itself changes. Photo and signature cleanup must remain on-device and must not require cloud or network processing. Existing employee ID photos may be uploaded from the device, and QR images remain upload-based for a simple offline workflow.

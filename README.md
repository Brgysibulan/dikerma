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
- GitHub is only used to develop and build new APK versions; the installed app itself does not need GitHub or a network connection to operate.

## Current Features
- Single-user Android app
- Offline employee records using Room database
- Employee photo and signature per record
- Camera or Gallery input for employee photo and signature
- Fully offline plain-background cleanup for ID photos
- Fully offline paper/background removal for signatures
- Processed image preview before saving the employee record
- WEBV3LITE QR token / QR image stored per employee
- Stored Logo 1 and Logo 2
- Stored Punong Barangay name, position and signature
- Uploadable front and back ID design templates
- Permission-safe Android document/image pickers
- System camera capture without granting the app a permanent camera permission
- No broad storage, camera, contacts, location, microphone, notification, or internet permission
- A4 portrait PDF generation
- Supports 1 or 2 persons per A4 sheet
- Print instruction: use **Actual Size / 100%** and do not use **Fit to Page**

## Offline ID Photo Cleanup
The employee editor now supports **Camera** and **Gallery** for the ID photo.

Recommended capture setup:

- Put the person in front of a **plain solid-color background**.
- Use even lighting and avoid strong shadows on the background.
- Use a background color that contrasts with the person's hair and clothing.
- Keep the head and shoulders clearly separated from the background.

Processing happens fully on-device:

1. The app estimates the plain background color from the image edges.
2. It removes the edge-connected plain background.
3. It replaces the removed background with **pure white**.
4. It detects the remaining subject area.
5. It auto-crops and centers the result into a square ID-photo image.
6. It stores only the processed result in the employee record.
7. A processed preview is shown so the user can retake or choose another image if the edges do not look clean.

This uses deterministic local image processing and does not upload the photo to any cloud service.

## Offline Signature Cleanup
The employee editor also supports **Camera** and **Gallery** for the employee signature.

Recommended capture setup:

- Use **clean white or very light paper**.
- Write using **black or dark ink**.
- Use even lighting and avoid strong paper shadows.
- Keep other marks away from the signature area.

Processing happens fully on-device:

1. The app estimates the light paper/background from the image edges.
2. Light paper pixels are removed.
3. Dark signature strokes are kept.
4. The signature is automatically cropped around the detected ink.
5. The result is saved as a **transparent PNG**.
6. A processed preview is shown before the employee record is saved.

The transparent signature can then be placed cleanly over the ID design during PDF generation.

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
- Added offline plain-background detection for ID photos
- Added automatic white-background replacement and square auto-crop for ID photos
- Added offline light-paper removal and transparent PNG generation for signatures
- Added processed previews and retake/replace workflow
- Added private local file storage and FileProvider support for processed images and temporary camera captures

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
The app remains a fully offline Barangay ID Maker. The print layout is locked to the real paper template so future ID-design changes should not alter the physical A4 slot positions or cutting guides unless the paper template itself changes. Photo and signature cleanup must remain on-device and must not require cloud or network processing.

# Barangay ID Maker

## App Description — v0.3.0
A **fully offline Android Barangay Employee ID Maker** for Barangay Sibulan. The app manages employee records locally, accepts ID photos from Camera or Gallery/files, performs on-device plain-background cleanup to white, cleans employee signatures to transparent PNG, stores uploaded WEBV3LITE QR images, and generates print-ready A4 PDFs using the fixed Barangay Sibulan cutting layout.

The current ID format is **CR80 portrait (53.98 × 85.60 mm)**. The employee photo frame is **30 × 35 mm**, center-cropped to preserve face proportions. The front and back ID content now uses the refined formal Barangay Employee ID wording, while the physical A4 slot positions and cut guides remain locked to the Publisher-matched print template.

## Current Testing Note — September 4, 2026
Current version: **0.3.0 / versionCode 4**.

- Front ID now includes Republic, Province, Municipality, Barangay, Barangay Employee ID title, 30 × 35 mm employee photo, Name, Designation, ID No., Cardholder's Signature, uploaded QR image, Scan to Verify, and Verify ID Validity.
- Back ID now includes Date of Birth, Address, Sex, Civil Status, identification statement, Issued By, Approved By, Punong Barangay signature/name/title, Important Notice, and Barangay contact footer.
- QR remains **upload-based**; no in-app QR generator is required.
- ID photo can come from **Camera or Gallery / Upload** and is processed fully offline.
- Signature/document-like images are rejected from the ID Photo workflow. Existing records showing **Invalid — replace photo** must have their ID photo replaced in Records before PDF generation.
- The 4-slot A4 layout is unchanged: Slot 1 = Person 1 Front, Slot 2 = Person 1 Back, Slot 3 = Person 2 Front, Slot 4 = Person 2 Back.
- Paper slot size remains approximately **85.01 × 115.05 mm** with visible cut lines.
- Print using **Actual Size / 100%**. Do not use **Fit to Page**.
- During testing, check the actual printed photo position, name/designation spacing, QR size, signature position, back-ID notice spacing, and footer readability before treating the visual layout as final.

## Current Status
The app is already able to manage employee ID data offline and generate a print-ready A4 PDF. The latest build includes the corrected Publisher-matched paper-slot layout, fully offline photo/signature cleanup, invalid-photo protection, and the refined v0.3.0 front/back employee ID content.

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
- Invalid ID-photo protection for signature/document-like images
- WEBV3LITE QR image can be uploaded per employee and placed on the printed ID
- Stored Logo 1 and Logo 2
- Stored Punong Barangay name, position and signature
- Uploadable front and back ID design templates
- Permission-safe Android document/image pickers
- System camera capture without granting the app a permanent camera permission
- No broad storage, camera, contacts, location, microphone, notification, or internet permission
- A4 portrait PDF generation
- Supports 1 or 2 persons per A4 sheet
- Refined formal front/back Barangay Employee ID content
- Exact 30 × 35 mm employee photo frame inside the CR80 card
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
6. It auto-crops and centers the processed result.
7. The PDF renderer center-crops the saved photo into the exact **30 × 35 mm** ID-photo frame without stretching the face.
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

## Refined ID Content — v0.3.0
### Front ID
- Republic of the Philippines
- Province of Davao del Sur
- Municipality of Sta. Cruz
- Barangay Sibulan
- **BARANGAY EMPLOYEE ID**
- **30 × 35 mm** employee photo
- Name
- Designation
- ID No.
- Employee signature with **CARDHOLDER'S SIGNATURE** label
- **SCAN TO VERIFY**
- Uploaded QR image
- **VERIFY ID VALIDITY**

### Back ID
- Date of Birth
- Address
- Sex
- Civil Status
- Formal identification statement confirming the bearer as a bona fide employee of the Barangay Local Government Unit of Sibulan
- **ISSUED BY: BLGU - SIBULAN**
- **APPROVED BY** with Punong Barangay signature, name and title
- **IMPORTANT NOTICE** covering non-transferability, BLGU ownership, loss reporting, and unauthorized use
- Footer with Barangay Hall address, email and contact number

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
The Android launcher icon uses:

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
- Added invalid-photo checks so signature/document-like images cannot be used as an employee ID photo.
- Kept the QR workflow upload-based; no in-app QR generator was added.
- Kept all Publisher-matched A4 slot coordinates and cutting guides unchanged.
- GitHub Actions compiles the app and checks that the final merged manifest remains offline-only.

## Latest Adjustments — September 4, 2026
- Refined the complete front and back Barangay Employee ID wording and placement
- Added exact **30 × 35 mm** ID photo frame
- Changed photo rendering to center-crop instead of stretching
- Added formal Name, Designation, ID No., Cardholder's Signature and QR verification labels
- Added refined back-ID identification statement and Important Notice
- Added Barangay contact footer
- Added invalid-photo protection for existing and newly processed employee records
- Corrected the PDF layout to the actual Publisher-based paper-slot layout
- Kept the actual ID at **53.98 × 85.60 mm CR80 portrait size**
- Kept visible cutting lines around all four paper slots
- Confirmed the correct front/back pairing for two people per sheet
- Added green launcher icon with a white **B**
- Locked the application to **offline-only** operation by explicitly removing `android.permission.INTERNET`
- Added Camera and Gallery actions for ID photos and signatures
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
The app remains a fully offline Barangay ID Maker. The print layout is locked to the real paper template so future ID-design changes should not alter the physical A4 slot positions or cutting guides unless the paper template itself changes. Photo and signature cleanup must remain on-device and must not require cloud or network processing. Existing employee ID photos may be uploaded from the device, QR images remain upload-based, and the v0.3.0 visual layout should be validated through actual PDF/print testing before finalizing spacing and typography.

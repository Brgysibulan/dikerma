# Barangay ID Maker

## App Description — v0.3.0 + current input-mode update
A **fully offline Android Barangay Employee ID Maker** for Barangay Sibulan. The app manages employee records locally, accepts employee photos and signatures from Camera or Gallery/files, stores uploaded WEBV3LITE QR images, and generates print-ready A4 PDFs using the fixed Barangay Sibulan cutting layout.

The current ID format is **CR80 portrait (53.98 × 85.60 mm)**. The employee photo frame is **30 × 35 mm** and is center-cropped during PDF rendering so the face is not stretched. The physical A4 slot positions and cut guides remain locked to the Publisher-matched print template.

## Current Image Input Modes
The employee editor now gives a clear choice for **ID Photo** and **Employee Signature**:

### Auto Clean
Use this when the app should process the image locally.

- **ID Photo:** removes a plain background, replaces it with pure white, validates the portrait, and auto-crops the subject.
- **Signature:** removes light paper/background, keeps the dark signature strokes, auto-crops the signature, and saves a transparent PNG.
- Processing is fully offline and stays on the device.

### Keep Original
Use this when the image is already prepared and should not be altered.

- **ID Photo:** uses the selected Gallery/File image as uploaded, with no background removal.
- **Signature:** uses the selected Gallery/File image as uploaded, with no background removal.
- For signatures, a **transparent PNG is strongly recommended** for the cleanest PDF result.
- Keep Original currently uses **Gallery / Upload**. Camera capture continues to use **Auto Clean**.
- Original Gallery/Documents files are never deleted by the app's processed-file cleanup logic.

### Recommended workflow
- Already-clean white-background employee photo → **Keep Original → Gallery / Upload**.
- Photo taken against a plain colored background → **Auto Clean**.
- Ready transparent PNG signature → **Keep Original → Gallery / Upload**.
- Signature photographed on white/light paper → **Auto Clean**.

## Current Testing Note — September 4, 2026
Current Android version remains **0.3.0 / versionCode 4** while the new image-input options are being tested.

Check these items in the next test:
- Confirm Auto Clean still produces a clean white-background portrait.
- Confirm Keep Original leaves an uploaded employee photo unchanged.
- Confirm a transparent PNG signature uploaded with Keep Original appears without a black/white background box.
- Confirm Auto Clean signature still works for signatures photographed on light paper.
- Confirm existing employee records can be edited and saved without losing unrelated data.
- Confirm QR upload and PDF generation are unchanged.
- Confirm the 4-slot A4 layout and cut guides remain unchanged.

## Offline-Only Policy
This app is intentionally designed to work without internet access after installation.

- No `android.permission.INTERNET` permission is granted to the app.
- The Android manifest explicitly removes INTERNET permission if a future dependency attempts to add it.
- GitHub Actions checks the merged Android manifests and fails the build if INTERNET permission appears.
- Employee records, photos, signatures, QR images, templates, settings, image processing, and PDF generation stay on-device.
- Android application backup is disabled for stricter local-only handling.
- GitHub is used only for development/build distribution; the installed app itself does not need network access.

## Current Features
- Offline employee records using Room database
- Camera and Gallery/File image input
- **Auto Clean / Keep Original** choice for employee photo and signature
- Fully offline white-background cleanup for ID photos
- Fully offline paper/background removal for signatures
- Direct transparent PNG signature upload through Keep Original
- Image preview before saving
- EXIF orientation correction for processed images
- Safe cleanup of obsolete app-generated processed images
- Invalid ID-photo protection for sparse signature/document-like images
- Uploaded WEBV3LITE QR image per employee
- Uploadable front/back ID templates
- Saved logos and Punong Barangay/signatory details
- A4 portrait PDF generation for one or two people
- Fixed Publisher-matched cutting guides
- Green app branding and white **B** icon

## QR Workflow
QR generation is intentionally not required inside the app. Upload the employee's existing WEBV3LITE QR image and the PDF generator places that image on the ID.

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
- **CARDHOLDER'S SIGNATURE**
- **SCAN TO VERIFY**
- Uploaded QR image
- **VERIFY ID VALIDITY**

### Back ID
- Date of Birth
- Address
- Sex
- Civil Status
- Formal identification statement
- **ISSUED BY: BLGU - SIBULAN**
- **APPROVED BY** with Punong Barangay signature, name and title
- **IMPORTANT NOTICE**
- Barangay Hall address, email and contact number

## ID and Print Size
The employee ID remains CR80 portrait:

- Width: **53.98 mm**
- Height: **85.60 mm**
- Employee photo frame: **30 × 35 mm**

The CR80 card is centered inside the larger printable paper slot.

## A4 Paper Slot Layout
- PDF page: **A4 Portrait — 210 × 297 mm**
- Paper slot: approximately **85.01 × 115.05 mm**
- Total slots: **4**
- Visible cut guide around each slot

| Slot | Position | Content |
| --- | --- | --- |
| 1 | Top-left | Person 1 — Front |
| 2 | Top-right | Person 1 — Back |
| 3 | Bottom-left | Person 2 — Front |
| 4 | Bottom-right | Person 2 — Back |

If only one person is selected, Slots 3 and 4 remain blank while the cutting layout stays fixed.

### Publisher-matched positions
- Left column start: **19.97 mm**
- Right column start: **105.02 mm**
- Top row start: **14.77 mm**
- Bottom row start: **168.62 mm**

These coordinates must not be changed during visual refinements unless the actual paper template changes.

## Print Instruction
Always print using **Actual Size / 100%**. Do **not** use **Fit to Page**.

## Main Screens
- **Home** — app shortcuts and setup guidance
- **Records** — employee data, photo, signature, and QR image
- **Generate ID** — select Person 1 and optional Person 2, then generate A4 PDF
- **Settings** — front/back templates, logos, headings, and signatory details

## Build and APK
GitHub Actions automatically builds a debug APK on pushes to `main` and verifies that the merged Android manifest remains offline-only.

To get the latest successful APK:
1. Open the repository on GitHub.
2. Open **Actions**.
3. Open **Build Android APK**.
4. Select the latest successful run.
5. Download **BarangayIDMaker-debug-apk** from Artifacts.

## Current Direction
Keep the app fully offline, keep QR upload-based, preserve the exact 4-slot A4 cutting layout, and improve the actual ID appearance through real PDF/print testing. For photo/signature handling, the user should always have control over whether the app performs background cleanup or keeps the uploaded image unchanged.

See `CURRENT_NOTE.md` for the active testing checklist and `CHANGELOG.md` for release history.

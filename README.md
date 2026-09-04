# Barangay ID Maker

## App Description — v0.4.0
A **fully offline Android Barangay Employee ID Maker** for Barangay Sibulan. The app keeps employee records on-device, accepts employee photos and signatures from Camera or Gallery/files, supports optional offline background cleanup, stores uploaded WEBV3LITE QR images, and generates print-ready A4 PDFs for front/back employee IDs.

The actual ID is **CR80 portrait: 53.98 × 85.60 mm**. Version 0.4.0 resets the internal ID composition using millimeter-based coordinates so the layout is consistent with the physical card size instead of relying on arbitrary PDF point offsets.

## Current Version
- Version name: **0.4.0**
- Version code: **5**
- Android minSdk: 26
- Android targetSdk: 35

## Offline-Only Policy
The installed app is intentionally designed to operate without internet access.

- `android.permission.INTERNET` is not granted.
- The manifest explicitly removes INTERNET permission if a dependency tries to merge it in.
- GitHub Actions checks the merged manifest and fails the build if INTERNET permission appears.
- Room employee records, photos, signatures, QR images, templates, settings and PDF generation remain on-device.
- Android application backup is disabled for stricter local-only handling.
- GitHub is used only to develop/build APKs; the installed app itself does not require GitHub.

## Employee Photo and Signature Input
Both **ID Photo** and **Employee Signature** provide two modes.

### Auto Clean
Use when the app should process the selected image locally.

**ID Photo**
- Detects a plain background.
- Replaces the removed background with pure white.
- Validates that the image resembles an ID portrait rather than a sparse signature/document image.
- Auto-crops the detected subject.
- Processing remains fully offline.

**Employee Signature**
- Removes light paper/background.
- Keeps dark signature strokes.
- Auto-crops the signature.
- Saves the processed result as a transparent PNG.

### Keep Original
Use when the image is already prepared and should not be cleaned.

- Available through **Gallery / Upload**.
- The selected photo/signature is used without background removal.
- Recommended for an existing ID photo that already has the desired background.
- Strongly recommended for a ready **transparent PNG signature**.
- Original Gallery/Documents files are never deleted by app-generated cleanup.

Camera capture currently uses **Auto Clean**.

## Recommended Signature Workflow
For the cleanest signature in the generated ID:

1. Prepare a transparent PNG signature.
2. Open the employee record.
3. Under **Employee Signature**, choose **Keep Original**.
4. Tap **Gallery / Upload**.
5. Select the PNG.
6. Check the preview and save the employee.

The PDF renderer preserves transparent pixels and fits the signature proportionally inside its signature area.

## QR Workflow
QR generation is intentionally not required inside this app.

- Upload the employee's existing **WEBV3LITE QR image**.
- The QR image is stored with the employee record.
- The PDF generator places the uploaded QR on the front ID.
- QR remains upload-based and works with the fully offline app workflow.

## CR80 Card Size
The actual employee card is:

- Width: **53.98 mm**
- Height: **85.60 mm**
- Orientation: Portrait

Uploaded front/back background designs are rendered into the exact CR80 card area.

## v0.4.0 Front Layout
The front ID now uses millimeter-based placement.

- Government header with Logo 1 and Logo 2
- **BARANGAY EMPLOYEE ID** title
- Employee photo: **25 × 30 mm**
- Photo is center-cropped to the frame without stretching the face
- Name / Designation / ID No. placed in a dedicated right-side information block
- Employee signature fitted proportionally in the lower-left area
- **CARDHOLDER'S SIGNATURE** label
- Uploaded QR image: **14 × 14 mm** target area
- **SCAN TO VERIFY** and **VERIFY ID VALIDITY** labels

The smaller 25 × 30 mm photo replaces the previous 30 × 35 mm layout so employee information has enough horizontal space and the front composition is more balanced.

## v0.4.0 Back Layout
The back ID intentionally has **no repeated government header**.

It contains:
- Date of Birth
- Sex
- Civil Status
- Address with up to two wrapped lines
- **IDENTIFICATION** section
- Formal bona fide employee statement
- **ISSUED BY: BLGU - SIBULAN**
- **APPROVED BY**
- Punong Barangay signature, name and title
- **IMPORTANT NOTICE**
- Barangay Hall address, email and contact number

Long text uses controlled wrapped-text rendering instead of forcing everything into a single tiny line.

## A4 Placement
The A4 sheet remains portrait and supports up to two employees.

| Slot | Position | Content |
| --- | --- | --- |
| 1 | Top-left | Person 1 Front |
| 2 | Top-right | Person 1 Back |
| 3 | Bottom-left | Person 2 Front |
| 4 | Bottom-right | Person 2 Back |

The existing 85.01 × 115.05 mm placement zones are retained only as **A4 positioning anchors**.

### Important v0.4.0 Cut-Guide Change
The visible cutting guide no longer follows the oversized 85.01 × 115.05 mm placement zone.

The visible cutting border now follows the **actual CR80 card boundary: 53.98 × 85.60 mm**.

This makes the printed cut line match the physical ID size.

## A4 Anchor Coordinates
The placement anchors remain:

- Left column start: **19.97 mm**
- Right column start: **105.02 mm**
- Top row start: **14.77 mm**
- Bottom row start: **168.62 mm**

Each CR80 card is centered inside its corresponding placement zone.

## Printing
Always print the generated PDF using:

- **Actual Size / 100%**
- Do **not** use Fit to Page

After printing, verify the physical card width and height with a ruler before mass production.

## Main Screens
- **Home** — shortcuts and workflow guidance
- **Records** — employee CRUD, photo, signature and QR management
- **Generate ID** — select Person 1 and optional Person 2 and create the A4 PDF
- **Settings** — front/back designs, logos, headings and Punong Barangay/signatory assets

## Current Testing Priorities
Before finalizing the visual design, test the generated PDF and an actual 100% print for:

1. Exact 53.98 × 85.60 mm cut boundary
2. 25 × 30 mm employee photo size and face crop
3. Name / Designation / ID No. readability
4. Employee transparent PNG signature with no background box
5. 14 × 14 mm QR scanning reliability
6. Back address wrapping
7. Identification paragraph readability
8. Punong Barangay signature/name spacing
9. Important Notice readability
10. Footer readability

Visual spacing can still be refined after real print testing, but the physical CR80 size, slot pairing and offline-only architecture should remain stable.

## Build and APK
GitHub Actions automatically builds a debug APK on pushes to `main`.

To obtain the latest APK from GitHub:
1. Open **Actions**.
2. Open **Build Android APK**.
3. Select the latest successful run.
4. Download the **BarangayIDMaker-debug-apk** artifact.

## Current Direction
The project remains a fully offline Barangay Employee ID preparation and printing app. The current priority is real-size print validation and visual refinement inside the exact CR80 card boundary, without reintroducing oversized cutting guides or cloud-dependent processing.

# Barangay ID Maker

## App Description — v0.6.1
A **fully offline Android Barangay Employee ID Maker** for Barangay Sibulan. The app stores employee records on-device, accepts employee photos and signatures from Camera or Gallery/files, supports optional offline background cleanup, stores uploaded WEBV3LITE QR images, and generates print-ready A4 PDFs for front/back employee IDs.

The current physical ID size is **85 mm × 115 mm portrait** for both the front and back. The uploaded front/back artwork is treated as the actual visual background. The app should only place dynamic data on top of that artwork unless no background design has been uploaded.

## Current Version
- Version name: **0.6.1**
- Version code: **8**
- Android minSdk: 26
- Android targetSdk: 35

## Continuation Status — September 4, 2026
The first generated-PDF screenshot from v0.6.0 was reviewed and was **not approved as the final visual layout**. Version 0.6.1 is the current screenshot-driven correction.

Latest completed work:
- Fixed Auto Clean for signatures photographed on a plain light or dark background.
- Added PDF-time cleanup for older app-generated signatures showing a uniform black rectangle.
- Preserved Keep Original behavior without modifying the uploaded source.
- Moved the back Issued By / Approved By, Important Notice, and footer content upward.
- Increased footer readability and bottom safe margin.
- Confirmed that selecting only Person 1 leaves the entire bottom A4 row blank without cut boxes.
- GitHub Actions run **#73** passed APK compilation, offline-only verification, and artifact upload.

Next continuation step: install the v0.6.1 APK, generate a new PDF using the same employee/template data, and review new screenshots plus an Actual Size / 100% test print. Do not describe the layout as visually final until the user approves those results.

## Locked Physical ID Size
Each generated front/back ID is:

- Width: **85 mm / 8.5 cm**
- Height: **115 mm / 11.5 cm**
- Orientation: Portrait

This is the actual cutting size used by the current project. Do not replace it with CR80 dimensions unless the physical barangay ID specification is intentionally changed later.

## A4 Placement
One A4 portrait sheet supports up to two people:

| Position | Content |
| --- | --- |
| Top-left | Person 1 Front |
| Top-right | Person 1 Back |
| Bottom-left | Person 2 Front |
| Bottom-right | Person 2 Back |

Current placement anchors:
- Left column: **19.97 mm**
- Right column: **105.02 mm**
- Top row: **14.77 mm**
- Bottom row: **168.62 mm**

If Person 2 is not selected, the bottom row stays unused and the app does **not** draw empty cut boxes there.

## Background-First Design Rule
The uploaded front/back ID artwork is the design.

When a front/back background is uploaded:
- The app does **not** add extra decorative green header bands.
- The app does **not** add decorative footer bands.
- The app does **not** add a translucent body panel over the artwork.
- The app only renders dynamic ID content such as text, photo, signature, QR and optional overlay lines.

A simple fallback design is used only when no uploaded background exists.

This prevents the app design from fighting with the user-provided barangay artwork.

## Outline Controls
Settings now allows each overlay outline/line to be controlled independently.

Available controls:
- **Outer cut guide**
- **Photo outline**
- **Employee info divider lines**
- **Signature line**
- **QR outline**
- **Back section divider lines**
- **Outline thickness** from 0.30 pt to 1.50 pt

Default behavior is intentionally minimal:
- Outer cut guide: On
- Photo outline: Off
- Employee info dividers: Off
- Signature line: Off
- QR outline: Off
- Back section dividers: Off

This keeps the uploaded background clean while still allowing lines to be enabled when the artwork needs them.

## Typography Controls
Front and back now use a consistent typography system.

Settings provides one global font-family choice:
- **Sans Serif** — default
- **Serif**
- **Monospace**

The selected family is used across the full ID. Headings, labels and values differ only by size and weight rather than switching randomly between typefaces.

A global **Font Scale** control is also available from **85% to 120%** so the overall text size can be adjusted without changing every field individually.

Text is rendered directly over the uploaded artwork without glow, shadow, sticker, pill, or automatic gray/white backing effects. Long employee names and designations may wrap to two lines and scale only within a controlled readable range.

## Employee Photo and Signature Input
Both **ID Photo** and **Employee Signature** support two modes.

### Auto Clean
Use when the app should process the selected image locally.

**ID Photo**
- Detects a plain background.
- Replaces removed background with pure white.
- Validates that the image resembles an ID portrait rather than a sparse signature/document image.
- Auto-crops the detected subject.
- Fully offline.

**Employee Signature**
- Removes a plain light or dark background.
- Keeps dark signature strokes.
- Auto-crops the signature.
- Saves the processed result as a transparent PNG.

### Keep Original
Use when the selected file is already prepared.

- Available through **Gallery / Upload**.
- No background removal is applied.
- Recommended for existing prepared ID photos.
- Strongly recommended for a ready **transparent PNG signature**.
- Original Gallery/Documents files are not deleted by app-generated cleanup.

Camera capture currently uses **Auto Clean**.

## Recommended Signature Workflow
For the cleanest result:

1. Prepare a transparent PNG signature.
2. Open the employee record.
3. Under **Employee Signature**, choose **Keep Original**.
4. Tap **Gallery / Upload**.
5. Select the PNG.
6. Check the preview and save the employee.
7. Generate the PDF and verify that no black/white box appears behind the signature.

The PDF renderer fits signature images proportionally.

App-generated signatures from older builds are also checked while rendering so a uniform opaque black rectangle can be removed without changing Keep Original uploads.

## QR Workflow
QR generation is intentionally not required inside this app.

- Upload the employee's existing **WEBV3LITE QR image**.
- The QR image is stored with the employee record.
- The PDF generator places the uploaded QR on the front ID.
- QR remains upload-based and the installed app remains fully offline.

## Front-ID Rendering Direction
The front should remain visually simple because the uploaded artwork already carries the barangay design.

Dynamic content currently includes:
- Barangay/header text
- Optional logos
- Employee photo
- Name
- Designation
- Employee/Control No.
- Employee signature
- QR image
- Verification label
- Punong Barangay/signatory information where applicable

Internal outlines are optional and controlled from Settings.

## Back-ID Rendering Direction
The back intentionally has no repeated decorative government header added by the app.

Dynamic content includes:
- Date of Birth
- Sex
- Civil Status
- Address
- Identification statement
- Issued By
- Approved By
- Punong Barangay signature, name and title
- Important Notice
- Contact footer

Back section lines are optional and controlled from Settings.

Date of Birth is displayed in English full-month form, for example **January 12, 1987**, even when an existing record stores a supported machine-friendly value such as `1987-01-12` or `01/12/1987`.

The contact footer stays inside the card safe area and includes the Barangay Hall address, `brgysibulan8001@gmail.com`, and `0970 972 3363`.

## Printing
Always print the generated PDF using:
- **Actual Size / 100%**
- Do **not** use Fit to Page

Before mass production, measure one test print with a ruler and confirm that each front/back ID is approximately **85 mm × 115 mm**.

## Offline-Only Policy
The installed app is intentionally designed to operate without internet access.

- `android.permission.INTERNET` is not granted.
- The manifest explicitly removes INTERNET permission if a dependency tries to merge it in.
- GitHub Actions checks the merged manifest and fails the build if INTERNET permission appears.
- Room employee records, photos, signatures, QR images, templates, settings and PDF generation remain on-device.
- Android application backup is disabled for stricter local-only handling.
- GitHub is used only for development/building; the installed app itself does not require GitHub.

## Main Screens
- **Home** — shortcuts and workflow guidance
- **Records** — employee CRUD, photo, signature and QR management
- **Generate ID** — select Person 1 and optional Person 2 and create the A4 PDF
- **Settings** — front/back designs, logos, headings, signatory assets, outline controls and typography controls

## Current Testing Priorities
Before finalizing the visual layout, test:

1. Physical ID size is **85 × 115 mm** at Actual Size / 100%.
2. Uploaded front/back artwork remains visible without extra decorative panels from the app.
3. Photo placement looks balanced against the background.
4. Name / Designation / Employee No. are aligned and readable.
5. Transparent PNG signatures render without a background box.
6. QR remains readable/scannable.
7. Outline toggles correctly enable/disable each line type.
8. Outline thickness changes are reflected in the PDF.
9. Font family stays consistent across front and back.
10. Font Scale changes all text proportionally without breaking alignment.
11. Back text and Important Notice remain readable.
12. When Person 2 is empty, no blank bottom-row cut boxes appear.
13. Date of Birth appears like **January 12, 1987**, never as a raw numeric or ISO date.
14. QR and `SCAN TO VERIFY` stay in their lower-right block and never overlap Employee No.

## Build and APK
GitHub Actions automatically builds a debug APK on pushes to `main` and verifies that the APK remains offline-only.

To obtain the latest APK from GitHub:
1. Open **Actions**.
2. Open **Build Android APK**.
3. Select the latest successful run.
4. Download the **BarangayIDMaker-debug-apk** artifact.

## Current Direction
The project is now focused on **background-first rendering**: the uploaded barangay artwork defines the visual design, while the app only overlays data, optional outlines, consistent typography and fully offline PDF output at the locked **85 × 115 mm** physical size. The visual positions remain subject to user approval of generated-PDF screenshots and an Actual Size / 100% test print.

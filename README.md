# Barangay ID Maker

## App Description — v0.7.1
A **fully offline Android Barangay Employee ID Maker** for Barangay Sibulan. The app stores employee records on-device, accepts employee photos and signatures from Camera or Gallery/files, supports optional offline background cleanup, stores uploaded WEBV3LITE QR images, and generates print-ready A4 PDFs for front/back employee IDs.

The current physical ID size is **85 mm × 115 mm portrait** for both the front and back. The uploaded front/back artwork is treated as the actual visual background. The app should only place dynamic data on top of that artwork unless no background design has been uploaded.

## Current Version
- Version name: **0.7.1**
- Version code: **10**
- Android minSdk: 26
- Android targetSdk: 35

## Continuation Status — September 4, 2026
The generated-PDF screenshots were reviewed and were **not approved as the final visual layout**. Version 0.7.1 keeps the saved mobile Layout Studio and adds per-text underlining plus clearer white-background photo cleanup controls.

Latest completed work:
- Added Front/Back **Layout Studio** with a live 85 × 115 mm template preview.
- Added touch drag placement plus sliders for exact horizontal/vertical position and element size.
- Added per-text size, alignment, color, visibility, underline on/off, outline on/off, and outline-thickness controls.
- Added overlap warnings for critical Front and Back sections.
- Added layout locking, per-side professional reset, and one **Save placement • Apply to all IDs** action.
- Saved coordinates use millimetres inside the physical card and are reused for every employee, including both people on the A4 sheet.
- Removed the duplicate Punong Barangay signatory block from the bottom of the Front ID.
- Preserved the v0.6.1 signature cleanup, full-month DOB formatting, background-first rendering, and independent outline controls.

GitHub Actions run **#77** passed the v0.7.1 debug APK compile, offline-only verification, and `BarangayIDMaker-debug-apk` artifact upload. After installation, arrange and lock one layout, then generate a new PDF using the same employee/template data. Do not describe the layout as visually final until the user approves the screenshots and Actual Size / 100% test print.

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

## Saved Layout Studio
Settings includes **Layout Studio** for matching overlays to any uploaded Front/Back artwork without changing employee records.

User-friendly workflow:
1. Upload the blank Front and Back background artwork.
2. Open **Settings → Arrange Front & Back**.
3. Choose the Front or Back tab.
4. Tap and drag an overlay, or use the position and size sliders for fine adjustment.
5. Adjust text size, alignment, black/white/green color, visibility, underline, and optional text outline for the selected text item.
6. Review automatic warnings for critical collisions such as QR versus Employee No. or Important Notice versus footer.
7. Turn on **Lock placement after saving** when the composition is ready.
8. Tap **Save placement • Apply to all IDs**.

The saved layout is global and uniform. It is not stored separately in each employee record, so one approved placement can be reused for 200 or more employee IDs without manual re-layout.

Technical behavior:
- All overlay positions and sizes are stored in millimetres inside the fixed **85 × 115 mm** card.
- The editor preview and PDF generator read the same saved layout model.
- Phone resolution and preview zoom do not change the physical PDF coordinates.
- A professional default is available separately for Front and Back.
- QR rendering remains proportional and its editor size stays square.
- Text outline is controlled per text item and is **off by default**.
- Underline is controlled per text item and is **off by default**.
- Existing photo, information-divider, signature-line, QR, back-divider, and cut-guide controls remain independent.

## Employee Photo and Signature Input
Both **ID Photo** and **Employee Signature** support two modes.

### Auto Clean
Use when the app should process the selected image locally.

**ID Photo**
- Detects a plain background.
- Uses **White BG (Default)** and replaces the removed background with pure white.
- Validates that the image resembles an ID portrait rather than a sparse signature/document image.
- Auto-crops the detected subject.
- Fully offline.

**Employee Signature**
- Uses **Transparent BG** cleanup and removes a plain light or dark background.
- Keeps dark signature strokes.
- Auto-crops the signature.
- Saves the processed result as a transparent PNG.

The photo/signature screen describes the result before upload and displays the selected image on a white preview surface. White replacement applies to ID photos; signatures remain transparent so the app does not create a visible rectangle over the ID artwork.

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

The Front does not repeat a second Punong Barangay name/title at the bottom. Approval information remains on the Back.

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
- **Settings** — front/back designs, Layout Studio, logos, headings, signatory assets, outline controls and typography controls

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
15. Save a Front/Back placement, reopen Layout Studio, and confirm the layout persists.
16. Generate IDs for two different employees and confirm both use the same saved placement.
17. Confirm editor selection boxes/guides never appear in the PDF.
18. Confirm optional per-text outlines are absent when disabled and use the saved thickness when enabled.

## Build and APK
GitHub Actions automatically builds a debug APK on pushes to `main` and verifies that the APK remains offline-only.

To obtain the latest APK from GitHub:
1. Open **Actions**.
2. Open **Build Android APK**.
3. Select the latest successful run.
4. Download the **BarangayIDMaker-debug-apk** artifact.

## Current Direction
The project is now focused on **background-first rendering with one reusable saved layout**: the uploaded barangay artwork defines the visual design, while the app only overlays data, optional outlines, consistent typography and fully offline PDF output at the locked **85 × 115 mm** physical size. The saved composition can be reused uniformly for a 200-ID batch. Visual positions remain subject to user approval of generated-PDF screenshots and an Actual Size / 100% test print.

# Current Note — Barangay ID Maker v0.7.1

Date: September 5, 2026

## Stable Status
**v0.7.1 / versionCode 10 is the CURRENT STABLE BASELINE of DIKERMAAPP / Barangay ID Maker.**

Use v0.7.1 as the known-good reference point for future development, experiments, UI changes, and layout refinements.

Important distinction:
- **Stable build:** v0.7.1
- **Visual layout:** still adjustable and subject to user approval from generated-PDF screenshots and an Actual Size / 100% print test
- Do not treat future experimental layout changes as the new stable baseline unless they are explicitly approved and documented.

Latest successful validation for the current code line:
- Repository: `Brgysibulan/dikerma`
- Branch: `main`
- Android version: **0.7.1 / versionCode 10**
- GitHub Actions run **#81** completed the debug APK build, offline-only verification, and APK artifact upload successfully.

## Locked Physical Size
- A4 Portrait: 210 × 297 mm
- Each front/back ID: **85 × 115 mm**
- Width: **8.5 cm**
- Height: **11.5 cm**
- Top-left: Person 1 Front
- Top-right: Person 1 Back
- Bottom-left: Person 2 Front
- Bottom-right: Person 2 Back
- Left placement anchor: 19.97 mm
- Right placement anchor: 105.02 mm
- Top-row anchor: 14.77 mm
- Bottom-row anchor: 168.62 mm

If Person 2 is not selected, the bottom row stays unused and no empty bottom-row cut boxes are drawn.

## Main Design Rule
> **Uploaded front/back artwork is the design. The app only places dynamic data and optional overlays on top of it.**

When an uploaded background exists:
- No extra decorative green header is added.
- No extra decorative footer band is added.
- No translucent body panel is added.
- The complete uploaded artwork is mapped to the complete 85 × 115 mm card.
- Dynamic content only is rendered over the background.

A simple fallback design may be used only when the corresponding uploaded background is missing.

## Current Layout Studio
Settings contains a mobile-friendly **Layout Studio** used to arrange one reusable Front/Back layout for all employee IDs.

Current behavior:
- Live 85 × 115 mm preview using the uploaded background.
- Tap and drag any overlay element.
- X/Y position, width, and height are stored in millimetres.
- Saved placement applies uniformly to all current and future employee IDs.
- Layout can be locked after saving.
- Front and Back can be reset independently.
- Editor-only guides and selection borders never print in the PDF.
- Overlap warnings help detect obvious collisions before saving.

### Precision and Alignment Controls
The current Layout Studio includes:
- Safe-margin guide
- Horizontal center guide
- Vertical center guide
- Grid
- Snap-to-grid
- Precision nudge controls
- Quick horizontal and vertical alignment controls
- Reset Position / Size / Style for the selected element

This reduces the need to estimate placement manually.

## Independent Front Header Elements
The Front header is not treated as one fixed text block.

These are independently positionable:
- Logo 1
- Barangay Sibulan
- Sta. Cruz / Municipality
- Davao del Sur / Province
- Barangay Employee ID
- Logo 2

**Sta. Cruz and Davao del Sur are separate layout elements.** Davao del Sur can be moved lower or adjusted independently without moving Sta. Cruz.

## Per-Text Typography and Effects
Each text element can have its own saved style.

Available controls include:
- Font family
  - Sans Serif
  - Serif
  - Monospace
- Text size
- Bold on/off
- Left / Center / Right alignment
- Visibility
- Text color presets
- Custom HEX text color

### Underline
Per text element:
- Underline on/off
- Underline color
- Underline thickness
- Underline offset/distance
- Text-width or full-element-width behavior

Underline is off by default.

### Text Outline
Per text element:
- Outline on/off
- Outline color
- Outline thickness

Outline color is user-controlled and is not automatically forced to black or white.

Text outline is off by default.

### Text Shadow
Per text element:
- Shadow on/off
- Shadow color
- Shadow opacity
- Horizontal offset
- Vertical offset
- Shadow size/strength

Shadow is off by default.

The app should not create automatic gray/white/black pills, sticker backgrounds, or glow boxes behind normal text.

## Existing Outline Controls
The separate line/box outline settings remain available for:
- Outer cut guide
- Photo outline
- Employee info divider lines
- Signature line
- QR outline
- Back section divider lines
- Outline thickness

Default behavior remains intentionally minimal so the uploaded background stays clean.

## PDF Rendering Rule
The Layout Studio preview and generated PDF use the same saved layout/style model.

Saved settings used by the PDF include:
- Position
- Size
- Font family
- Font size
- Font weight
- Alignment
- Text color
- Underline
- Text outline
- Text shadow
- Visibility

The goal is that a layout that looks correct in Layout Studio should render in the same position and style in the PDF.

## Date Display
Birthdates displayed in the PDF use full English month format.

Required example:
**January 12, 1987**

Supported stored/input formats are normalized before PDF output. Empty or unrecognized dates render as a dash instead of exposing an inconsistent raw date.

## Photo and Signature Modes
Both ID Photo and Employee Signature support:

### Auto Clean
- ID Photo: **White BG (Default)** cleanup, pure-white replacement, validation, and auto-crop
- Signature: **Transparent BG** cleanup and transparent PNG output
- Fully offline

### Keep Original
- Gallery / Upload file is used without background cleanup
- Recommended for prepared ID photos
- Strongly recommended for transparent PNG signatures
- Camera capture currently uses Auto Clean

## QR Behavior
- QR remains manual/upload-based from WEBV3LITE.
- No automatic QR generation is required in the app.
- QR is stored with the employee record and rendered on the Front ID.

## A4 Batch Behavior
- Maximum 2 people per A4 sheet.
- Person 1 Front/Back stay paired on the top row.
- Person 2 Front/Back stay paired on the bottom row.
- One saved Layout Studio configuration is reused for the full batch, including a planned 200-ID production run.

## Offline Policy
The app remains fully offline.
- `android.permission.INTERNET` remains removed.
- GitHub Actions verifies the merged manifest before publishing the debug APK artifact.
- Room employee records, photos, signatures, QR files, settings, layout data, and PDF generation stay on-device.

## Stable-Baseline Rule Going Forward
Until explicitly changed:

**CURRENT STABLE = v0.7.1 / versionCode 10**

For future work:
1. Start from the v0.7.1 stable baseline.
2. Treat new layout/UI work as experimental until tested.
3. Do not change the locked 85 × 115 mm size unless explicitly requested.
4. Do not replace or redesign the uploaded background artwork automatically.
5. Do not call a new visual layout final until the user approves the generated PDF and print test.
6. Only mark a later version as the new stable baseline after explicit approval and documentation.

## Printing Rule
Print at **Actual Size / 100%**. Never use **Fit to Page** for physical-size validation.

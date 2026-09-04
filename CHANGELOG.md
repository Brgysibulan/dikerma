# Changelog

## Unreleased — September 4, 2026

Current visual-control update for the 85 × 115 mm ID layout.

### Background-first rendering
- Uploaded front/back artwork is now treated as the actual visual design.
- When uploaded artwork exists, the app no longer adds extra decorative green header/footer/body panels over it.
- The renderer focuses on dynamic content only: text, photo, signature, QR and optional overlay lines.
- A basic fallback design remains available when an uploaded background is missing.

### Editable outline controls
Added independent Settings controls for:
- Outer cut guide
- Photo outline
- Employee info divider lines
- Signature line
- QR outline
- Back section divider lines
- Outline thickness from 0.30 pt to 1.50 pt

Default outline behavior is intentionally minimal:
- Outer cut guide enabled
- Internal photo/info/signature/QR/back-divider outlines disabled

### Uniform typography
- Added a global ID font-family setting.
- Supported families: Sans Serif, Serif and Monospace.
- Sans Serif is the default.
- The selected family is applied consistently across front and back.
- Headings, labels and values vary by size/weight rather than randomly changing typefaces.
- Added a global Font Scale control from 85% to 120%.

### A4 cleanup
- If Person 2 is not selected, the bottom row remains unused without empty bottom-row cut boxes.
- Person 1 front/back pairing remains top-left / top-right.
- Person 2 front/back pairing remains bottom-left / bottom-right when selected.

### Offline behavior
- INTERNET permission remains removed.
- GitHub Actions continues to verify offline-only merged manifests before uploading the APK artifact.

## 0.5.0 — September 4, 2026

Corrected the project to use the intended **85 × 115 mm portrait ID** as the actual physical front/back size.

### Physical-size correction
- Each front/back ID is now exactly **85 mm × 115 mm**.
- The previous 53.98 × 85.60 mm CR80 interpretation was removed from the active generator.
- The full 85 × 115 mm rectangle is the ID and cutting area.
- Existing A4 placement anchors remain:
  - Left column: 19.97 mm
  - Right column: 105.02 mm
  - Top row: 14.77 mm
  - Bottom row: 168.62 mm

### Layout direction
- Reworked the front/back renderer for the larger 85 × 115 mm format.
- Kept Person 1 front/back at the top row and optional Person 2 front/back at the bottom row.
- Continued proportional rendering for photos, logos, signatures and QR images.

### Version
- Android version bumped to **0.5.0 / versionCode 6**.

## 0.4.0 — September 4, 2026

Intermediate CR80 layout experiment.

- Introduced millimeter-based placement helpers.
- Added a 25 × 30 mm photo layout experiment.
- Added wrapped back-ID text and proportional signature/QR rendering.
- This CR80 physical-size assumption was later superseded by v0.5.0 after confirming the intended ID size is 85 × 115 mm.

## 0.3.0 — September 4, 2026
- Added refined formal front/back ID wording.
- Added initial 30 × 35 mm employee photo layout.
- Added employee signature and QR verification labels.
- Added formal back identification statement, Important Notice and Barangay contact footer.

## 0.2.1 — September 4, 2026
- Added protection against signature/document-like images being saved as employee ID photos.
- Added existing-record photo validation before PDF generation.
- Added additional bitmap recycling to reduce memory pressure.

## 0.2.0 — September 4, 2026
- Added green Material 3 app theme and in-app B branding.
- Removed unused legacy PDF generators and retained TightPortraitGenerateScreen as the active generator.
- Added duplicate Person 1 / Person 2 protection and Generate-screen Ready Check.
- Added bounded bitmap loading and temporary bitmap recycling.
- Added orientation correction and safe app-generated image cleanup.
- Aligned settings preference keys and disabled Android backup for the local-only data policy.

## 0.1.0
Initial fully offline Barangay ID Maker with Room employee records, Settings assets, camera/gallery input, offline photo/signature processing, uploaded QR images and A4 PDF generation.

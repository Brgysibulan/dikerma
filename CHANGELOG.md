# Changelog

## 0.7.0 — September 4, 2026

Saved mobile layout workflow for repeatable batch production.

### Layout Studio
- Added a Front/Back Layout Studio under Settings with the uploaded template as the live background.
- Added touch drag placement and fine position/size sliders for every overlay element.
- Stored layout coordinates and dimensions in millimetres inside the physical 85 × 115 mm card.
- Added one **Save placement • Apply to all IDs** action so the same approved composition is reused for a 200-ID batch.
- Added layout locking to prevent accidental movement after approval.
- Added separate professional-default reset actions for Front and Back.
- Added editor-only safe guides and selected-item borders that are never rendered in the PDF.
- Added critical overlap warnings for the Front photo/information/QR/signature blocks and Back content/footer blocks.

### Per-element controls
- Added per-text size, alignment, black/white/dark-green color, visibility, text-outline on/off, and text-outline thickness.
- Text outlines remain off by default and do not force outlines on any other element.
- Preserved the existing independent cut-guide, photo, information-divider, signature-line, QR, and back-divider controls.
- Kept the global Sans Serif/Serif/Monospace family and Font Scale controls.

### PDF renderer
- The active PDF renderer now reads the exact same saved placement model used by Layout Studio.
- Preserved full uploaded-template mapping to the complete 85 × 115 mm rectangle.
- Preserved proportional logo, signature, and upload-only QR rendering.
- Kept long-name/designation fitting and full-month English DOB formatting.
- Removed the duplicate Punong Barangay name/title from the bottom of the Front; approval information remains on the Back.
- Kept A4 Person 1/Person 2 pairing and blank-bottom-row behavior unchanged.

### Version and validation
- Android version bumped to **0.7.0 / versionCode 9**.
- GitHub Actions compile, offline-only verification, and APK artifact validation are required before release handoff.

## 0.6.1 — September 4, 2026

Screenshot-driven correction after the first v0.6.0 PDF test.

- Fixed Auto Clean signatures photographed on a plain dark background.
- Added PDF-time repair for older app-generated signature PNGs with a uniform opaque rectangle.
- Keep Original signature uploads are still rendered unchanged.
- Moved the back Issued/Approved, Important Notice, and footer groups upward.
- Increased footer readability and bottom safe margin.
- Android version bumped to **0.6.1 / versionCode 8**.

## 0.6.0 — September 4, 2026

Layout-safety and typography pass for the locked 85 × 115 mm background-first generator.

### Front composition
- Rebalanced the two-logo header and centered its heading hierarchy.
- Gave Name, Designation, and Employee No. a wider dedicated column.
- Added controlled two-line fitting for long names and designations.
- Separated the lower-left employee signature and lower-right QR blocks.
- Moved `SCAN TO VERIFY` into the QR block with safe space above the QR.
- Reduced the visual weight of the bottom Punong Barangay name/title.
- Preserved proportional QR/signature/logo rendering and transparent bitmap alpha.

### Back composition
- Aligned Date of Birth, Sex, Civil Status, and Address with consistent spacing.
- Added English full-month DOB output such as **January 12, 1987**.
- Reduced the Identification heading and improved paragraph wrapping.
- Balanced the Issued By and Approved By blocks.
- Moved Important Notice higher and replaced large bullet glyphs with small dash marks.
- Restored the complete approved notice wording.
- Added the Barangay Hall address above the email/phone footer inside safe margins.

### Template and outline behavior
- The full uploaded front/back source is mapped to the full 85 × 115 mm card without an inset card or decorative overlay.
- Outer cut guides are drawn after the full-bleed content so an enabled guide remains visible.
- All internal outlines remain independently controlled and off by default.

### Validation status
- Automated build/offline validation is required before release handoff.
- Visual positioning remains pending user approval of generated-PDF screenshots and an Actual Size / 100% test print.

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

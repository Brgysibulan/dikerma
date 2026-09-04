# Changelog

## 0.4.0 — September 4, 2026

Professional CR80 layout reset for real-size PDF and print validation.

### CR80 cut-guide correction
- Actual card remains **53.98 × 85.60 mm** portrait.
- The existing 85.01 × 115.05 mm zones remain as A4 placement anchors only.
- Visible cutting guides now follow the exact **53.98 × 85.60 mm CR80 boundary** instead of the oversized placement zones.
- Slot pairing remains unchanged:
  - Slot 1 = Person 1 Front
  - Slot 2 = Person 1 Back
  - Slot 3 = Person 2 Front
  - Slot 4 = Person 2 Back

### Front layout reset
- Internal positions now use millimeter-based coordinates relative to the CR80 card.
- Employee photo changed from 30 × 35 mm to **25 × 30 mm** for better balance and more room for employee information.
- Photo remains center-cropped without stretching.
- Name / Designation / ID No. now use a dedicated right-side information block.
- Employee signature is proportionally fitted in the lower-left area.
- QR target area increased to **14 × 14 mm** and remains upload-based.
- SCAN TO VERIFY and VERIFY ID VALIDITY remain on the front.

### Back layout reset
- Back has no repeated government header.
- Address supports controlled wrapping.
- Identification statement uses wrapped text rather than forcing a single tiny line.
- Issued By / Approved By / Punong Barangay signature-name-title spacing was reorganized.
- Important Notice was shortened and spaced for physical-card readability.
- Footer contact information was condensed.

### Image rendering
- Signatures, logos and QR images use proportional fit rendering instead of forced stretching.
- Transparent PNG signature pixels remain transparent when rendered.

### Photo and signature input modes
- Both ID Photo and Employee Signature support **Auto Clean** and **Keep Original**.
- Auto Clean photo: offline plain-background removal, white replacement, validation and auto-crop.
- Auto Clean signature: offline light-paper removal and transparent PNG output.
- Keep Original uses the selected Gallery/File image without background cleanup.
- Transparent PNG is recommended for Keep Original signatures.
- Camera capture currently uses Auto Clean.

### Privacy / offline behavior
- QR remains upload-based from WEBV3LITE.
- `android.permission.INTERNET` remains removed.
- GitHub Actions continues to verify that the merged manifest is offline-only.

### Version
- Android version bumped to **0.4.0 / versionCode 5**.

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

# Current Note — Barangay ID Maker v0.4.0

Date: September 4, 2026

## Current Build Purpose
Version 0.4.0 is the CR80 layout-reset build for real PDF/print testing. The internal front/back composition now uses millimeter-based positions inside the exact 53.98 × 85.60 mm card.

## Locked Physical Size
- A4 Portrait: 210 × 297 mm
- CR80 portrait card: **53.98 × 85.60 mm**
- Slot 1: Person 1 Front
- Slot 2: Person 1 Back
- Slot 3: Person 2 Front
- Slot 4: Person 2 Back
- Left placement anchor: 19.97 mm
- Right placement anchor: 105.02 mm
- Top-row anchor: 14.77 mm
- Bottom-row anchor: 168.62 mm

The old 85.01 × 115.05 mm zones remain only as placement anchors. **The visible cut guide now follows the actual CR80 53.98 × 85.60 mm card boundary.**

## Front ID — v0.4.0
- Government header + Logo 1 / Logo 2
- BARANGAY EMPLOYEE ID
- Employee photo: **25 × 30 mm**
- Photo uses center-crop without stretching
- Name / Designation / ID No. in a dedicated right-side block
- Employee signature lower-left
- CARDHOLDER'S SIGNATURE label
- Uploaded QR image: **14 × 14 mm target area**
- SCAN TO VERIFY
- VERIFY ID VALIDITY

## Back ID — v0.4.0
- No repeated government header
- Date of Birth
- Sex
- Civil Status
- Address with wrapped text
- IDENTIFICATION section
- Bona fide employee statement
- ISSUED BY: BLGU - SIBULAN
- APPROVED BY
- Punong Barangay signature, name and title
- IMPORTANT NOTICE
- Barangay Hall contact footer

## Photo & Signature Modes
Both ID Photo and Employee Signature support:

### Auto Clean
- Photo: plain-background removal, white replacement, validation and auto-crop
- Signature: light-paper removal and transparent PNG output
- Fully offline

### Keep Original
- Gallery / Upload image is used without background cleanup
- Recommended for already-prepared ID photos
- Strongly recommended for ready transparent PNG signatures
- Camera capture currently uses Auto Clean

## Recommended Signature Test
1. Prepare a transparent PNG signature.
2. Records → employee → Employee Signature.
3. Choose **Keep Original**.
4. Gallery / Upload the PNG.
5. Save employee.
6. Generate PDF.
7. Confirm no black/white rectangle appears behind the signature.

## What to Check in the Next PDF / Actual Print
1. Measure cut boundary: **53.98 × 85.60 mm**
2. Confirm photo is **25 × 30 mm** and face is not distorted
3. Check header readability at physical size
4. Check Name / Designation / ID No. alignment
5. Check transparent employee signature and label
6. Confirm QR scans reliably at the new 14 × 14 mm target size
7. Check back address wrapping
8. Check Identification paragraph readability
9. Check Punong Barangay signature/name/title spacing
10. Check Important Notice readability
11. Check footer readability
12. Confirm Person 1 front/back and optional Person 2 front/back remain paired correctly

## Printing Rule
Print at **Actual Size / 100%**. Never use **Fit to Page** for size validation.

## Offline Policy
The app remains fully offline. QR remains upload-based from WEBV3LITE. INTERNET permission remains removed and is checked by GitHub Actions.

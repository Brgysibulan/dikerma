# Current Note — Barangay ID Maker v0.3.0

Date: September 4, 2026

## Current Build Purpose
This build is for testing the refined Barangay Employee ID front/back layout plus the new user-controlled image handling options, while keeping the physical A4 cutting layout locked.

## New Photo & Signature Input Modes
Both **ID Photo** and **Employee Signature** now have two modes:

### Auto Clean
- ID Photo: plain-background removal, white-background replacement, portrait validation, and auto-crop.
- Signature: light-paper/background removal, dark signature extraction, auto-crop, and transparent PNG output.
- Processing is fully offline.

### Keep Original
- Uses the selected Gallery/File image without background removal or cleanup.
- Recommended when the employee photo is already prepared.
- Strongly recommended for a ready **transparent PNG signature**.
- Keep Original currently uses **Gallery / Upload**.
- Camera capture continues to use **Auto Clean**.

## Recommended Signature Workflow
For the cleanest result in the generated ID:
1. Prepare a transparent PNG signature.
2. Open the employee record.
3. Under **Employee Signature**, select **Keep Original**.
4. Tap **Gallery / Upload**.
5. Select the transparent PNG.
6. Check the preview and save the employee.

This avoids the unwanted background/black-box result that can happen when an already-prepared signature is processed again.

## Front ID
- Republic of the Philippines
- Province of Davao del Sur
- Municipality of Sta. Cruz
- Barangay Sibulan
- BARANGAY EMPLOYEE ID
- 30 × 35 mm employee photo
- Name
- Designation
- ID No.
- Cardholder's Signature
- Scan to Verify
- Uploaded QR image
- Verify ID Validity

The employee photo is center-cropped into the 30 × 35 mm frame so the face is not stretched.

## Back ID
- Date of Birth
- Address
- Sex
- Civil Status
- Formal identification statement
- Issued By: BLGU - Sibulan
- Approved By
- Punong Barangay signature, name and title
- Important Notice
- Barangay Hall address, email and contact number

## Important Testing Notes
- Test both **Auto Clean** and **Keep Original** on an ID photo.
- Test a transparent PNG signature using **Keep Original** and confirm there is no visible background box in the PDF.
- Test a photographed signature using **Auto Clean**.
- If a selected employee shows **Invalid — replace photo**, open Records and replace the employee portrait before generating.
- QR remains upload-based from WEBV3LITE.
- The app remains fully offline and does not require INTERNET permission.
- Print generated PDFs using **Actual Size / 100%** and never **Fit to Page**.

## Locked A4 Layout
Do not change these values during visual/content refinements unless the actual paper template changes:

- A4 Portrait: 210 × 297 mm
- Paper slot: approximately 85.01 × 115.05 mm
- CR80 portrait card: 53.98 × 85.60 mm
- Slot 1: Person 1 Front
- Slot 2: Person 1 Back
- Slot 3: Person 2 Front
- Slot 4: Person 2 Back
- Left column start: 19.97 mm
- Right column start: 105.02 mm
- Top row start: 14.77 mm
- Bottom row start: 168.62 mm
- Visible cut guides remain enabled

## What to Check in the Next Test PDF
1. 30 × 35 mm photo size and face crop
2. Header readability
3. Name / Designation / ID No. alignment
4. Employee signature transparency and size
5. No black/white box behind a Keep Original transparent PNG signature
6. QR size and verification labels
7. Back-ID paragraph readability
8. Punong Barangay signature/name spacing
9. Important Notice spacing
10. Footer readability
11. Actual printed CR80 dimensions after cutting

Visual spacing may still be adjusted after real PDF/print inspection, but the A4 slot coordinates and cut guides must remain unchanged.

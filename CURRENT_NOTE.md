# Current Note — Barangay ID Maker v0.3.0

Date: September 4, 2026

## Current Build Purpose
This build is for testing the refined Barangay Employee ID front/back layout while keeping the physical A4 cutting layout locked.

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

The employee photo is center-cropped to the 30 × 35 mm frame so the face is not stretched.

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
- If a selected employee shows **Invalid — replace photo**, open Records and upload/retake the correct employee portrait before generating.
- QR remains upload-based from WEBV3LITE.
- Camera and Gallery ID photos use the same fully offline white-background cleanup.
- Employee signatures are processed locally to transparent PNG.
- The app remains fully offline and does not require INTERNET permission.
- Print generated PDFs using **Actual Size / 100%** and never **Fit to Page**.

## Locked A4 Layout
Do not change these values during visual ID-content refinements unless the actual paper template changes:

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
4. Employee signature size and label
5. QR size and verification labels
6. Back-ID paragraph readability
7. Punong Barangay signature/name spacing
8. Important Notice spacing
9. Footer readability
10. Actual printed CR80 dimensions after cutting

Visual spacing may still be adjusted after real PDF/print inspection, but the A4 slot coordinates and cut guides must remain unchanged.

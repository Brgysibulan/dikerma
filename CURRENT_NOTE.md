# Current Note — Barangay ID Maker v0.6.1

Date: September 4, 2026

## Latest Continuation Handoff
- Repository: `Brgysibulan/dikerma`
- Branch: `main`
- Current implementation commit: `cdf9c55d2e7f9d4754153003a440b5d56d436320`
- Current build: **0.6.1 / versionCode 8**
- GitHub Actions: **Run #73 succeeded**
- APK artifact: `BarangayIDMaker-debug-apk`

The user tested the earlier v0.6.0 PDF and supplied a screenshot. That screenshot is a test reference only and was **not approved as final**.

Observed from that screenshot:
- Person 1 Front and Back correctly occupied the top-left/top-right slots.
- The optional Person 2 bottom row correctly remained blank with no empty cut boxes.
- The employee signature displayed as an unacceptable black rectangle.
- The back Important Notice and footer were too dense and too close to the lower artwork.

Corrections implemented in v0.6.1:
- Auto Clean signature processing now removes a uniform light or dark background instead of assuming white paper.
- Older app-generated signature images with a uniform opaque rectangle are repaired during PDF rendering.
- Keep Original uploads remain unmodified as required.
- Back Issued By / Approved By, Important Notice, address, email, and phone were moved upward for safer spacing.
- Footer text size and safe bottom margin were increased.

Required next test:
1. Install the APK from GitHub Actions Run #73.
2. Generate the same one-person PDF again.
3. Confirm the black signature rectangle is gone.
4. Confirm the Important Notice and footer are readable and do not collide with the dark/busy background.
5. Send the new front/back screenshot for another layout review.
6. Print at Actual Size / 100% and measure 85 × 115 mm before approval.

## Current Build Purpose
The current build is for real PDF/print testing of the corrected **85 × 115 mm portrait ID** layout with user-controlled outlines and consistent typography.

The most important design rule now is:

> **Uploaded front/back artwork is the design. The app should only place dynamic data on top of it.**

The app should not add extra decorative header/footer/body panels over an uploaded background.

The complete uploaded artwork is mapped to the complete 85 × 115 mm rectangle. The app does not place a smaller card inside that area or crop decorative edges from the supplied template.

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

## Background-First Rendering
When a front/back background is uploaded:
- No extra app-generated decorative green header is added.
- No app-generated decorative footer band is added.
- No translucent body panel is added over the artwork.
- Dynamic data only is rendered over the background.

A basic fallback design may be used only if the corresponding uploaded background is missing.

## Outline Controls
Settings now provides independent controls for:
- Outer cut guide
- Photo outline
- Employee info divider lines
- Signature line
- QR outline
- Back section divider lines
- Outline thickness: 0.30–1.50 pt

Current defaults:
- Outer cut guide: **On**
- Photo outline: **Off**
- Employee info divider lines: **Off**
- Signature line: **Off**
- QR outline: **Off**
- Back section divider lines: **Off**

The goal is to avoid over-outlining an ID when the uploaded artwork already contains its own visual structure.

## Typography Controls
The ID now uses one consistent font family across front and back.

Available choices:
- Sans Serif — default
- Serif
- Monospace

A global Font Scale slider is available from **85% to 120%**.

Headings, labels and values may use different sizes/weights, but the font family stays uniform throughout the ID.

Most PDF text is plain text over the artwork. There are no automatic glow, shadow, gray/white backing, pill, or sticker effects. Long names and designations use controlled two-line fitting.

## Photo & Signature Modes
Both ID Photo and Employee Signature support:

### Auto Clean
- Photo: plain-background removal, white replacement, validation and auto-crop
- Signature: plain light/dark-background removal and transparent PNG output
- Fully offline

### Keep Original
- Gallery / Upload image is used without background cleanup
- Recommended for already-prepared ID photos
- Strongly recommended for ready transparent PNG signatures
- Camera capture currently uses Auto Clean

## QR Behavior
- QR remains upload-based from WEBV3LITE.
- No automatic QR generator is required in the app.
- The uploaded QR is stored with the employee record and rendered on the front ID.
- `SCAN TO VERIFY` belongs to the isolated lower-right QR block and does not share the Employee No. area.

## Date Display
- Stored birthdates are normalized for PDF output.
- Required display style: **January 12, 1987** (`MMMM d, yyyy`, English month names).
- Supported stored/input forms include ISO `1987-01-12`, numeric `01/12/1987`, and already formatted English dates.
- Unrecognized or empty dates render as a dash rather than exposing an inconsistent raw date.

## Back Content
- The Identification statement uses the approved full wording with controlled wrapping.
- Issued By and Approved By occupy balanced left/right blocks.
- Important Notice uses small dash marks and the complete approved wording.
- The footer includes the Barangay Hall address, email, and telephone number inside safe margins.
- The Important Notice and footer were moved upward after review of the first v0.6.0 generated-PDF screenshot.
- Older app-generated signature files with an opaque uniform black rectangle are cleaned during PDF rendering; Keep Original files remain untouched.

## What to Check in the Next PDF / Actual Print
1. Measure each front/back ID and confirm **85 × 115 mm** at Actual Size / 100%.
2. Confirm uploaded background artwork is not covered by extra app decorative panels.
3. Check overall front alignment and visual balance.
4. Check photo placement against the background artwork.
5. Check Name / Designation / Employee No. alignment and readability.
6. Test a transparent PNG employee signature using Keep Original.
7. Confirm no black/white rectangle appears behind the signature.
8. Confirm QR scans reliably.
9. Turn Photo outline on/off and verify the PDF changes.
10. Turn Employee info dividers on/off and verify the PDF changes.
11. Turn Signature line and QR outline on/off independently.
12. Turn Back section dividers on/off independently.
13. Adjust outline thickness and verify it is applied consistently.
14. Test Sans Serif / Serif / Monospace and confirm one family is used throughout the ID.
15. Test Font Scale without breaking alignment.
16. Confirm back text, Identification section and Important Notice remain readable.
17. With no Person 2 selected, confirm the bottom row has no empty cut boxes.
18. Confirm DOB is shown like **January 12, 1987**.
19. Confirm the address/email/phone footer stays inside the card.

## Printing Rule
Print at **Actual Size / 100%**. Never use **Fit to Page** for physical-size validation.

## Offline Policy
The app remains fully offline. INTERNET permission is removed and GitHub Actions verifies the merged manifest before publishing the debug APK artifact.

## Current Priority
The next priority is visual fine-tuning based on real generated-PDF screenshots and actual print measurements, while keeping these locked:
- 85 × 115 mm physical ID size
- A4 front/back pairing
- uploaded-background-first design rule
- fully offline architecture

Do not call the visual layout final until the generated PDF screenshots and Actual Size / 100% test print are reviewed and approved by the user.

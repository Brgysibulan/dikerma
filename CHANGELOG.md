# Changelog

## 0.2.0 — September 4, 2026

Improvement release for the fully offline Barangay ID Maker.

### App workflow and UI
- Added a consistent green Material 3 app theme using the Barangay green brand color.
- Added an in-app white **B** badge on a green background on the Home screen.
- Improved Home guidance for the actual offline workflow:
  - Camera or Gallery/file upload for employee ID photos.
  - Offline white-background cleanup.
  - Upload-based WEBV3LITE QR image workflow.
  - Up to two people per A4 sheet.
- Added a Setup Status card in Settings so the user can quickly see whether front/back ID designs, logos and signatory signature are present.
- Added clearer confirmation messages when Settings assets are uploaded or replaced.

### Active PDF generator
- `TightPortraitGenerateScreen` remains the only active PDF generator.
- Removed the unused legacy `GenerateScreen.kt` landscape/test generator.
- Removed the unused legacy `PortraitGenerateScreen.kt` generator.
- Added duplicate-person protection so Person 1 and Person 2 cannot be the same employee.
- Added a Ready Check before PDF generation showing front/back design status and selected employees' photo/QR readiness.
- Large templates, logos, photos, signatures and QR images are now decoded with a bounded maximum size before PDF drawing to reduce memory pressure on Android devices.
- Temporary PDF drawing bitmaps are recycled after use.

### Locked print layout — unchanged
The improvement pass intentionally does **not** change the physical paper layout.

- PDF page: A4 Portrait, 210 × 297 mm.
- Paper slot size: approximately 85.01 × 115.05 mm.
- CR80 portrait card: 53.98 × 85.60 mm.
- Slot 1 / top-left: Person 1 Front.
- Slot 2 / top-right: Person 1 Back.
- Slot 3 / bottom-left: Person 2 Front.
- Slot 4 / bottom-right: Person 2 Back.
- Existing Publisher-matched slot coordinates and visible cut lines remain unchanged.
- Print using **Actual Size / 100%**; do not use **Fit to Page**.

### Offline image handling
- Existing ID photos can be uploaded from Gallery/files; taking a new photo is optional.
- Camera and uploaded photos use the same fully offline plain-background cleanup pipeline.
- Photo EXIF orientation is normalized before processing when needed.
- Replaced, cleared, cancelled and deleted processed images are cleaned up from the app's private processed-image storage.
- Cleanup is restricted to files created by the app; original Gallery/Documents files are not deleted.
- Employee signatures continue to be cleaned from white/light paper to transparent PNG locally on-device.

### QR behavior
- QR generation is intentionally not required inside the app.
- The employee's existing WEBV3LITE QR image is uploaded and stored with the employee record.
- The PDF generator uses the uploaded QR image.

### Privacy and offline-only safeguards
- `android.permission.INTERNET` remains explicitly removed from the Android manifest.
- GitHub Actions continues to fail the build if INTERNET permission appears in merged manifests.
- Android app backup is disabled for stricter local-only handling of app data.
- Employee records, photos, processed images, signatures, QR images, templates, settings and PDF generation stay on-device.

### Maintenance fixes
- Aligned legacy `AppSettings` preference keys with the active Settings keys.
- Improved processed-image lifecycle management.
- Bumped Android app version from `0.1.0` / versionCode 1 to `0.2.0` / versionCode 2.

## 0.1.0
Initial offline Barangay ID Maker implementation with Room employee records, Settings assets, camera/gallery input, offline photo/signature cleanup, uploaded QR images and A4 PDF generation.

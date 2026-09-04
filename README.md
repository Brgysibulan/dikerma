# Barangay ID Maker

Offline Android ID maker for Barangay Sibulan.

## Locked build direction
- Single-user Android app
- Offline employee records (Room)
- Fixed CR80 ID size: 85.60 × 53.98 mm
- Fixed front/back layout
- Stored Logo 1 and Logo 2
- Stored Punong Barangay name and signature
- Employee photo and signature per record
- WEBV3LITE QR token stored per employee
- A4 portrait PDF output for 1 or 2 persons
- Permission-safe Android system pickers; no broad storage permission

## Build
GitHub Actions builds a debug APK on every push to `main`.
Open **Actions → Build Android APK → latest successful run → Artifacts** to download the APK.

Current phase: project scaffold, local database model, navigation, and permission-safe foundation.

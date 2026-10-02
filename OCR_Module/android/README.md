# Android OCR Module Reference

This directory contains the exact Kotlin implementation used inside the Android application:
- `Frontend/app/src/main/java/com/smartvendor/ai/ocr/OcrScannerManager.kt`

## Integration in Android
The Android pipeline uses CameraX frame streaming:
1. `ScanScreen.kt` passes an `ImageProxy` or `Bitmap` to `ScanViewModel.kt`.
2. When **OCR Mode** is active (`isOcrActive = true`), `processFrame()` sends the frame directly to `OcrScannerManager.processBitmap(...)`.
3. `OcrScannerManager` runs ML Kit's Latin Text Recognizer (`TextRecognition.getClient()`).
4. If valid product text is identified:
   - Matches against the vendor's active local SQLite store inventory (`findRankedInventoryMatches`).
   - If not found in the local store, falls back to the 117K master database catalog (`findRankedCatalogMatches`).
   - Returns an `OcrResult` with detected product, price, packaging color, and match score.

Any improvements made in `OCR_Module/python/ocr_engine.py` can be directly mapped to `OcrScannerManager.kt` in this folder.

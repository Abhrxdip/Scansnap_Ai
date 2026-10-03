#!/usr/bin/env python3
"""
ScanSnap AI — Generate Barcode Images for Android App Scanning
Generates real barcode PNGs for every product in MASTER_CATALOG.
Display these on screen and scan with the Android app's Barcode/OCR mode.
"""

import sys
import os

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

import barcode
from barcode.writer import ImageWriter
from ocr_engine import MASTER_CATALOG

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "generated_barcodes")


def generate_all_barcodes():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    print("=" * 75)
    print("  ScanSnap AI — Barcode Image Generator")
    print("=" * 75)
    print(f"  Products: {len(MASTER_CATALOG)}")
    print(f"  Output:   {OUTPUT_DIR}")
    print("-" * 75)

    generated = 0
    failed = 0

    for product in MASTER_CATALOG:
        pid = product["id"]
        name = product["name"]
        code = product["barcode"].strip()

        try:
            if len(code) == 13:
                bc_class = barcode.get_barcode_class("ean13")
                bc = bc_class(code[:12], writer=ImageWriter())
            elif len(code) == 8:
                bc_class = barcode.get_barcode_class("ean8")
                bc = bc_class(code[:7], writer=ImageWriter())
            else:
                bc_class = barcode.get_barcode_class("code128")
                bc = bc_class(code, writer=ImageWriter())

            filepath = os.path.join(OUTPUT_DIR, pid)
            saved = bc.save(filepath)
            generated += 1
            print(f"  ✅ {pid:<25} | {code} | {name}")

        except Exception as e:
            failed += 1
            print(f"  ❌ {pid:<25} | {code} | ERROR: {e}")

    print("=" * 75)
    print(f"  Generated: {generated}/{len(MASTER_CATALOG)}")
    print(f"  Failed:    {failed}")
    print(f"  Location:  {OUTPUT_DIR}")
    print("=" * 75)
    print()
    print("  HOW TO TEST:")
    print("  1. Open the generated PNG files on your laptop/monitor")
    print("  2. Open your ScanSnap Android app")
    print("  3. Tap '🏷️ Barcode' mode")
    print("  4. Point camera at the barcode on screen → should auto-detect!")
    print()


if __name__ == "__main__":
    generate_all_barcodes()

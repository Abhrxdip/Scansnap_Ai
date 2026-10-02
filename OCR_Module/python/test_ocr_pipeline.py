#!/usr/bin/env python3
"""
ScanSnap AI — OCR Automated Test Harness & Benchmark Runner
Runs all benchmark test cases in test_labels.json against OcrPipeline.
Evaluates Product Matching Accuracy, Price Extraction Accuracy, and Unit Parsing.
"""

import sys
import json
import time
from pathlib import Path

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

from ocr_engine import OcrPipeline

def run_benchmarks():
    current_dir = Path(__file__).resolve().parent
    test_file = current_dir / "test_labels.json"

    if not test_file.exists():
        print(f"Error: {test_file} not found!")
        return

    with open(test_file, "r", encoding="utf-8") as f:
        tests = json.load(f)

    pipeline = OcrPipeline()

    passed_matches = 0
    passed_prices = 0
    passed_units = 0
    total = len(tests)

    print("=" * 80)
    print(f"  ScanSnap AI — OCR Pipeline Benchmark Suite ({total} Test Cases)")
    print("=" * 80)
    print(f"{'ID':<7} | {'Expected SKU':<18} | {'Resolved SKU':<18} | {'Conf':<6} | {'Price':<8} | {'Status'}")
    print("-" * 80)

    start_time = time.time()

    for t in tests:
        tid = t["test_id"]
        raw = t["raw_ocr"]
        exp_id = t["expected_id"]
        exp_price = t["expected_price"]
        exp_unit = t["expected_unit"]

        res = pipeline.match_product(raw)
        matched_id = res.get("product_id") if res["status"] == "MATCHED" else None
        extracted_price = res.get("extracted_price")
        extracted_unit = res.get("extracted_unit")
        conf = res.get("confidence", 0.0)

        # Evaluate match
        match_ok = (matched_id == exp_id)
        if match_ok:
            passed_matches += 1

        # Evaluate price
        if exp_price is None:
            price_ok = (extracted_price is None)
        else:
            price_ok = (extracted_price == exp_price)
        if price_ok:
            passed_prices += 1

        # Evaluate unit
        if exp_unit is None:
            unit_ok = (extracted_unit is None)
        else:
            unit_ok = (extracted_unit == exp_unit)
        if unit_ok:
            passed_units += 1

        status_str = "✅ PASS" if (match_ok and price_ok) else "❌ FAIL"
        disp_exp = exp_id if exp_id else "[UNRESOLVED]"
        disp_res = matched_id if matched_id else "[UNRESOLVED]"
        disp_price = f"₹{extracted_price}" if extracted_price else "--"

        print(f"{tid:<7} | {disp_exp:<18} | {disp_res:<18} | {conf*100:>5.1f}% | {disp_price:<8} | {status_str}")

    elapsed = (time.time() - start_time) * 1000

    print("=" * 80)
    print("  Benchmark Summary Results:")
    print(f"  • SKU Matching Accuracy:   {passed_matches}/{total} ({passed_matches/total*100:.1f}%)")
    print(f"  • Price (MRP) Accuracy:    {passed_prices}/{total} ({passed_prices/total*100:.1f}%)")
    print(f"  • Unit/Weight Accuracy:    {passed_units}/{total} ({passed_units/total*100:.1f}%)")
    print(f"  • Total Evaluation Time:   {elapsed:.2f} ms ({elapsed/total:.2f} ms/test)")
    print("=" * 80)

if __name__ == "__main__":
    run_benchmarks()

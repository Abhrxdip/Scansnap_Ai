import sys
import json
import time
from pathlib import Path

# Fix for windows console encoding
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

from ocr_engine import OcrPipeline

def run_baseline():
    current_dir = Path(__file__).resolve().parent
    test_file = current_dir / "test_labels.json"
    
    with open(test_file, "r", encoding="utf-8") as f:
        tests = json.load(f)
        
    pipeline = OcrPipeline()
    latencies = []
    
    passed_matches = 0
    passed_prices = 0
    passed_units = 0
    false_positives = 0
    false_negatives = 0
    total = len(tests)
    
    for t in tests:
        raw = t["raw_ocr"]
        exp_id = t["expected_id"]
        exp_price = t["expected_price"]
        exp_unit = t["expected_unit"]
        
        start_time = time.perf_counter()
        res = pipeline.match_product(raw)
        end_time = time.perf_counter()
        
        latency = (end_time - start_time) * 1000
        latencies.append(latency)
        
        matched_id = res.get("product_id") if res["status"] == "MATCHED" else None
        extracted_price = res.get("extracted_price")
        extracted_unit = res.get("extracted_unit")
        
        # Matches
        match_ok = (matched_id == exp_id)
        if match_ok:
            passed_matches += 1
        
        if exp_id is None and matched_id is not None:
            false_positives += 1
        elif exp_id is not None and matched_id is None:
            false_negatives += 1
            
        # Price
        if exp_price is None:
            price_ok = (extracted_price is None)
        else:
            price_ok = (extracted_price == exp_price)
        if price_ok:
            passed_prices += 1
            
        # Unit
        if exp_unit is None:
            unit_ok = (extracted_unit is None)
        else:
            unit_ok = (extracted_unit == exp_unit)
        if unit_ok:
            passed_units += 1
            
    latencies.sort()
    avg_lat = sum(latencies) / len(latencies) if latencies else 0
    p50_lat = latencies[int(len(latencies) * 0.50)] if latencies else 0
    p95_lat = latencies[int(len(latencies) * 0.95)] if latencies else 0
    p99_lat = latencies[int(len(latencies) * 0.99)] if latencies else 0
    
    report = [
        "## Baseline Benchmark Results",
        f"- **Existing test count:** {total}",
        f"- **Pass count (SKU Match):** {passed_matches}",
        f"- **Failure count (SKU Match):** {total - passed_matches}",
        f"- **SKU accuracy:** {passed_matches/total*100:.2f}%",
        f"- **MRP accuracy:** {passed_prices/total*100:.2f}%",
        f"- **Unit accuracy:** {passed_units/total*100:.2f}%",
        f"- **False positives:** {false_positives}",
        f"- **False negatives:** {false_negatives}",
        f"- **Average processing time:** {avg_lat:.2f} ms",
        f"- **p50 latency:** {p50_lat:.2f} ms",
        f"- **p95 latency:** {p95_lat:.2f} ms",
        f"- **p99 latency:** {p99_lat:.2f} ms"
    ]
    
    report_text = "\n".join(report)
    out_file = current_dir.parent / "baseline_results.md"
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(report_text)
        
    print(f"Baseline saved to {out_file}")
    print(report_text)

if __name__ == "__main__":
    run_baseline()

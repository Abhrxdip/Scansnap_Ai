import json
import time
import random
from pathlib import Path
from ocr_engine import OcrPipeline

def run_tests_and_stress():
    current_dir = Path(__file__).resolve().parent
    test_file = current_dir / "test_labels.json"
    
    with open(test_file, "r", encoding="utf-8") as f:
        tests = json.load(f)

    # 1. Expand with adversarial & negative tests
    new_tests = [
        {"test_id": "TC-17", "raw_ocr": "M A G G 1 2-Minute Noodles Net Wt 70g MRP Rs 14.00", "expected_id": "maggi", "expected_price": 14.0, "expected_unit": "70G", "notes": "Adversarial: I->1, spaces in MAGGI"},
        {"test_id": "TC-18", "raw_ocr": "Th um s Up Charged 250ml", "expected_id": "thums_up", "expected_price": None, "expected_unit": "250ML", "notes": "Curved text disconnected snippets"},
        {"test_id": "TC-19", "raw_ocr": "Amul Ice Cream \n MRP \n ₹ 30.00", "expected_id": "amul_ice_cream", "expected_price": 30.0, "expected_unit": None, "notes": "Multi-line MRP"},
        {"test_id": "TC-20", "raw_ocr": "M R P \n 30.00", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Multi-line MRP spaces"},
        {"test_id": "TC-21", "raw_ocr": "Cadbury ORE0 Biscuits 120g\nMRP:\n₹\n35", "expected_id": "oreo", "expected_price": 35.0, "expected_unit": "120G", "notes": "Multi-line MRP completely split"},
        {"test_id": "TC-22", "raw_ocr": "1234567890 0987654321", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Negative: Barcode-like numbers"},
        {"test_id": "TC-23", "raw_ocr": "MAGGI 2-Minute Noodles\nMRP 14.00\nPack 2 MRP 25.00", "expected_id": "maggi", "expected_price": 14.0, "expected_unit": None, "notes": "Multiple prices, should pick first or correctly handle"},
        {"test_id": "TC-24", "raw_ocr": "Plum Gr33n T3a Pore Clean5ing 100ml", "expected_id": "plum", "expected_price": None, "expected_unit": "100ML", "notes": "Adversarial numbers in letters"},
        {"test_id": "TC-25", "raw_ocr": "Britania Cake Choco Chill MRP 30.00", "expected_id": "cake", "expected_price": 30.0, "expected_unit": None, "notes": "Spelling error"}
    ]
    
    # We won't save them back to file yet, just run them in memory
    all_tests = tests + new_tests
    
    pipeline = OcrPipeline()
    passed = 0
    failed = 0
    critical_bugs = []
    major_bugs = []
    
    latencies = []
    
    print("--- RUNNING STANDARD & ADVERSARIAL TESTS ---")
    for t in all_tests:
        raw = t["raw_ocr"]
        exp_id = t["expected_id"]
        exp_price = t["expected_price"]
        
        start_time = time.perf_counter()
        res = pipeline.match_product(raw)
        lat = (time.perf_counter() - start_time) * 1000
        latencies.append(lat)
        
        matched_id = res.get("product_id") if res["status"] == "MATCHED" else None
        ext_price = res.get("extracted_price")
        
        match_ok = (matched_id == exp_id)
        price_ok = (ext_price == exp_price) if exp_price is not None else (ext_price is None)
        
        if match_ok and price_ok:
            passed += 1
        else:
            failed += 1
            is_baseline = int(t["test_id"].split("-")[1]) <= 16
            bug = f"{t['test_id']}: Expected {exp_id}/{exp_price}, got {matched_id}/{ext_price} ('{t['notes']}')"
            if is_baseline:
                critical_bugs.append(bug)
            else:
                major_bugs.append(bug)
                
    # 2. Run Stress tests (1000 synthetic variations of the base tests)
    print("\n--- RUNNING STRESS TESTS (1000 iterations) ---")
    stress_passed = 0
    stress_failed = 0
    stress_crashes = 0
    
    for _ in range(1000):
        base = random.choice(tests)
        # Randomize spaces, case, insert random characters to simulate noisy OCR
        chars = list(base["raw_ocr"])
        if random.random() > 0.5:
            # insert noise
            idx = random.randint(0, len(chars)-1)
            chars.insert(idx, random.choice(['#', '@', ' ', '1', '0', '~']))
        
        noisy_text = "".join(chars)
        
        try:
            start_time = time.perf_counter()
            res = pipeline.match_product(noisy_text)
            lat = (time.perf_counter() - start_time) * 1000
            latencies.append(lat)
            
            matched_id = res.get("product_id") if res["status"] == "MATCHED" else None
            # Evaluate only sku match since price extraction might reasonably fail on noisy text, but we want to see crash rate and basic robustness
            if matched_id == base["expected_id"]:
                stress_passed += 1
            else:
                stress_failed += 1
                
        except Exception as e:
            stress_crashes += 1
            critical_bugs.append(f"Stress test CRASH on input '{noisy_text}': {str(e)}")

    latencies.sort()
    avg_lat = sum(latencies) / len(latencies) if latencies else 0
    p50_lat = latencies[int(len(latencies) * 0.50)] if latencies else 0
    p95_lat = latencies[int(len(latencies) * 0.95)] if latencies else 0
    
    # Save results to a report file
    with open("test_findings.json", "w", encoding="utf-8") as f:
        json.dump({
            "standard_passed": passed,
            "standard_failed": failed,
            "stress_passed": stress_passed,
            "stress_failed": stress_failed,
            "stress_crashes": stress_crashes,
            "critical_bugs": critical_bugs,
            "major_bugs": major_bugs,
            "avg_latency": avg_lat,
            "p50_latency": p50_lat,
            "p95_latency": p95_lat
        }, f)
        
    print("Test run complete. Findings saved.")

if __name__ == "__main__":
    run_tests_and_stress()

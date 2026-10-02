import json
import time
import random
from collections import Counter
from pathlib import Path

from ocr_engine import OcrPipeline

def apply_ocr_corruption(text, rng):
    chars = list(text)
    if not chars:
        return text
        
    num_ops = rng.randint(1, 3)
    for _ in range(num_ops):
        op = rng.choice(['delete', 'insert', 'substitute', 'space', 'merge'])
        idx = rng.randint(0, len(chars) - 1)
        if op == 'delete' and len(chars) > 1:
            chars.pop(idx)
        elif op == 'insert':
            chars.insert(idx, rng.choice(['1', '0', '!', '@', ' ', 'O', 'I']))
        elif op == 'substitute':
            if chars[idx] == 'O': chars[idx] = '0'
            elif chars[idx] == '0': chars[idx] = 'O'
            elif chars[idx] == 'I': chars[idx] = '1'
            elif chars[idx] == 'S': chars[idx] = '5'
            elif chars[idx] == 'B': chars[idx] = '8'
            else: chars[idx] = rng.choice(['A', 'X', '1', '-'])
        elif op == 'space':
            chars.insert(idx, ' ')
        elif op == 'merge' and chars[idx] == ' ':
            chars.pop(idx)
    return "".join(chars)

def run():
    current_dir = Path(__file__).resolve().parent
    test_file = current_dir / "test_labels.json"
    
    with open(test_file, "r", encoding="utf-8") as f:
        static_tests = json.load(f)

    pipeline = OcrPipeline()
    
    # We will collect metrics
    total_tests = 0
    passed = 0
    failed = 0
    skipped = 0
    
    sku_correct = 0
    mrp_correct = 0
    unit_correct = 0
    
    false_positives = 0
    false_negatives = 0
    unresolved_count = 0
    ambiguous_count = 0
    
    latencies = []
    
    bugs = []
    failure_patterns = []
    
    stress_cases = 0
    stress_crashes = 0
    stress_exceptions = 0
    
    rng = random.Random(42) # fixed seed
    
    # Generate 500 stress tests based on the clean ones
    stress_tests = []
    clean_bases = [t for t in static_tests if int(t["test_id"].split("-")[1]) <= 105]
    for i in range(500):
        base = rng.choice(clean_bases)
        noisy_text = apply_ocr_corruption(base["raw_ocr"], rng)
        stress_tests.append({
            "test_id": f"STRESS-{i}",
            "raw_ocr": noisy_text,
            "expected_id": base["expected_id"],
            "expected_price": base["expected_price"],
            "expected_unit": base["expected_unit"],
            "notes": "Stress test generated"
        })
        
    all_tests = static_tests + stress_tests
    total_tests = len(all_tests)
    
    for t in all_tests:
        raw = t["raw_ocr"]
        exp_id = t["expected_id"]
        exp_price = t["expected_price"]
        exp_unit = t["expected_unit"]
        is_stress = t["test_id"].startswith("STRESS")
        
        start_time = time.perf_counter()
        try:
            res = pipeline.match_product(raw)
        except Exception as e:
            if is_stress:
                stress_crashes += 1
                stress_exceptions += 1
            else:
                bugs.append({"id": t["test_id"], "cat": "BUG-CRITICAL", "sev": "CRITICAL", "desc": str(e)})
            failed += 1
            continue
            
        lat = (time.perf_counter() - start_time) * 1000
        latencies.append(lat)
        
        status = res.get("status")
        matched_id = res.get("product_id") if status == "MATCHED" else None
        ext_price = res.get("extracted_price")
        ext_unit = res.get("extracted_unit")
        
        if status == "UNRESOLVED":
            unresolved_count += 1
        elif status == "AMBIGUOUS":
            ambiguous_count += 1
            
        # Check SKU
        match_ok = (matched_id == exp_id)
        if match_ok:
            sku_correct += 1
        else:
            if exp_id is None and matched_id is not None:
                false_positives += 1
                failure_patterns.append("False Positive Match")
                if not is_stress:
                    bugs.append({"id": t["test_id"], "cat": "BUG-FALSE-POSITIVE", "sev": "HIGH", "desc": f"Expected None, got {matched_id}"})
            elif exp_id is not None and matched_id is None:
                false_negatives += 1
                failure_patterns.append("False Negative Match")
                if not is_stress:
                    bugs.append({"id": t["test_id"], "cat": "BUG-MATCHING", "sev": "HIGH", "desc": f"Expected {exp_id}, got None"})
            else:
                failure_patterns.append("Wrong SKU Match")
                if not is_stress:
                    bugs.append({"id": t["test_id"], "cat": "BUG-MATCHING", "sev": "HIGH", "desc": f"Expected {exp_id}, got {matched_id}"})
                
        # Check MRP
        price_ok = (ext_price == exp_price) if exp_price is not None else (ext_price is None)
        if price_ok:
            mrp_correct += 1
        else:
            failure_patterns.append("MRP Extraction Failure")
            if not is_stress:
                bugs.append({"id": t["test_id"], "cat": "BUG-MRP", "sev": "MEDIUM", "desc": f"Expected {exp_price}, got {ext_price}"})
                
        # Check Unit
        unit_ok = (ext_unit == exp_unit) if exp_unit is not None else (ext_unit is None)
        if unit_ok:
            unit_correct += 1
        else:
            failure_patterns.append("Unit Extraction Failure")
            if not is_stress:
                bugs.append({"id": t["test_id"], "cat": "BUG-UNIT", "sev": "MEDIUM", "desc": f"Expected {exp_unit}, got {ext_unit}"})
                
        if match_ok and price_ok and unit_ok:
            passed += 1
        else:
            failed += 1
            
        if is_stress:
            stress_cases += 1
            
    latencies.sort()
    avg_lat = sum(latencies) / len(latencies) if latencies else 0
    p50_lat = latencies[int(len(latencies) * 0.50)] if latencies else 0
    p95_lat = latencies[int(len(latencies) * 0.95)] if latencies else 0
    
    # Write Bug Log
    bug_log_path = current_dir.parent / "BUG_LOG.md"
    with open(bug_log_path, "w", encoding="utf-8") as f:
        f.write("# OCR Module Bug Log\n\n")
        for b in bugs:
            f.write(f"- **{b['id']}** | {b['sev']} | {b['cat']} | {b['desc']}\n")
            
    # Calculate top patterns
    top_patterns = [p[0] for p in Counter(failure_patterns).most_common(10)]
    
    report = f"""PHASE 1 COMPLETE

TEST SUITE
-----------
Before: 16
After: {len(static_tests)}

Passed: {passed}
Failed: {failed}
Skipped: {skipped}

ACCURACY
-----------
SKU: {sku_correct/total_tests*100:.2f}%
MRP: {mrp_correct/total_tests*100:.2f}%
Unit: {unit_correct/total_tests*100:.2f}%

ROBUSTNESS
-----------
False positives: {false_positives}
False negatives: {false_negatives}
Unresolved: {unresolved_count}
Ambiguous: {ambiguous_count}

STRESS TEST
-----------
Cases: {stress_cases}
Crashes: {stress_crashes}
Exceptions: {stress_exceptions}
Average latency: {avg_lat:.2f} ms
P95 latency: {p95_lat:.2f} ms

BUGS DISCOVERED
-----------
Critical: {sum(1 for b in bugs if b['sev'] == 'CRITICAL')}
High: {sum(1 for b in bugs if b['sev'] == 'HIGH')}
Medium: {sum(1 for b in bugs if b['sev'] == 'MEDIUM')}
Low: {sum(1 for b in bugs if b['sev'] == 'LOW')}

TOP FAILURE PATTERNS
-----------
"""
    for i, pat in enumerate(top_patterns, 1):
        report += f"{i}. {pat}\n"
        
    print(report)

if __name__ == "__main__":
    run()

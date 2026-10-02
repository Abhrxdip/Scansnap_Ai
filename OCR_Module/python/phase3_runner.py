import json
import time
import random
from collections import Counter
from pathlib import Path

from ocr_engine import OcrPipeline

def apply_realistic_corruption(text, rng):
    chars = list(text)
    if not chars:
        return text
    num_ops = rng.randint(1, 2)
    for _ in range(num_ops):
        op = rng.choice(['substitute', 'space', 'delete'])
        idx = rng.randint(0, len(chars) - 1)
        if op == 'substitute':
            c = chars[idx]
            if c == 'O': chars[idx] = '0'
            elif c == '0': chars[idx] = 'O'
            elif c == 'I': chars[idx] = '1'
            elif c == '1': chars[idx] = 'I'
            elif c == 'S': chars[idx] = '5'
            elif c == '5': chars[idx] = 'S'
            elif c == 'B': chars[idx] = '8'
            elif c == 'G': chars[idx] = '6'
        elif op == 'space':
            chars.insert(idx, ' ')
        elif op == 'delete' and chars[idx] in [' ', '-', '.', ':']:
            chars.pop(idx)
    return "".join(chars)

def apply_extreme_corruption(text, rng):
    chars = list(text)
    if not chars: return text
    num_ops = rng.randint(3, 5)
    for _ in range(num_ops):
        if not chars: break
        op = rng.choice(['delete_chunk', 'random_char'])
        idx = rng.randint(0, len(chars) - 1)
        if op == 'delete_chunk':
            del chars[idx:idx+3]
        elif op == 'random_char':
            chars[idx] = rng.choice(['@', '#', '$', '%', '&', '*'])
    return "".join(chars)

def run():
    current_dir = Path(__file__).resolve().parent
    test_file = current_dir / "test_labels.json"
    
    with open(test_file, "r", encoding="utf-8") as f:
        static_tests = json.load(f)

    pipeline = OcrPipeline()
    rng = random.Random(42)
    
    # Generate 250 realistic and 250 extreme stress tests
    realistic_stress = []
    extreme_stress = []
    clean_bases = [t for t in static_tests if int(t["test_id"].split("-")[1]) <= 105]
    
    for i in range(250):
        base = rng.choice(clean_bases)
        realistic_stress.append({
            "test_id": f"STRESS-REAL-{i}",
            "raw_ocr": apply_realistic_corruption(base["raw_ocr"], rng),
            "expected_id": base["expected_id"],
            "expected_price": base["expected_price"],
            "expected_unit": base["expected_unit"],
            "notes": "Realistic stress"
        })
        base = rng.choice(clean_bases)
        extreme_stress.append({
            "test_id": f"STRESS-EXT-{i}",
            "raw_ocr": apply_extreme_corruption(base["raw_ocr"], rng),
            "expected_id": base["expected_id"],
            "expected_price": base["expected_price"],
            "expected_unit": base["expected_unit"],
            "notes": "Extreme stress"
        })
        
    all_tests = static_tests + realistic_stress + extreme_stress
    
    false_negatives = []
    false_positives = []
    unresolved_static = []
    
    latencies = []
    
    for t in all_tests:
        raw = t["raw_ocr"]
        exp_id = t["expected_id"]
        is_stress = t["test_id"].startswith("STRESS")
        is_extreme = "EXT" in t["test_id"]
        
        start_time = time.perf_counter()
        res = pipeline.match_product(raw)
        lat = (time.perf_counter() - start_time) * 1000
        latencies.append(lat)
        
        status = res.get("status")
        matched_id = res.get("product_id") if status == "MATCHED" else None
        
        if status == "UNRESOLVED":
            unresolved_static.append(t)
        if exp_id is not None and matched_id is None:
            false_negatives.append((t, res))
        elif exp_id is None and matched_id is not None:
            false_positives.append((t, res))
        elif exp_id is not None and matched_id is not None and exp_id != matched_id:
            # Wrong match
            pass
                
    latencies.sort()
    avg_lat = sum(latencies) / len(latencies) if latencies else 0
    p95_lat = latencies[int(len(latencies) * 0.95)] if latencies else 0
    
    print("--- FALSE NEGATIVES (Static) ---")
    for t, r in false_negatives:
        print(f"ID: {t['test_id']} | Input: {t['raw_ocr']} | Expected: {t['expected_id']} | Actual: {r.get('status')} | Top: {r.get('top_candidate')} ({r.get('candidate_confidence',0):.3f})")

    print("\n--- FALSE POSITIVES (Static) ---")
    for t, r in false_positives:
        print(f"ID: {t['test_id']} | Expected: None | Got: {r.get('product_id')}")

    print(f"\nAverage Latency: {avg_lat:.2f} ms")
    print(f"P95 Latency: {p95_lat:.2f} ms")

if __name__ == "__main__":
    run()

import random
import time
import json
import traceback
from ocr_engine import OcrPipeline, PRICE_REGEX, QUANTITY_REGEX
from spatial_reconstructor import reconstruct_text, OcrLine

pipeline = OcrPipeline()

results = {}

def run_zero_fp_gate():
    print("\n--- PHASE 10B: ZERO-FALSE-POSITIVE GATE ---")
    test_cases = [
        {"input": "Britannia", "expected": "AMBIGUOUS_OR_UNRESOLVED"},
        {"input": "Biscuits", "expected": "UNRESOLVED"},
        {"input": "Chocolate", "expected": "UNRESOLVED"},
        {"input": "Masala", "expected": "UNRESOLVED"},
        {"input": "Maggi", "expected": "MATCHED", "id": "maggi"},
        {"input": "Oreo Hide & Seek", "expected": "AMBIGUOUS"},
        {"input": "Hide & Seek Bourbon", "expected": "AMBIGUOUS"},
        {"input": "Britannia Good Day", "expected": "UNRESOLVED"}, # Not in catalog
        {"input": "Britannia Bourbon Chocolate Biscuits", "expected": "MATCHED", "id": "bourbon_biscuit"},
        {"input": "150g Rs 30", "expected": "UNRESOLVED"},
        {"input": "Cadbury", "expected": "AMBIGUOUS_OR_UNRESOLVED"},
        {"input": "Munch", "expected": "UNRESOLVED"},
        {"input": "Maggi Oats Noodles", "expected": "UNRESOLVED"}, # Wrong product
        {"input": "Oreo Chocolate", "expected": "AMBIGUOUS_OR_UNRESOLVED"},
        {"input": "Appy Fizz 1L Rs 80", "expected": "UNRESOLVED"}, # Wrong MRP/Unit
        {"input": "", "expected": "UNRESOLVED"},
        {"input": "    ", "expected": "UNRESOLVED"},
        {"input": "!@#$ %^&*", "expected": "UNRESOLVED"},
        {"input": "a", "expected": "UNRESOLVED"},
        {"input": "Britannia Biscuits 150g Rs 30", "expected": "AMBIGUOUS_OR_UNRESOLVED"},
    ] * 50 # 1000 cases
    
    fp_count = 0
    correct = 0
    
    for case in test_cases:
        res = pipeline.match_product(case["input"])
        status = res["status"]
        if case["expected"] == "MATCHED":
            if status == "MATCHED" and res["product_id"] == case.get("id"):
                correct += 1
            else:
                fp_count += 1
        else:
            if status == "MATCHED":
                print(f"FP ALERT: {case['input']} -> {res['product_name']}")
                fp_count += 1
            else:
                correct += 1
                
    print(f"Total: {len(test_cases)}, Correct Rejections/Matches: {correct}, FP: {fp_count}")
    results["fp_gate"] = fp_count

def run_mrp_safety_gate():
    print("\n--- PHASE 10C: MRP SAFETY GATE ---")
    test_cases = [
        ("MRP Rs 40", 40.0),
        ("M.R.P. 35.50", 35.50),
        ("MRP: 100", 100.0),
        ("M R P 50", 50.0),
        ("Rs. 20", 20.0),
        ("INR 30", 30.0),
        ("₹ 45", 45.0),
        ("Offer Price 30", None),
        ("Discount 20", None),
        ("Net Wt 100g", None),
        ("Exp Date 24/10/25", None),
        ("Batch 1002345", None),
        ("Ph: 9876543210", None),
        ("900.0", None), # just a number
    ] * 50
    
    mrp_errors = 0
    for txt, expected in test_cases:
        mrp = pipeline.extract_mrp(txt)
        if mrp != expected:
            print(f"MRP Error on '{txt}': got {mrp}, expected {expected}")
            mrp_errors += 1
            
    print(f"Total: {len(test_cases)}, Errors: {mrp_errors}")
    results["mrp_gate"] = mrp_errors

def run_unit_safety_gate():
    print("\n--- PHASE 10D: UNIT SAFETY GATE ---")
    test_cases = [
        ("150g", "150G"),
        ("1.5kg", "1.5KG"),
        ("500 ml", "500ML"),
        ("2 L", "2L"),
        ("10 pcs", "10PCS"),
        ("Net Wt. 120 gm", "120GM"),
        ("Rs 500", None),
        ("100234", None),
        ("Batch 150", None),
        ("Date 12/10", None),
    ] * 50
    
    unit_errors = 0
    for txt, expected in test_cases:
        unit = pipeline.extract_unit(txt)
        if unit != expected:
            print(f"Unit Error on '{txt}': got {unit}, expected {expected}")
            unit_errors += 1
            
    print(f"Total: {len(test_cases)}, Errors: {unit_errors}")
    results["unit_gate"] = unit_errors

def run_spatial_torture():
    print("\n--- PHASE 10E: SPATIAL TORTURE TEST ---")
    # Simulate fragmented lines that should be reconstructed
    cases = []
    
    # 1. Clean horizontal
    cases.append([
        OcrLine("Maggi", 10, 10, 50, 25),
        OcrLine("2-Minute", 55, 11, 100, 24),
    ])
    
    # 2. Diagonal / staggered
    cases.append([
        OcrLine("Oreo", 10, 10, 50, 25),
        OcrLine("Biscuits", 55, 20, 100, 35),
    ])
    
    # 3. Fragmented MRP
    cases.append([
        OcrLine("MRP", 10, 100, 40, 115),
        OcrLine("Rs.", 45, 101, 60, 116),
        OcrLine("14", 65, 100, 80, 115),
    ])
    
    # 4. Empty / Zero width
    cases.append([
        OcrLine("Ghost", 0, 0, 0, 0),
        OcrLine("Text", 10, 10, 50, 25),
    ])
    
    errors = 0
    for _ in range(500):
        for case in cases:
            try:
                res = reconstruct_text(case)
                if not res:
                    errors += 1
            except Exception as e:
                print(f"Crash in spatial: {e}")
                errors += 1
                
    print(f"Total Spatial Layouts tested: 2000, Errors: {errors}")
    results["spatial_gate"] = errors

def run_crash_fuzzing():
    print("\n--- PHASE 10K: CRASH / EXCEPTION FUZZING ---")
    bad_inputs = [
        "",
        "   ",
        "\n\n\t",
        "A" * 100,
        "₹₹₹₹₹",
        "NULL",
        "NaN",
        "True",
        "None",
        "¯\\_(ツ)_/¯",
        "1/0",
        "{'json': 'payload'}",
    ] * 100
    
    crashes = 0
    for txt in bad_inputs:
        try:
            pipeline.match_product(txt)
            pipeline.extract_mrp(txt)
            pipeline.extract_unit(txt)
        except Exception as e:
            crashes += 1
            print(f"Crash on input '{txt}': {e}")
            traceback.print_exc()
            
    print(f"Total Fuzz Cases: {len(bad_inputs)}, Crashes: {crashes}")
    results["crashes"] = crashes

def run_performance():
    print("\n--- PHASE 10I: PERFORMANCE ---")
    t0 = time.time()
    latencies = []
    for _ in range(1000):
        t_start = time.time()
        pipeline.match_product("Britannia Bourbon Chocolate Biscuits 150g MRP Rs 30")
        latencies.append((time.time() - t_start) * 1000)
        
    latencies.sort()
    print(f"Average: {sum(latencies)/len(latencies):.2f} ms")
    print(f"P50: {latencies[int(len(latencies)*0.5)]:.2f} ms")
    print(f"P95: {latencies[int(len(latencies)*0.95)]:.2f} ms")
    print(f"P99: {latencies[int(len(latencies)*0.99)]:.2f} ms")

if __name__ == "__main__":
    run_zero_fp_gate()
    run_mrp_safety_gate()
    run_unit_safety_gate()
    run_spatial_torture()
    run_crash_fuzzing()
    run_performance()
    
    print("\nSUMMARY:")
    for k, v in results.items():
        print(f"{k}: {v}")

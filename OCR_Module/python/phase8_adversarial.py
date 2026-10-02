import random
import json
from ocr_engine import OcrPipeline

def run_adversarial_tests():
    pipeline = OcrPipeline()
    
    # Generate 1000 tests
    test_cases = []
    
    # Hardcoded adversarial cases
    test_cases.append({"input": "Britannia Biscuits", "expected": "AMBIGUOUS"}) # generic brand
    test_cases.append({"input": "Cadbury", "expected": "AMBIGUOUS"}) # generic brand
    test_cases.append({"input": "Biscuits 150g Rs 30", "expected": "AMBIGUOUS"}) # generic category with price/unit
    test_cases.append({"input": "Oreo Hide & Seek", "expected": "AMBIGUOUS"}) # conflicting brands
    test_cases.append({"input": "Maggi 2-Minute", "expected": "MATCHED", "id": "maggi"})
    test_cases.append({"input": "Mggi Noodls Rs 14", "expected": "MATCHED", "id": "maggi"})
    test_cases.append({"input": "Soya Sticks 100g", "expected": "MATCHED", "id": "soya_sticks"})
    test_cases.append({"input": "Soya Sticks 500g Rs 120", "expected": "UNRESOLVED"}) # conflicting price/unit
    test_cases.append({"input": "Surf Excel Easy Wash 500g Rs 120", "expected": "MATCHED", "id": "surf_excel"})
    test_cases.append({"input": "Lays Chips 50g", "expected": "MATCHED", "id": "lays"})
    test_cases.append({"input": "Lays Chips 100g", "expected": "MATCHED", "id": "lays"})
    test_cases.append({"input": "Lyzol 500ml", "expected": "UNRESOLVED"}) # Should not match lays

    # Random generation
    base_products = [
        ("Oreo", "oreo"),
        ("Maggi", "maggi"),
        ("KitKat", "kitkat"),
        ("Kurkure", "kurkure"),
        ("Appy Fizz", "appe_fizz"),
        ("Bourbon", "bourbon_biscuit"),
        ("Thums Up", "thums_up")
    ]
    
    rng = random.Random(42)
    for i in range(988):
        prod, expected_id = rng.choice(base_products)
        
        # Corruptions
        corrupted = list(prod)
        num_corruptions = rng.randint(0, 3)
        for _ in range(num_corruptions):
            idx = rng.randint(0, len(corrupted)-1)
            char_to_add = rng.choice("abcdefghijklmnopqrstuvwxyz0123456789")
            if rng.random() > 0.5:
                corrupted[idx] = char_to_add
            else:
                corrupted.insert(idx, char_to_add)
        
        test_str = "".join(corrupted)
        
        if rng.random() > 0.7:
            test_str += f" {rng.randint(10, 500)}g"
        if rng.random() > 0.7:
            test_str += f" MRP {rng.randint(10, 150)}"
            
        test_cases.append({"input": test_str, "expected": "MATCHED" if num_corruptions <= 1 else "UNRESOLVED", "id": expected_id})
        
    results = {"CORRECT": 0, "INCORRECT": 0, "AMBIGUOUS": 0, "UNRESOLVED": 0}
    
    for case in test_cases:
        res = pipeline.match_product(case["input"])
        status = res["status"]
        
        if status == "AMBIGUOUS":
            results["AMBIGUOUS"] += 1
            if case["expected"] != "AMBIGUOUS":
                # We count ambiguous as safe unresolved, not incorrect
                pass
        elif status == "UNRESOLVED":
            results["UNRESOLVED"] += 1
        elif status == "MATCHED":
            if case.get("expected") == "AMBIGUOUS":
                print(f"FALSE POSITIVE: Input '{case['input']}' matched {res['product_name']} but should be AMBIGUOUS")
                results["INCORRECT"] += 1
            elif res["product_id"] != case.get("id"):
                print(f"FALSE POSITIVE: Input '{case['input']}' matched {res['product_name']} instead of {case.get('id')}")
                results["INCORRECT"] += 1
            else:
                results["CORRECT"] += 1
                
    print(f"Adversarial Tests: {len(test_cases)}")
    print(f"Correct: {results['CORRECT']}")
    print(f"Incorrect / False Positives: {results['INCORRECT']}")
    print(f"Ambiguous: {results['AMBIGUOUS']}")
    print(f"Unresolved: {results['UNRESOLVED']}")

if __name__ == '__main__':
    run_adversarial_tests()

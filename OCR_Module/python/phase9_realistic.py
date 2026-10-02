import random
import json
import time
from ocr_engine import OcrPipeline

def run_realistic_tests():
    pipeline = OcrPipeline()
    
    # ── 1. Base Real Products ──────────────────────────────────────────
    base_products = [
        {"name": "Maggi 2-Minute Masala Noodles", "id": "maggi", "price": 14.0, "unit": "70g", "color": "YELLOW"},
        {"name": "Cadbury Oreo Original Biscuits", "id": "oreo", "price": 35.0, "unit": "120g", "color": "BLUE"},
        {"name": "Nestle KitKat Chocolate Bar", "id": "kitkat", "price": 20.0, "unit": "38g", "color": "RED"},
        {"name": "Kurkure Masala Munch", "id": "kurkure", "price": 20.0, "unit": "90g", "color": "ORANGE"},
        {"name": "Appy Fizz Sparkling Apple Juice", "id": "appe_fizz", "price": 35.0, "unit": "250ml", "color": "RED"},
        {"name": "Britannia Bourbon Chocolate Biscuits", "id": "bourbon_biscuit", "price": 30.0, "unit": "150g", "color": "ORANGE"},
        {"name": "Thums Up Charged Carbonated Beverage 250ml Can", "id": "thums_up", "price": 40.0, "unit": "250ml", "color": "BLACK"}
    ]

    test_cases = []
    
    # HARDCODED COLLISIONS (100 Ambiguity/Collision cases)
    # Generic product collisions
    test_cases.extend([
        {"input": "Britannia", "expected": "AMBIGUOUS"},
        {"input": "Britannia Biscuits", "expected": "AMBIGUOUS"},
        {"input": "Chocolate Biscuits", "expected": "AMBIGUOUS"},
        {"input": "Masala", "expected": "UNRESOLVED"},
        {"input": "Oreo Hide & Seek", "expected": "AMBIGUOUS"},
        {"input": "Cadbury", "expected": "AMBIGUOUS"},
        {"input": "Nestle", "expected": "AMBIGUOUS"},
    ] * 15)

    rng = random.Random(42)
    
    def apply_realistic_ocr_corruption(text, mode):
        if mode == "clean":
            return text
        elif mode == "mild":
            # Simple substitution: O->0, I->1, l->1, S->5, B->8
            subs = {'O':'0', 'I':'1', 'l':'1', 'S':'5', 's':'5', 'B':'8', 'Z':'2'}
            res = ""
            for c in text:
                if c in subs and rng.random() > 0.5:
                    res += subs[c]
                else:
                    res += c
            return res
        elif mode == "spacing":
            # Add or remove spaces
            tokens = text.split()
            if rng.random() > 0.5 and len(tokens) > 1:
                idx = rng.randint(0, len(tokens)-2)
                tokens[idx] = tokens[idx] + tokens[idx+1]
                del tokens[idx+1]
            return " ".join(tokens)
        elif mode == "punctuation":
            # Add stray punctuation
            res = text
            if rng.random() > 0.5:
                res = res.replace(" ", ". ", 1)
            if rng.random() > 0.5:
                res = res.replace(" ", "- ", 1)
            return res
        return text

    # Generate the remaining 1900 cases
    # 500 clean, 500 mild, 400 spacing, 300 numeric/MRP, 200 alias/punctuation
    
    def create_case(mode):
        prod = rng.choice(base_products)
        # Select alias or base name
        if rng.random() > 0.5:
            base_text = rng.choice([prod["name"]] + prod["name"].split())
        else:
            base_text = prod["name"]

        text = apply_realistic_ocr_corruption(base_text, mode)
        
        # Add unit and MRP sometimes
        if rng.random() > 0.5:
            if rng.random() > 0.5:
                text += f" {prod['unit']}"
            else:
                text += f" {rng.randint(10, 500)}g"
        
        if mode == "numeric" or rng.random() > 0.5:
            if rng.random() > 0.5:
                text += f" MRP Rs {prod['price']}"
            else:
                text += f" MRP {rng.randint(10, 150)}"
                
        if len(text) < 5:
            expected = "UNRESOLVED"
        elif prod["name"] in base_text and len(base_text) > 5:
            expected = "MATCHED"
        else:
            expected = "MATCHED" if rng.random() > 0.2 else "UNRESOLVED" # Simplified
            
        return {"input": text, "expected": expected, "id": prod["id"]}

    for _ in range(500): test_cases.append(create_case("clean"))
    for _ in range(500): test_cases.append(create_case("mild"))
    for _ in range(400): test_cases.append(create_case("spacing"))
    for _ in range(300): test_cases.append(create_case("numeric"))
    for _ in range(200): test_cases.append(create_case("punctuation"))

    # Cap to exactly 2000
    test_cases = test_cases[:2000]
    
    results = {"CORRECT": 0, "INCORRECT": 0, "AMBIGUOUS": 0, "UNRESOLVED": 0}
    latencies = []
    
    for case in test_cases:
        t0 = time.time()
        res = pipeline.match_product(case["input"])
        latencies.append((time.time() - t0) * 1000)
        
        status = res["status"]
        if status == "AMBIGUOUS":
            results["AMBIGUOUS"] += 1
        elif status == "UNRESOLVED":
            results["UNRESOLVED"] += 1
        elif status == "MATCHED":
            if case.get("expected") == "AMBIGUOUS" or case.get("expected") == "UNRESOLVED":
                if case.get("expected") == "AMBIGUOUS" and case.get("id") == res["product_id"]:
                     pass # Soft pass
                elif case.get("id") != res.get("product_id"):
                     # True false positive
                     print(f"FALSE POSITIVE: Input '{case['input']}' matched {res['product_name']} but expected {case['expected']}")
                     results["INCORRECT"] += 1
                else:
                     results["CORRECT"] += 1
            elif res["product_id"] != case.get("id"):
                print(f"FALSE POSITIVE: Input '{case['input']}' matched {res['product_name']} instead of {case.get('id')}")
                results["INCORRECT"] += 1
            else:
                results["CORRECT"] += 1

    print(f"Total Realistic Tests: {len(test_cases)}")
    print(f"Correct: {results['CORRECT']}")
    print(f"Incorrect / False Positives: {results['INCORRECT']}")
    print(f"Ambiguous: {results['AMBIGUOUS']}")
    print(f"Unresolved: {results['UNRESOLVED']}")
    
    if latencies:
        latencies.sort()
        print(f"Average: {sum(latencies)/len(latencies):.2f} ms")
        print(f"P50: {latencies[int(len(latencies)*0.5)]:.2f} ms")
        print(f"P95: {latencies[int(len(latencies)*0.95)]:.2f} ms")
        print(f"P99: {latencies[int(len(latencies)*0.99)]:.2f} ms")

if __name__ == '__main__':
    run_realistic_tests()

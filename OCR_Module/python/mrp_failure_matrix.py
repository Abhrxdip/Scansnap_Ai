import re
from ocr_engine import OcrPipeline

def run_mrp_matrix():
    pipeline = OcrPipeline()
    
    cases = [
        ("Normal", "MRP 30", 30.0),
        ("Spaced", "M R P 30", 30.0),
        ("Dotted", "M.R.P. 30", 30.0),
        ("Newline", "M R P\n30", 30.0),
        ("Newline with Rupee", "MRP\n₹30", 30.0),
        ("Space Rupee", "MRP ₹ 30", 30.0),
        ("Space Rs", "MRP Rs 30", 30.0),
        ("Space INR", "MRP INR 30", 30.0),
        ("Colon", "MRP: 30", 30.0),
        ("Dash", "MRP - 30", 30.0),
        ("Adversarial: Unrelated Qty", "MRP 30 500ml", 30.0),
        ("Adversarial: Date", "12/03/2023", None),
        ("Adversarial: Phone", "9876543210", None),
        ("Adversarial: Code", "A1B2 3000", None),
        ("Adversarial: Qty near MRP", "100g MRP 30", 30.0),
        ("Adversarial: Weight near MRP", "Net Weight 500g MRP 45.50", 45.50),
        ("Adversarial: MRP without value", "MRP", None)
    ]
    
    passed = 0
    failed = 0
    for name, text, expected in cases:
        result = pipeline.extract_mrp(text)
        if result == expected:
            passed += 1
            print(f"PASS: {name} | Input: {repr(text)} | Got: {result}")
        else:
            failed += 1
            print(f"FAIL: {name} | Input: {repr(text)} | Expected: {expected} | Got: {result}")
            
    print(f"\nTotal: {len(cases)} | Passed: {passed} | Failed: {failed}")

if __name__ == "__main__":
    run_mrp_matrix()

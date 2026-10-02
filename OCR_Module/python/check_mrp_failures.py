import json
from ocr_engine import OcrPipeline

def check_mrp_failures():
    with open("test_labels.json", "r") as f:
        tests = json.load(f)
        
    pipeline = OcrPipeline()
    failures = []
    
    for t in tests:
        raw_ocr = t["raw_ocr"]
        expected_price = t["expected_price"]
        actual_price = pipeline.extract_mrp(raw_ocr)
        
        if expected_price is not None and actual_price != expected_price:
            failures.append((raw_ocr, expected_price, actual_price))
            
    print(f"Total Static MRP Failures: {len(failures)}")
    for raw, exp, act in failures:
        print(f"EXPECTED: {exp} | GOT: {act} | RAW: {repr(raw)}")

if __name__ == "__main__":
    check_mrp_failures()

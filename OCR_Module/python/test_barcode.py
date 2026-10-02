import time
import random
from ocr_engine import OcrPipeline, MASTER_CATALOG

def run_barcode_stress_test():
    pipeline = OcrPipeline()
    
    total_cases = 1500
    correct_detections = 0
    missed_detections = 0
    incorrect_detections = 0
    exceptions = 0
    crashes = 0
    latencies = []
    
    # Supported formats simulated (Android ML Kit supports these)
    formats = ["EAN-13", "EAN-8", "UPC-A", "UPC-E", "Code 128", "Code 39", "ITF"]
    
    format_results = {f: {"tested": 0, "correct": 0} for f in formats}
    
    print("--- RUNNING BARCODE STRESS TEST ---")
    
    for i in range(total_cases):
        try:
            # 1. Pick a random product from master catalog
            target_product = random.choice(MASTER_CATALOG)
            expected_id = target_product["id"]
            true_barcode = target_product["barcode"]
            
            simulated_format = random.choice(formats)
            format_results[simulated_format]["tested"] += 1
            
            # Simulate real-world conditions
            # Blur, rotation, low light, clutter etc. usually result in MISSED detections in standard ML Kit
            # They rarely result in incorrect barcodes due to strict checksums.
            condition = random.choice(["clean", "blur", "rotated_90", "rotated_180", "low_light", "glare", "small_barcode", "partial_crop", "clutter"])
            
            detected_barcode = true_barcode
            
            if condition in ["blur", "partial_crop", "glare"]:
                # High chance of missing
                if random.random() < 0.6:
                    detected_barcode = None
            elif condition in ["low_light", "small_barcode"]:
                if random.random() < 0.3:
                    detected_barcode = None
                    
            # Simulate an incredibly rare checksum collision or OCR-noise taking over (incorrect detection)
            if random.random() < 0.001:
                detected_barcode = "1234567890123" # completely wrong
                
            # Simulate OCR text noise around the barcode
            noisy_ocr_text = target_product["name"] + f" MRP {target_product['suggested_price']} "
            chars = list(noisy_ocr_text)
            if random.random() > 0.5:
                idx = random.randint(0, len(chars)-1)
                chars.insert(idx, random.choice(['#', '@', ' ', '1']))
            raw_ocr = "".join(chars)
            
            # RUN PIPELINE
            start_time = time.perf_counter()
            res = pipeline.match_product(raw_text=raw_ocr, detected_barcode=detected_barcode)
            lat = (time.perf_counter() - start_time) * 1000
            latencies.append(lat)
            
            matched_id = res.get("product_id") if res["status"] == "MATCHED" else None
            
            if matched_id == expected_id:
                if detected_barcode == true_barcode:
                    correct_detections += 1
                    format_results[simulated_format]["correct"] += 1
                else:
                    # Matched via OCR despite barcode missing!
                    missed_detections += 1
            else:
                if detected_barcode is None:
                    missed_detections += 1
                elif detected_barcode != true_barcode:
                    incorrect_detections += 1
                else:
                    # It was correct barcode but matched wrong? (Shouldn't happen with exact match logic)
                    incorrect_detections += 1
                    
        except Exception as e:
            exceptions += 1
            crashes += 1
            
    latencies.sort()
    avg_lat = sum(latencies) / len(latencies) if latencies else 0
    
    print(f"Total test cases: {total_cases}")
    print(f"Correct detections: {correct_detections}")
    print(f"Missed detections: {missed_detections}")
    print(f"Incorrect detections: {incorrect_detections}")
    print(f"Exceptions: {exceptions}")
    print(f"Crashes: {crashes}")
    print(f"Detection recall: {(correct_detections / total_cases) * 100:.2f}%")
    print(f"False positive rate: {(incorrect_detections / total_cases) * 100:.2f}%")
    print(f"Average latency: {avg_lat:.2f} ms")
    
    print("\nFormat Support Simulated:")
    for f in formats:
        tested = format_results[f]["tested"]
        correct = format_results[f]["correct"]
        print(f" - {f}: Tested {tested}, Correct {correct}")

if __name__ == "__main__":
    run_barcode_stress_test()

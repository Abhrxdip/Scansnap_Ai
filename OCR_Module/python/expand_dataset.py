import json
from pathlib import Path

def generate_expanded_dataset():
    p = Path("C:/Users/ankit/.gemini/antigravity-ide/scratch/Scansnap_Ai/OCR_Module/python/test_labels.json")
    with open(p, "r", encoding="utf-8") as f:
        tests = json.load(f)
        
    # Keep only original 16 if it was previously expanded
    tests = [t for t in tests if int(t["test_id"].split("-")[1]) <= 16]

    new_tests = [
        # A. CLEAN OCR
        {"test_id": "TC-101", "raw_ocr": "Maggi 2-Minute Masala Noodles Net Wt 140g MRP Rs 28.00", "expected_id": "maggi", "expected_price": 28.0, "expected_unit": "140G", "notes": "Clean Maggi 140g"},
        {"test_id": "TC-102", "raw_ocr": "Cadbury Oreo Original Biscuits 300g MRP ₹ 80.00", "expected_id": "oreo", "expected_price": 80.0, "expected_unit": "300G", "notes": "Clean Oreo 300g"},
        {"test_id": "TC-103", "raw_ocr": "Britannia Bourbon Chocolate Biscuits 150g MRP: 30", "expected_id": "bourbon_biscuit", "expected_price": 30.0, "expected_unit": "150G", "notes": "Clean Bourbon"},
        {"test_id": "TC-104", "raw_ocr": "Britannia Treat Jim Jam Biscuits 150g Rs. 35", "expected_id": "jim_jam", "expected_price": 35.0, "expected_unit": "150G", "notes": "Clean Jim Jam"},
        {"test_id": "TC-105", "raw_ocr": "Thums Up Charged Carbonated Beverage 750ml MRP 40.00", "expected_id": "thums_up", "expected_price": 40.0, "expected_unit": "750ML", "notes": "Clean Thums Up 750ml"},

        # B. OCR CHARACTER CORRUPTION
        {"test_id": "TC-106", "raw_ocr": "Cadbury ORE0 Original Biscuits", "expected_id": "oreo", "expected_price": None, "expected_unit": None, "notes": "O -> 0 substitution"},
        {"test_id": "TC-107", "raw_ocr": "Britannia BURBON Chocolate Biscuits", "expected_id": "bourbon_biscuit", "expected_price": None, "expected_unit": None, "notes": "Missing O"},
        {"test_id": "TC-108", "raw_ocr": "Britannia BOURB0N Chocolate Biscuits", "expected_id": "bourbon_biscuit", "expected_price": None, "expected_unit": None, "notes": "O -> 0 in Bourbon"},
        {"test_id": "TC-109", "raw_ocr": "M4GGI 2-Minute Masala Noodles", "expected_id": "maggi", "expected_price": None, "expected_unit": None, "notes": "A -> 4 substitution"},

        # C. WORD SPACING CORRUPTION
        {"test_id": "TC-110", "raw_ocr": "Britannia Treat JIMJAM Biscuits", "expected_id": "jim_jam", "expected_price": None, "expected_unit": None, "notes": "Merged Jim Jam"},
        {"test_id": "TC-111", "raw_ocr": "Britannia Treat JIM  JAM Biscuits", "expected_id": "jim_jam", "expected_price": None, "expected_unit": None, "notes": "Extra spaces Jim Jam"},
        {"test_id": "TC-112", "raw_ocr": "Britannia Treat JIM-JAM Biscuits", "expected_id": "jim_jam", "expected_price": None, "expected_unit": None, "notes": "Hyphen Jim Jam"},
        {"test_id": "TC-113", "raw_ocr": "THUMSUP Charged", "expected_id": "thums_up", "expected_price": None, "expected_unit": None, "notes": "Merged Thums Up"},
        {"test_id": "TC-114", "raw_ocr": "THUM S UP Charged", "expected_id": "thums_up", "expected_price": None, "expected_unit": None, "notes": "Split Thums Up"},
        {"test_id": "TC-115", "raw_ocr": "THUM\nS UP", "expected_id": "thums_up", "expected_price": None, "expected_unit": None, "notes": "Newline split Thums Up"},

        # D. MRP CASES
        {"test_id": "TC-116", "raw_ocr": "MRP 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Simple MRP"},
        {"test_id": "TC-117", "raw_ocr": "MRP: 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "MRP with colon"},
        {"test_id": "TC-118", "raw_ocr": "MRP ₹30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "MRP rupee symbol"},
        {"test_id": "TC-119", "raw_ocr": "MRP ₹ 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "MRP rupee space"},
        {"test_id": "TC-120", "raw_ocr": "MRP Rs 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "MRP Rs"},
        {"test_id": "TC-121", "raw_ocr": "MRP Rs. 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "MRP Rs dot"},
        {"test_id": "TC-122", "raw_ocr": "MRP INR 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "MRP INR"},
        {"test_id": "TC-123", "raw_ocr": "₹30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Just rupee"},
        {"test_id": "TC-124", "raw_ocr": "₹ 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Rupee space"},
        {"test_id": "TC-125", "raw_ocr": "Rs 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Rs 30"},
        {"test_id": "TC-126", "raw_ocr": "Rs. 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Rs. 30"},
        {"test_id": "TC-127", "raw_ocr": "INR 30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "INR 30"},
        {"test_id": "TC-128", "raw_ocr": "MRP\n30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Fragmented: MRP \\n 30"},
        {"test_id": "TC-129", "raw_ocr": "MRP\n₹30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Fragmented: MRP \\n rupee"},
        {"test_id": "TC-130", "raw_ocr": "MRP\n₹\n30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Fragmented: completely split"},
        {"test_id": "TC-131", "raw_ocr": "M R P\n30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Fragmented: spaced MRP"},
        {"test_id": "TC-132", "raw_ocr": "MRP:\nRs.\n30", "expected_id": None, "expected_price": 30.0, "expected_unit": None, "notes": "Fragmented: MRP: \\n Rs. \\n 30"},

        # E. MULTIPLE PRICE CASES
        {"test_id": "TC-133", "raw_ocr": "MRP 40 Selling Price 35", "expected_id": None, "expected_price": 40.0, "expected_unit": None, "notes": "MRP + selling price"},
        {"test_id": "TC-134", "raw_ocr": "MRP 50 Discount 10", "expected_id": None, "expected_price": 50.0, "expected_unit": None, "notes": "MRP + discount"},
        {"test_id": "TC-135", "raw_ocr": "MRP 100 Date 25/12", "expected_id": None, "expected_price": 100.0, "expected_unit": None, "notes": "Price + date"},
        {"test_id": "TC-136", "raw_ocr": "MRP 250 Barcode 8901058852311", "expected_id": None, "expected_price": 250.0, "expected_unit": None, "notes": "Price + barcode-like number"},

        # F. QUANTITY / UNIT CASES
        {"test_id": "TC-137", "raw_ocr": "Maggi 10g", "expected_id": "maggi", "expected_price": None, "expected_unit": "10G", "notes": "10g"},
        {"test_id": "TC-138", "raw_ocr": "Maggi 20 g", "expected_id": "maggi", "expected_price": None, "expected_unit": "20G", "notes": "20 g"},
        {"test_id": "TC-139", "raw_ocr": "Maggi 100g", "expected_id": "maggi", "expected_price": None, "expected_unit": "100G", "notes": "100g"},
        {"test_id": "TC-140", "raw_ocr": "Maggi 100 g", "expected_id": "maggi", "expected_price": None, "expected_unit": "100G", "notes": "100 g"},
        {"test_id": "TC-141", "raw_ocr": "Amul 500ml", "expected_id": "amul_ice_cream", "expected_price": None, "expected_unit": "500ML", "notes": "500ml"},
        {"test_id": "TC-142", "raw_ocr": "Amul 500 ml", "expected_id": "amul_ice_cream", "expected_price": None, "expected_unit": "500ML", "notes": "500 ml"},
        {"test_id": "TC-143", "raw_ocr": "Surf 1L", "expected_id": None, "expected_price": None, "expected_unit": "1L", "notes": "1L"},
        {"test_id": "TC-144", "raw_ocr": "Surf 1 L", "expected_id": None, "expected_price": None, "expected_unit": "1L", "notes": "1 L"},
        {"test_id": "TC-145", "raw_ocr": "Surf 1kg", "expected_id": None, "expected_price": None, "expected_unit": "1KG", "notes": "1kg"},
        {"test_id": "TC-146", "raw_ocr": "Surf 1 kg", "expected_id": None, "expected_price": None, "expected_unit": "1KG", "notes": "1 kg"},
        {"test_id": "TC-147", "raw_ocr": "Drug 250mg", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "250mg shouldn't be parsed if mg isn't supported, or should be. We expect None currently based on regex."},
        {"test_id": "TC-148", "raw_ocr": "Maggi 1O0g", "expected_id": "maggi", "expected_price": None, "expected_unit": "100G", "notes": "OCR corrupted 1O0g"},
        {"test_id": "TC-149", "raw_ocr": "Amul 5OOml", "expected_id": "amul_ice_cream", "expected_price": None, "expected_unit": "500ML", "notes": "OCR corrupted 5OOml"},

        # G. NOISE
        {"test_id": "TC-150", "raw_ocr": "BEST BEFORE 12 MONTHS FSSAI LIC NO 10014022002758 BATCH NO 123", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Regulatory noise"},
        {"test_id": "TC-151", "raw_ocr": "NET WEIGHT 100g MANUFACTURED IN INDIA INGREDIENTS SUGAR", "expected_id": None, "expected_price": None, "expected_unit": "100G", "notes": "Marketing/ingredients noise"},
        {"test_id": "TC-152", "raw_ocr": "CUSTOMER CARE LICENSE NUMBER 12345", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Contact noise"},

        # H. NEGATIVE TESTS
        {"test_id": "TC-153", "raw_ocr": "", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Empty text"},
        {"test_id": "TC-154", "raw_ocr": "   \n  \t ", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Whitespace only"},
        {"test_id": "TC-155", "raw_ocr": ".,-!?:;", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Punctuation only"},
        {"test_id": "TC-156", "raw_ocr": "XyzQwertyAsdf", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Random characters"},
        {"test_id": "TC-157", "raw_ocr": "9876543210 12345", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Random numbers"},
        {"test_id": "TC-158", "raw_ocr": "Date: 12-05-2023", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Dates"},
        {"test_id": "TC-159", "raw_ocr": "+91-9876543210", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Phone numbers"},
        {"test_id": "TC-160", "raw_ocr": "Snickers Chocolate Bar", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Unrelated product name"},

        # I. AMBIGUOUS CASES
        # Currently the engine doesn't have a concept of AMBIGUOUS (only MATCHED/UNRESOLVED).
        # We will expect UNRESOLVED or AMBIGUOUS (which evaluates to None for matching)
        {"test_id": "TC-161", "raw_ocr": "Britannia Biscuits", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Ambiguous: could be Bourbon, Milk Bikis, Jim Jam. Expected Unresolved."},

        # J. ANTI-CONFUSION CASES
        {"test_id": "TC-162", "raw_ocr": "Maaza Mango Drink", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Similar brand/family check (Maaza vs Maggi)"},
        {"test_id": "TC-163", "raw_ocr": "Munch Chocolate", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Munch vs Maggi"},
        {"test_id": "TC-164", "raw_ocr": "Soya Sticks Snack", "expected_id": None, "expected_price": None, "expected_unit": None, "notes": "Soya Sticks vs Snickers/Surf Excel"}
    ]

    tests.extend(new_tests)

    with open(p, "w", encoding="utf-8") as f:
        json.dump(tests, f, indent=2)

if __name__ == "__main__":
    generate_expanded_dataset()

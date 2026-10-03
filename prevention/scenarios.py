"""
ScanSnap AI — Retail Shrinkage & Loss Prevention Pre-Configured Demo Scenarios
Provides deterministic test cases and judge demonstrations for the 7 scenarios.
"""

from typing import Dict, Any, List

DEMO_SCENARIOS: Dict[str, Dict[str, Any]] = {
    "product_switching": {
        "id": "product_switching",
        "name": "Product Ticket Switching",
        "category": "High Shrink",
        "description": "Shopper placed a cheap barcode (Wild Stone Soap ₹40) over a high-value Puma Classic T-Shirt (₹1,299).",
        "scanned_sku": {
            "barcode": "8901030383418",
            "name": "Wild Stone Forest Spice Deodorant Soap 125g",
            "category": "Personal Care",
            "price": 40.0
        },
        "detected_sku": {
            "name": "Puma Regular Fit T-Shirt",
            "category": "Fashion",
            "brand": "Puma",
            "price": 1299.0,
            "confidence": 0.94
        },
        "expected_risk": "CRITICAL",
        "expected_discrepancy": 1259.0
    },
    "fake_scan": {
        "id": "fake_scan",
        "name": "Fake Scan / Barcode Obscuration",
        "category": "Ghost Scanning",
        "description": "Shopper swept boAt Bluetooth Headphones across scanner while covering the barcode with palm.",
        "detected_item_name": "boAt Rockerz 450 Bluetooth Headphones",
        "estimated_price": 1499.0,
        "motion_in_scan_zone": True,
        "barcode_read_success": False,
        "item_placed_in_bag": True,
        "expected_risk": "HIGH"
    },
    "items_in_basket": {
        "id": "items_in_basket",
        "name": "Unscanned Items Left in Cart",
        "category": "Cart Leftover",
        "description": "Shopper hit 'Proceed to Payment' while 2 items remain in the bottom basket.",
        "basket_item_count": 2,
        "checkout_initiated": True,
        "basket_items": [
            {"name": "Amul Ice Cream Cup Vanilla Magic 100ml", "price": 30.0},
            {"name": "Britannia Cake Gobbles Choco Chill 65g", "price": 25.0}
        ],
        "expected_risk": "HIGH"
    },
    "multi_product": {
        "id": "multi_product",
        "name": "Multi-Product Stacking",
        "category": "Double Loading",
        "description": "Shopper placed 2 items simultaneously in the scan zone while only 1 barcode was read.",
        "detections_count": 2,
        "barcodes_registered": 1,
        "expected_risk": "MEDIUM"
    },
    "hidden_items": {
        "id": "hidden_items",
        "name": "Hidden Merchandise in Cart Tray",
        "category": "Concealment",
        "description": "Computer vision detected high-value merchandise concealed under the lower trolley tray.",
        "bottom_rack_detected": True,
        "hidden_items": [
            {"name": "Nike Revolution 6 Running Shoes", "price": 2499.0}
        ],
        "expected_risk": "HIGH"
    },
    "sweethearting": {
        "id": "sweethearting",
        "name": "Cashier Sweethearting / Bypass",
        "category": "Collusion",
        "description": "Cashier used deliberate hand motion to obscure scanner window during scan swipe.",
        "cashier_scan_motion": True,
        "barcode_decoded": False,
        "item_passed_to_bag": True,
        "expected_risk": "CRITICAL"
    },
    "age_verification": {
        "id": "age_verification",
        "name": "Age-Restricted Compliance",
        "category": "Regulatory",
        "description": "Customer attempted self-checkout of age-restricted merchandise.",
        "product_name": "Premium Beer Bottle 650ml",
        "product_category": "Alcohol / Tobacco",
        "customer_verified": False,
        "expected_risk": "HIGH"
    }
}

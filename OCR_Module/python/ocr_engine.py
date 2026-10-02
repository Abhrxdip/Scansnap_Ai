#!/usr/bin/env python3
"""
ScanSnap AI — Standalone OCR & Packaging Intelligence Engine (Python Edition)
Replicates and extends the on-device ML Kit OCR token parsing, noise filtering,
price/unit regex extraction, font-alias resolution, and fuzzy catalog matching.

Usage:
    python ocr_engine.py "MAGGI 2-Minute Masala Noodles Net Wt 70g MRP Rs 14.00"
    python ocr_engine.py --interactive
"""

import sys
import re
import json
from typing import Optional, Dict, Any, List, Tuple

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

# ── 1. Master Packaging Catalog & Reference Metadata ─────────────────────────
MASTER_CATALOG = [
    {
        "id": "maggi",
        "name": "Maggi 2-Minute Masala Noodles",
        "category": "Instant Foods",
        "suggested_price": 14.0,
        "barcode": "8901058852311",
        "aliases": ["maggi", "maggi noodles", "2-minute masala", "nestle maggi"],
        "color": "YELLOW",
        "typical_units": ["70g", "140g", "280g"]
    },
    {
        "id": "oreo",
        "name": "Cadbury Oreo Original Biscuits",
        "category": "Snacks & Biscuits",
        "suggested_price": 35.0,
        "barcode": "7622201737018",
        "aliases": ["oreo", "cadbury oreo", "oreo vanilla", "oreo cookies"],
        "color": "BLUE",
        "typical_units": ["120g", "300g"]
    },
    {
        "id": "amul_ice_cream",
        "name": "Amul Ice Cream Cup Vanilla Magic 100ml",
        "category": "Dairy & Bakery",
        "suggested_price": 30.0,
        "barcode": "8901262010014",
        "aliases": ["amul ice cream", "vanilla magic", "amul cup", "amul vanilla", "amul"],
        "color": "BLUE",
        "typical_units": ["100ml", "500ml", "1L"]
    },
    {
        "id": "cake",
        "name": "Britannia Cake Gobbles Choco Chill 65g",
        "category": "Dairy & Bakery",
        "suggested_price": 30.0,
        "barcode": "8901063142018",
        "aliases": ["britannia cake", "gobbles choco chill", "choco chill", "britannia gobbles"],
        "color": "YELLOW",
        "typical_units": ["65g", "120g"]
    },
    {
        "id": "cerave",
        "name": "CeraVe Hydrating Cleanser 236ml",
        "category": "Personal Care & Hygiene",
        "suggested_price": 900.0,
        "barcode": "3337875597371",
        "aliases": ["cerave hydrating cleanser", "cerave cleanser", "cerave lotion"],
        "color": "GREEN",
        "typical_units": ["236ml", "473ml"]
    },
    {
        "id": "hns_shampoo",
        "name": "Head & Shoulders Cool Menthol Anti-Dandruff Shampoo 180ml",
        "category": "Personal Care & Hygiene",
        "suggested_price": 250.0,
        "barcode": "4902430730013",
        "aliases": ["head and shoulders", "head & shoulders", "cool menthol", "hns shampoo"],
        "color": "BLUE",
        "typical_units": ["180ml", "340ml", "650ml"]
    },
    {
        "id": "nestle_milk_powder",
        "name": "Nestle Everyday Dairy Whitener Milk Powder 20g",
        "category": "Dairy & Bakery",
        "suggested_price": 10.0,
        "barcode": "8901058852314",
        "aliases": ["nestle everyday", "dairy whitener", "milk powder", "everyday"],
        "color": "YELLOW",
        "typical_units": ["20g", "200g", "400g", "1kg"]
    },
    {
        "id": "plum",
        "name": "Plum Green Tea Pore Cleansing Face Wash 100ml",
        "category": "Personal Care & Hygiene",
        "suggested_price": 350.0,
        "barcode": "8906118410214",
        "aliases": ["plum green tea", "pore cleansing face wash", "plum face wash", "plum skincare"],
        "color": "PURPLE",
        "typical_units": ["100ml", "50ml"]
    },
    {
        "id": "thums_up",
        "name": "Thums Up Charged Carbonated Beverage 250ml Can",
        "category": "Beverages & Drinks",
        "suggested_price": 20.0,
        "barcode": "8901764012211",
        "aliases": ["thums up", "thumsup", "charged carbonated beverage", "toofani"],
        "color": "BLUE",
        "typical_units": ["250ml", "300ml", "750ml"]
    },
    {
        "id": "wild_stone",
        "name": "Wild Stone Forest Spice Deodorant Soap 125g",
        "category": "Personal Care & Hygiene",
        "suggested_price": 70.0,
        "barcode": "8904006304218",
        "aliases": ["wild stone", "forest spice", "wild stone soap", "code platinum"],
        "color": "GREEN",
        "typical_units": ["125g", "150ml"]
    },
    {
        "id": "bourbon_biscuit",
        "name": "Britannia Bourbon Chocolate Biscuits",
        "category": "Snacks & Biscuits",
        "suggested_price": 30.0,
        "barcode": "8901063012014",
        "aliases": ["bourbon", "britannia bourbon", "choco bourbon", "bourbon biscuits"],
        "color": "ORANGE",
        "typical_units": ["150g"]
    },
    {
        "id": "milky_biscuit",
        "name": "Britannia Milk Bikis Biscuits",
        "category": "Snacks & Biscuits",
        "suggested_price": 20.0,
        "barcode": "8901063141011",
        "aliases": ["milk bikis", "britannia milk bikis", "bikis"],
        "color": "YELLOW",
        "typical_units": ["100g", "200g"]
    },
    {
        "id": "surf_excel",
        "name": "Surf Excel Easy Wash Detergent",
        "category": "Household & Laundry",
        "suggested_price": 120.0,
        "barcode": "8901030012015",
        "aliases": ["surf excel", "easy wash", "surf detergent"],
        "color": "BLUE",
        "typical_units": ["500g", "1kg"]
    },
    {
        "id": "hide_and_seek",
        "name": "Parle Hide & Seek Choco Chip Biscuits",
        "category": "Snacks & Biscuits",
        "suggested_price": 30.0,
        "barcode": "8901719101014",
        "aliases": ["hide and seek", "hide & seek", "choco chip biscuits", "parle hide seek"],
        "color": "ORANGE",
        "typical_units": ["100g", "200g"]
    },
    {
        "id": "appe_fizz",
        "name": "Appy Fizz Sparkling Apple Juice",
        "category": "Beverages & Drinks",
        "suggested_price": 35.0,
        "barcode": "8902579100018",
        "aliases": ["appy fizz", "appe fizz", "sparkling apple juice"],
        "color": "RED",
        "typical_units": ["160ml", "250ml", "600ml"]
    },
    {
        "id": "jim_jam",
        "name": "Britannia Treat Jim Jam Biscuits",
        "category": "Snacks & Biscuits",
        "suggested_price": 35.0,
        "barcode": "8901063015015",
        "aliases": ["jim jam", "jimjam", "treat jim jam", "naughty jam"],
        "color": "PURPLE",
        "typical_units": ["100g", "150g"]
    },
    {
        "id": "nivea_deodorant",
        "name": "Nivea Men Fresh Active Deodorant 150ml",
        "category": "Personal Care & Hygiene",
        "suggested_price": 199.0,
        "barcode": "4005808816033",
        "aliases": ["nivea men", "fresh active", "nivea deodorant", "nivea deo"],
        "color": "BLUE",
        "typical_units": ["150ml"]
    }
]

# ── 2. OCR Cleansing, Alias Mappings & Guardrails ───────────────────────────
FONT_ALIASES_MAP = {
    "ore0": "oreo", "0reo": "oreo", "oreq": "oreo", "orco": "oreo", "cakoy": "oreo", "qikany": "oreo",
    "naggi": "maggi", "maggl": "maggi", "meggi": "maggi", "mggi": "maggi", "2-minute": "maggi", "2 minute": "maggi", "masala maggi": "maggi",
    "layss": "lays chips", "lais": "lays chips", "layz": "lays chips", "lay's": "lays chips",
    "ashirvad": "aashirvaad atta", "asirvad": "aashirvaad atta",
    "hide 3seek": "hide and seek", "hde seek": "hide and seek", "hide & seek": "hide and seek",
    "jimjan": "jim jam", "jimiam": "jim jam", "jimyam": "jim jam", "jimjam": "jim jam", "naughty jam": "jim jam",
    "soya stica": "soya sticks", "soya stic": "soya sticks",
    "appe fizz": "appy fizz", "appe": "appy fizz",
    "bourbon": "bourbon", "burbon": "bourbon", "bourbonn": "bourbon", "borbon": "bourbon", "choco bourbon": "bourbon",
    "parle-g": "parle g", "parleg": "parle g",
    "goodday": "good day", "good-day": "good day",
    "kurkure": "kurkure", "kur kure": "kurkure",
    "dettol": "dettol", "detol": "dettol",
    "colgate": "colgate", "colgat": "colgate",
    "thumsup": "thums up", "thums-up": "thums up", "thum s up": "thums up",
    "cerave lotion": "cerave", "cerave cleanser": "cerave",
    "hns": "head & shoulders", "head and shoulders": "head & shoulders",
    "magg1": "maggi", "m4ggi": "maggi",
    "bourb0n": "bourbon"
}

NOISE_WORDS = {
    "net", "wt", "mfg", "exp", "batch", "pack", "ingredients", "made", "india",
    "mrp", "incl", "taxes", "tax", "customer", "care", "lic", "iso", "store",
    "cool", "dry", "place", "recyclable", "use", "best", "before", "date",
    "weight", "grams", "kilograms", "quantity", "address", "marketed",
    "manufactured", "ltd", "pvt", "corp", "inc", "product", "details", "contact",
    "nutrition", "nutritional", "facts", "information", "per", "serve", "serving",
    "size", "energy", "protein", "carbohydrate", "sugar", "fat", "saturated",
    "trans", "cholesterol", "sodium", "calcium", "iron", "vitamins", "minerals",
    "vegetarian", "veg", "green", "dot", "fssai", "license", "reg", "tm",
    "copyright", "all", "rights", "reserved", "keep", "away", "direct", "sunlight",
    "hygienic", "conditions", "dispose", "dustbin", "scan", "qr", "feedback",
    "helpline", "toll", "free", "email", "website", "www", "com", "in",
    "barcode", "dop", "pkd", "by", "months", "from", "packaging", "super",
    "saver", "offer", "inside", "new", "improved", "taste", "delicious",
    "crunchy", "crispy", "snack", "tasty", "yummy", "original", "formula",
    "imported", "distributed", "packed", "contains", "added", "flavour",
    "artificial", "natural", "identical", "flavouring", "substances", "preservative"
}

ANTI_CONFUSION_PAIRS = [
    ("maggi", "maaza"),
    ("maaza", "maggi"),
    ("maggi", "munch"),
    ("munch", "maggi"),
    ("soya sticks", "snickers"),
    ("snickers", "soya sticks"),
    ("colgate", "close up"),
    ("close up", "colgate"),
    ("surf excel", "soya sticks"),
    ("soya sticks", "surf excel")
]

# ── 3. Regex Parsers ─────────────────────────────────────────────────────────
PRICE_REGEX = re.compile(r'(?:₹|M\s*R\s*P|Rs\.?|INR)\s*[:\.]?\s*(?:₹|Rs\.?)?\s*(\d+(?:\.\d{1,2})?)', re.IGNORECASE)
QUANTITY_REGEX = re.compile(r'\b([0-9oO]+(?:\.[0-9oO]+)?\s*(?:kg|g|gm|l|ml|ltr|litre|pack|pc|pcs|pouch|sachet))\b', re.IGNORECASE)


# ── 4. Fuzzy & Distance Math ────────────────────────────────────────────────
def levenshtein_distance(s1: str, s2: str) -> int:
    len1, len2 = len(s1), len(s2)
    dp = [[0] * (len2 + 1) for _ in range(len1 + 1)]
    for i in range(len1 + 1):
        dp[i][0] = i
    for j in range(len2 + 1):
        dp[0][j] = j
    for i in range(1, len1 + 1):
        for j in range(1, len2 + 1):
            cost = 0 if s1[i - 1].lower() == s2[j - 1].lower() else 1
            dp[i][j] = min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
    return dp[len1][len2]


def char_similarity(s1: str, s2: str) -> float:
    max_len = max(len(s1), len(s2))
    if max_len == 0:
        return 1.0
    dist = levenshtein_distance(s1.lower(), s2.lower())
    return 1.0 - (dist / float(max_len))


def is_conflicting(scanned: str, target: str) -> bool:
    s_raw = scanned.lower()
    t_raw = target.lower()
    for w1, w2 in ANTI_CONFUSION_PAIRS:
        if w1 in s_raw and w2 in t_raw:
            return True
    return False


# ── 5. Main OCR Pipeline Processor ──────────────────────────────────────────
class OcrPipeline:
    def __init__(self, catalog: Optional[List[Dict[str, Any]]] = None):
        self.catalog = catalog or MASTER_CATALOG

    def clean_text_tokens(self, text: str) -> Tuple[List[str], str]:
        """Strips noise words and applies font alias mappings."""
        # Merge isolated single characters (e.g., M A G G 1 -> MAGG1)
        text = re.sub(r'(?<!\S)(\w)(?:\s+(\w))+(?!\S)', lambda m: m.group(0).replace(' ', ''), text)
        
        raw_tokens = re.split(r'[\s\-_\,\.\:\;\|]+', text.strip())
        cleaned = []
        for t in raw_tokens:
            token_clean = t.lower()
            if not token_clean or len(token_clean) < 2:
                continue
            if token_clean in NOISE_WORDS:
                continue
            # Apply alias replacement if found
            token_mapped = FONT_ALIASES_MAP.get(token_clean, token_clean)
            cleaned.append(token_mapped)

        normalized_string = " ".join(cleaned)
        # Check phrase aliases
        for phrase, replacement in FONT_ALIASES_MAP.items():
            if phrase in normalized_string:
                normalized_string = normalized_string.replace(phrase, replacement)

        return cleaned, normalized_string

    def extract_mrp(self, text: str) -> Optional[float]:
        match = PRICE_REGEX.search(text)
        if match:
            try:
                return float(match.group(1))
            except ValueError:
                return None
        return None

    def extract_unit(self, text: str) -> Optional[str]:
        match = QUANTITY_REGEX.search(text)
        if match:
            unit_str = match.group(1).upper().replace(" ", "")
            num_match = re.match(r'^[0-9O\.]+', unit_str)
            if num_match:
                num_part = num_match.group(0).replace('O', '0')
                rest_part = unit_str[len(num_match.group(0)):]
                return num_part + rest_part
            return unit_str
        return None

    def match_product(self, raw_text: str, detected_color: Optional[str] = None) -> Dict[str, Any]:
        """
        Executes full OCR pipeline:
        1. Parse MRP Price
        2. Parse Quantity Unit
        3. Clean tokens & resolve aliases
        4. Multi-level fuzzy matching against Master Catalog
        """
        mrp = self.extract_mrp(raw_text)
        unit = self.extract_unit(raw_text)
        tokens, cleaned_query = self.clean_text_tokens(raw_text)

        candidates = []

        for item in self.catalog:
            sku_name = item["name"].lower()
            sku_id = item["id"]
            sku_aliases = [a.lower() for a in item.get("aliases", [])]
            sku_color = item.get("color")

            # Check anti-confusion guards
            if is_conflicting(cleaned_query, sku_name):
                continue

            # Calculate similarity scores
            # 1. Alias exact/substring hit
            alias_hit = any(alias in cleaned_query for alias in sku_aliases)
            alias_score = 0.95 if alias_hit else 0.0

            # 2. Token overlap score
            target_tokens = set(re.split(r'[\s\-_\,\.\:\;]+', sku_name.lower()))
            overlap_count = sum(1 for t in tokens if t in target_tokens or any(char_similarity(t, tt) > 0.82 for tt in target_tokens))
            token_score = (overlap_count / max(len(target_tokens), 1)) if target_tokens else 0.0

            # 3. String Levenshtein score
            lev_score = char_similarity(cleaned_query, sku_name)

            # Combined match score
            base_score = max(alias_score, token_score * 0.8 + lev_score * 0.2)

            # Color verification boost/penalty
            color_bonus = 0.0
            if detected_color and sku_color:
                if detected_color.upper() == sku_color.upper():
                    color_bonus = 0.08
                else:
                    color_bonus = -0.15

            final_score = min(1.0, max(0.0, base_score + color_bonus))

            candidates.append({
                "item": item,
                "score": round(final_score, 3),
                "alias_hit": alias_hit
            })

        # Rank candidates
        candidates.sort(key=lambda x: x["score"], reverse=True)

        best = candidates[0] if candidates else None
        
        if best and best["score"] >= 0.40:
            if len(candidates) > 1:
                second = candidates[1]
                if second["score"] >= 0.40 and (best["score"] - second["score"]) < 0.10:
                    return {
                        "status": "AMBIGUOUS",
                        "extracted_price": mrp,
                        "extracted_unit": unit,
                        "cleaned_query": cleaned_query,
                        "top_candidate": best["item"]["name"],
                        "candidate_confidence": best["score"]
                    }

            matched_item = best["item"]
            return {
                "status": "MATCHED",
                "product_id": matched_item["id"],
                "product_name": matched_item["name"],
                "category": matched_item["category"],
                "catalog_price": matched_item["suggested_price"],
                "extracted_price": mrp,
                "extracted_unit": unit,
                "barcode": matched_item["barcode"],
                "confidence": best["score"],
                "cleaned_query": cleaned_query,
                "matched_color": matched_item.get("color")
            }
        else:
            return {
                "status": "UNRESOLVED",
                "extracted_price": mrp,
                "extracted_unit": unit,
                "cleaned_query": cleaned_query,
                "top_candidate": best["item"]["name"] if best else None,
                "candidate_confidence": best["score"] if best else 0.0
            }


# ── 6. CLI Execution & Interactive Shell ────────────────────────────────────
if __name__ == "__main__":
    pipeline = OcrPipeline()

    if len(sys.argv) > 1 and sys.argv[1] != "--interactive":
        sample_input = " ".join(sys.argv[1:])
        result = pipeline.match_product(sample_input)
        print(json.dumps(result, indent=2))
    else:
        print("=================================================================")
        print("  ScanSnap AI — Interactive OCR Intelligence Bench (Python)     ")
        print("  Type any packaging text (or 'quit' to exit).                   ")
        print("=================================================================")
        while True:
            try:
                line = input("\n[OCR Input] > ").strip()
                if not line or line.lower() in ("quit", "exit", "q"):
                    break
                res = pipeline.match_product(line)
                print(f"Status:      {res['status']}")
                if res['status'] == 'MATCHED':
                    print(f"Product:     {res['product_name']} ({res['category']})")
                    print(f"Confidence:  {res['confidence'] * 100:.1f}%")
                    print(f"Barcode:     {res['barcode']}")
                    print(f"Price:       ₹{res['extracted_price'] or res['catalog_price']}")
                    if res.get('extracted_unit'):
                        print(f"Unit:        {res['extracted_unit']}")
                else:
                    print(f"Best Guess:  {res.get('top_candidate')} ({res.get('candidate_confidence', 0)*100:.1f}%)")
                    print(f"Tokens:      {res.get('cleaned_query')}")
            except (KeyboardInterrupt, EOFError):
                break

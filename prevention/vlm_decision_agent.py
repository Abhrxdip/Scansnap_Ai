"""
ScanSnap AI — Pure Software Decision Agent & VLM Verifier
Decoupled from RabbitMQ, MinIO, and Intel OVMS.
Operates on standard Python runtimes with zero external infrastructure requirements.
Cross-validates visual detection candidate labels with live store inventory.
"""

from typing import Dict, List, Optional, Any, Tuple
import re


class SoftwareDecisionAgent:
    """
    Evaluates ambiguous or high-risk retail items against store catalog.
    Detects whether an unclassified item is a legitimate store product
    or an unscanned/swapped item.
    """

    def __init__(self, inventory_items: Optional[List[Dict[str, Any]]] = None):
        self.inventory_items = inventory_items or []
        self._build_index()

    def update_inventory(self, items: List[Dict[str, Any]]):
        self.inventory_items = items
        self._build_index()

    def _build_index(self):
        self.name_map = {}
        for item in self.inventory_items:
            name = item.get("name", "").strip().lower()
            if name:
                self.name_map[name] = item

    def validate_candidate(
        self,
        candidate_label: str,
        ocr_text: Optional[str] = None,
        confidence: float = 0.8
    ) -> Dict[str, Any]:
        """
        Validates visual candidate label against inventory index and OCR text tokens.
        Returns match status, standardized product details, and validation certainty.
        """
        norm_label = candidate_label.strip().lower()
        ocr_norm = (ocr_text or "").lower()

        # 1. Direct Catalog Match
        if norm_label in self.name_map:
            item = self.name_map[norm_label]
            return {
                "matched": True,
                "product": item,
                "confidence": confidence,
                "decision": "ACCEPT",
                "notes": "Exact match found in retailer catalog."
            }

        # 2. Substring & Keyword Search in Inventory
        best_match = None
        highest_score = 0
        tokens = set(re.findall(r"\w+", norm_label))
        if ocr_norm:
            tokens.update(re.findall(r"\w+", ocr_norm))

        for name, item in self.name_map.items():
            name_tokens = set(re.findall(r"\w+", name))
            common = tokens.intersection(name_tokens)
            if common:
                score = len(common) / max(1, len(name_tokens))
                if score > highest_score and score >= 0.35:
                    highest_score = score
                    best_match = item

        if best_match:
            return {
                "matched": True,
                "product": best_match,
                "confidence": max(confidence, highest_score),
                "decision": "ACCEPT",
                "notes": f"High semantic similarity match ({highest_score * 100:.1f}%) in catalog."
            }

        # 3. No match found in catalog -> Flag as potential uncatalogued or contraband item
        return {
            "matched": False,
            "product": {"name": candidate_label, "price": 0.0},
            "confidence": confidence,
            "decision": "FLAG_SUSPICIOUS",
            "notes": "Visual candidate not found in active store inventory."
        }

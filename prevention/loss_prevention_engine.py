"""
ScanSnap AI — Pure Software Retail Loss Prevention Engine
Decoupled from Intel-specific hardware (GStreamer, OpenVINO, NPU/iGPU, OVMS).
Operates purely in software on standard CPU/GPU using PyTorch, OpenCV, PIL,
and ScanSnap's feature matcher / YOLO models.

Detects the 7 Critical Retail Shrinkage Scenarios:
1. Product Switching (Ticket Switching / Barcode Swapping)
2. Fake Scan Detection (Ghost Scanning / Unscanned Passage)
3. Items Remaining in Basket (Cart Leftover / Unbagged Items)
4. Multi-Product Scan (Stacking / Double Loading)
5. Hidden Items Detection (Bottom of Basket / Obscured Containers)
6. Sweethearting Detection (Cashier Collusion / Barcode Bypass)
7. Age & Restricted Item Verification (Regulatory Compliance)
"""

import time
import math
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field
from datetime import datetime


@dataclass
class BoundingBox:
    x1: float
    y1: float
    x2: float
    y2: float
    confidence: float
    label: str

    @property
    def area(self) -> float:
        return max(0.0, self.x2 - self.x1) * max(0.0, self.y2 - self.y1)

    def iou(self, other: "BoundingBox") -> float:
        ix1 = max(self.x1, other.x1)
        iy1 = max(self.y1, other.y1)
        ix2 = min(self.x2, other.x2)
        iy2 = min(self.y2, other.y2)
        inter_area = max(0.0, ix2 - ix1) * max(0.0, iy2 - iy1)
        union_area = self.area + other.area - inter_area
        return inter_area / union_area if union_area > 0 else 0.0


@dataclass
class ScanEvent:
    event_id: str
    timestamp: float
    barcode: Optional[str] = None
    product_name: Optional[str] = None
    category: Optional[str] = None
    price: float = 0.0
    lane_id: str = "Lane-01"


@dataclass
class LossIncident:
    incident_id: str
    timestamp: str
    scenario_type: str
    risk_level: str  # "LOW", "MEDIUM", "HIGH", "CRITICAL"
    risk_score: float  # 0.0 - 100.0
    lane_id: str
    title: str
    description: str
    scanned_product: Optional[Dict[str, Any]] = None
    detected_product: Optional[Dict[str, Any]] = None
    price_discrepancy: float = 0.0
    recommended_action: str = "ALERT_STAFF"  # "ALERT_STAFF", "LOCK_LANE", "PROMPT_RESCAN", "REQUIRE_SUPERVISOR"
    metadata: Dict[str, Any] = field(default_factory=dict)
    resolved: bool = False
    resolution_note: Optional[str] = None


class SoftwareLossPreventionEngine:
    """
    Hardware-independent loss prevention rule & decision engine.
    Cross-references real-time vision detections against POS/Scan transaction state.
    """

    def __init__(self):
        self.active_incidents: List[LossIncident] = []
        self.incident_history: List[LossIncident] = []
        self.total_shrink_prevented: float = 0.0

    def evaluate_product_switching(
        self,
        scanned_sku: Optional[Dict[str, Any]],
        detected_sku: Optional[Dict[str, Any]],
        lane_id: str = "Lane-01"
    ) -> Optional[LossIncident]:
        """
        Scenario 1: Product Switching (Ticket Switching)
        Triggered when physical visual classifier does NOT match the scanned barcode line item,
        especially when a cheaper item barcode was scanned for a more expensive physical product.
        """
        if not scanned_sku or not detected_sku:
            return None

        scanned_label = (scanned_sku.get("name") or scanned_sku.get("label", "")).lower().strip()
        detected_label = (detected_sku.get("name") or detected_sku.get("label", "")).lower().strip()

        # Check for semantic / category match
        scanned_category = (scanned_sku.get("category") or "").lower().strip()
        detected_category = (detected_sku.get("category") or "").lower().strip()

        scanned_price = float(scanned_sku.get("price") or 0.0)
        detected_price = float(detected_sku.get("price") or 0.0)

        # Exact or close name match means genuine scan
        if scanned_label in detected_label or detected_label in scanned_label:
            return None

        # Discrepancy detected
        price_diff = max(0.0, detected_price - scanned_price)
        confidence = float(detected_sku.get("confidence") or 0.85)

        # Calculate Risk Score (0 - 100)
        risk_score = 50.0
        if price_diff > 500:
            risk_score += 40.0
        elif price_diff > 100:
            risk_score += 25.0
        elif price_diff > 0:
            risk_score += 15.0

        if scanned_category != detected_category:
            risk_score += 10.0

        risk_score = min(99.0, risk_score * confidence)

        risk_level = "CRITICAL" if risk_score >= 80 else ("HIGH" if risk_score >= 60 else "MEDIUM")

        incident = LossIncident(
            incident_id=f"INC-SW-{int(time.time() * 1000)}",
            timestamp=datetime.utcnow().isoformat(),
            scenario_type="product_switching",
            risk_level=risk_level,
            risk_score=round(risk_score, 1),
            lane_id=lane_id,
            title="Product Ticket Switching Detected",
            description=f"Barcode registered '{scanned_sku.get('name')}' (₹{scanned_price:.2f}), but camera detected '{detected_sku.get('name')}' (₹{detected_price:.2f}).",
            scanned_product=scanned_sku,
            detected_product=detected_sku,
            price_discrepancy=round(price_diff, 2),
            recommended_action="LOCK_LANE" if risk_level == "CRITICAL" else "ALERT_STAFF",
            metadata={"detection_confidence": confidence, "detected_category": detected_category}
        )
        self._record_incident(incident)
        return incident

    def evaluate_fake_scan(
        self,
        motion_in_scan_zone: bool,
        barcode_read_success: bool,
        item_placed_in_bag: bool,
        detected_item_name: str = "Unknown Item",
        estimated_price: float = 299.0,
        lane_id: str = "Lane-01"
    ) -> Optional[LossIncident]:
        """
        Scenario 2: Fake Scan Detection (Ghost Scan / Barcode Obscuration)
        Customer passes an item across the scanner window into bagging area without a successful barcode decode.
        """
        if motion_in_scan_zone and item_placed_in_bag and not barcode_read_success:
            incident = LossIncident(
                incident_id=f"INC-FS-{int(time.time() * 1000)}",
                timestamp=datetime.utcnow().isoformat(),
                scenario_type="fake_scan_detection",
                risk_level="HIGH",
                risk_score=88.5,
                lane_id=lane_id,
                title="Fake / Unscanned Motion Detected",
                description=f"Item '{detected_item_name}' was moved from scan area to bagging zone without a valid barcode read.",
                detected_product={"name": detected_item_name, "price": estimated_price},
                price_discrepancy=estimated_price,
                recommended_action="PROMPT_RESCAN",
                metadata={"motion_detected": True, "bag_weight_triggered": True, "barcode_event": False}
            )
            self._record_incident(incident)
            return incident
        return None

    def evaluate_items_in_basket(
        self,
        basket_item_count: int,
        checkout_initiated: bool,
        basket_items: Optional[List[Dict[str, Any]]] = None,
        lane_id: str = "Lane-01"
    ) -> Optional[LossIncident]:
        """
        Scenario 3: Items Remaining in Basket / Cart Leftover
        Customer attempts to complete payment while items are still present in cart/basket.
        """
        if checkout_initiated and basket_item_count > 0:
            total_val = sum(float(it.get("price", 150.0)) for it in (basket_items or []))
            incident = LossIncident(
                incident_id=f"INC-BK-{int(time.time() * 1000)}",
                timestamp=datetime.utcnow().isoformat(),
                scenario_type="items_in_basket",
                risk_level="HIGH",
                risk_score=85.0,
                lane_id=lane_id,
                title="Unscanned Items Left in Shopping Basket",
                description=f"Customer attempted checkout while {basket_item_count} items remain in cart/basket without scanning.",
                detected_product={"item_count": basket_item_count, "items": basket_items or []},
                price_discrepancy=round(total_val, 2),
                recommended_action="PROMPT_RESCAN",
                metadata={"unscanned_count": basket_item_count}
            )
            self._record_incident(incident)
            return incident
        return None

    def evaluate_multi_product_scan(
        self,
        detections_in_scan_zone: List[BoundingBox],
        barcodes_registered_in_window: int = 1,
        lane_id: str = "Lane-01"
    ) -> Optional[LossIncident]:
        """
        Scenario 4: Multi-Product Scan (Stacking / Double Loading)
        Multiple distinct objects detected in the scanner ROI during a single scan cycle.
        """
        if len(detections_in_scan_zone) > barcodes_registered_in_window:
            unscanned_items = len(detections_in_scan_zone) - barcodes_registered_in_window
            incident = LossIncident(
                incident_id=f"INC-MP-{int(time.time() * 1000)}",
                timestamp=datetime.utcnow().isoformat(),
                scenario_type="multi_product_identification",
                risk_level="MEDIUM",
                risk_score=72.0,
                lane_id=lane_id,
                title="Multiple Products in Single Scan Zone",
                description=f"Detected {len(detections_in_scan_zone)} distinct objects in scan zone, but only {barcodes_registered_in_window} barcode was scanned.",
                price_discrepancy=unscanned_items * 199.0,
                recommended_action="PROMPT_RESCAN",
                metadata={"detected_objects": len(detections_in_scan_zone), "scanned_barcodes": barcodes_registered_in_window}
            )
            self._record_incident(incident)
            return incident
        return None

    def evaluate_hidden_items(
        self,
        bottom_rack_detected: bool,
        concealed_container_item_count: int = 0,
        hidden_items: Optional[List[Dict[str, Any]]] = None,
        lane_id: str = "Lane-01"
    ) -> Optional[LossIncident]:
        """
        Scenario 5: Hidden Items Detection
        Merchandise concealed on cart lower tray, under bags, or nested inside containers.
        """
        if bottom_rack_detected or concealed_container_item_count > 0:
            total_val = sum(float(it.get("price", 350.0)) for it in (hidden_items or [])) or 350.0
            incident = LossIncident(
                incident_id=f"INC-HD-{int(time.time() * 1000)}",
                timestamp=datetime.utcnow().isoformat(),
                scenario_type="hidden_items",
                risk_level="HIGH",
                risk_score=91.0,
                lane_id=lane_id,
                title="Hidden / Bottom-Tray Item Detected",
                description="Vision model identified merchandise in the bottom cart tray or concealed inside another container.",
                price_discrepancy=round(total_val, 2),
                recommended_action="ALERT_STAFF",
                metadata={"hidden_items": hidden_items or []}
            )
            self._record_incident(incident)
            return incident
        return None

    def evaluate_sweethearting(
        self,
        cashier_scan_motion: bool,
        barcode_decoded: bool,
        item_passed_to_bag: bool,
        lane_id: str = "Lane-01"
    ) -> Optional[LossIncident]:
        """
        Scenario 6: Sweethearting Detection
        Operator or customer deliberately covers barcode with palm or bypasses scanner window.
        """
        if cashier_scan_motion and item_passed_to_bag and not barcode_decoded:
            incident = LossIncident(
                incident_id=f"INC-SH-{int(time.time() * 1000)}",
                timestamp=datetime.utcnow().isoformat(),
                scenario_type="sweethearting",
                risk_level="CRITICAL",
                risk_score=95.0,
                lane_id=lane_id,
                title="Sweethearting / Barcode Bypass Motion Detected",
                description="Item was passed across scan plane with barcode deliberately obscured or bypass motion detected.",
                price_discrepancy=499.0,
                recommended_action="REQUIRE_SUPERVISOR",
                metadata={"bypass_detected": True}
            )
            self._record_incident(incident)
            return incident
        return None

    def evaluate_age_verification(
        self,
        product_category: str,
        product_name: str,
        customer_verified: bool = False,
        lane_id: str = "Lane-01"
    ) -> Optional[LossIncident]:
        """
        Scenario 7: Age & Restricted Merchandise Verification
        Flags restricted categories (alcohol, tobacco, blades) for attendant sign-off.
        """
        restricted_keywords = ["beer", "wine", "alcohol", "vodka", "whiskey", "cigarette", "tobacco", "blade"]
        p_name_lower = product_name.lower()
        cat_lower = product_category.lower()

        is_restricted = any(kw in p_name_lower or kw in cat_lower for kw in restricted_keywords)
        if is_restricted and not customer_verified:
            incident = LossIncident(
                incident_id=f"INC-AV-{int(time.time() * 1000)}",
                timestamp=datetime.utcnow().isoformat(),
                scenario_type="age_verification",
                risk_level="HIGH",
                risk_score=80.0,
                lane_id=lane_id,
                title="Age-Restricted Item Requires Verification",
                description=f"Restricted item '{product_name}' requires 18+ ID validation before checkout approval.",
                detected_product={"name": product_name, "category": product_category},
                price_discrepancy=0.0,
                recommended_action="REQUIRE_SUPERVISOR",
                metadata={"restricted_item": True}
            )
            self._record_incident(incident)
            return incident
        return None

    def _record_incident(self, incident: LossIncident):
        self.active_incidents.insert(0, incident)
        self.incident_history.insert(0, incident)
        if incident.price_discrepancy > 0:
            self.total_shrink_prevented += incident.price_discrepancy

    def resolve_incident(self, incident_id: str, action: str, note: Optional[str] = None) -> bool:
        for inc in self.active_incidents:
            if inc.incident_id == incident_id:
                inc.resolved = True
                inc.resolution_note = f"{action}: {note or 'Resolved by cashier'}"
                self.active_incidents.remove(inc)
                return True
        return False

    def get_stats(self) -> Dict[str, Any]:
        return {
            "total_active_alerts": len(self.active_incidents),
            "total_historical_incidents": len(self.incident_history),
            "total_shrink_prevented_inr": round(self.total_shrink_prevented, 2),
            "scenarios_breakdown": {
                "product_switching": sum(1 for i in self.incident_history if i.scenario_type == "product_switching"),
                "fake_scan": sum(1 for i in self.incident_history if i.scenario_type == "fake_scan_detection"),
                "items_in_basket": sum(1 for i in self.incident_history if i.scenario_type == "items_in_basket"),
                "multi_product": sum(1 for i in self.incident_history if i.scenario_type == "multi_product_identification"),
                "hidden_items": sum(1 for i in self.incident_history if i.scenario_type == "hidden_items"),
                "sweethearting": sum(1 for i in self.incident_history if i.scenario_type == "sweethearting"),
                "age_verification": sum(1 for i in self.incident_history if i.scenario_type == "age_verification"),
            }
        }

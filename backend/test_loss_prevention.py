"""
Comprehensive Automated Test Suite for ScanSnap AI Pure-Software Loss Prevention
Tests all 7 shrinkage scenarios, the VLM decision agent, database persistence,
FastAPI REST endpoints, and instant-find integration.
"""

import sys
import unittest
from pathlib import Path
from fastapi.testclient import TestClient

BACKEND_DIR = Path(__file__).resolve().parent
PROJECT_DIR = BACKEND_DIR.parent
PREVENTION_DIR = PROJECT_DIR / "prevention"

sys.path.insert(0, str(BACKEND_DIR))
sys.path.insert(0, str(PREVENTION_DIR))

from main import app
from services import loss_prevention_service
from loss_prevention_engine import SoftwareLossPreventionEngine, BoundingBox
from vlm_decision_agent import SoftwareDecisionAgent


class TestLossPreventionSoftwareEngine(unittest.TestCase):
    def setUp(self):
        self.engine = SoftwareLossPreventionEngine()

    def test_product_switching_detection(self):
        """Test detection of cheap barcode scanned on costly item (Wild Stone on Puma)."""
        scanned_sku = {"barcode": "8901030383418", "name": "Wild Stone Soap", "price": 40.0, "category": "Personal Care"}
        detected_sku = {"name": "Puma Regular Fit T-Shirt", "price": 1299.0, "category": "Fashion", "confidence": 0.95}

        incident = self.engine.evaluate_product_switching(scanned_sku, detected_sku)
        self.assertIsNotNone(incident)
        self.assertEqual(incident.scenario_type, "product_switching")
        self.assertEqual(incident.risk_level, "CRITICAL")
        self.assertGreater(incident.risk_score, 80.0)
        self.assertEqual(incident.price_discrepancy, 1259.0)

    def test_genuine_scan_no_switching(self):
        """Test genuine scan does NOT trigger product switching."""
        scanned_sku = {"barcode": "8901030383456", "name": "Puma Regular Fit T-Shirt", "price": 1299.0, "category": "Fashion"}
        detected_sku = {"name": "Puma Regular Fit T-Shirt", "price": 1299.0, "category": "Fashion", "confidence": 0.95}

        incident = self.engine.evaluate_product_switching(scanned_sku, detected_sku)
        self.assertIsNone(incident)

    def test_fake_scan_detection(self):
        """Test fake scan where motion occurs with 0 barcode decode."""
        incident = self.engine.evaluate_fake_scan(
            motion_in_scan_zone=True,
            barcode_read_success=False,
            item_placed_in_bag=True,
            detected_item_name="boAt Rockerz 450",
            estimated_price=1499.0
        )
        self.assertIsNotNone(incident)
        self.assertEqual(incident.scenario_type, "fake_scan_detection")
        self.assertEqual(incident.risk_level, "HIGH")
        self.assertEqual(incident.price_discrepancy, 1499.0)

    def test_items_in_basket_leftover(self):
        """Test detection of items remaining in shopping cart during checkout."""
        incident = self.engine.evaluate_items_in_basket(
            basket_item_count=2,
            checkout_initiated=True,
            basket_items=[{"name": "Amul Ice Cream", "price": 30.0}, {"name": "Britannia Cake", "price": 25.0}]
        )
        self.assertIsNotNone(incident)
        self.assertEqual(incident.scenario_type, "items_in_basket")
        self.assertEqual(incident.price_discrepancy, 55.0)

    def test_multi_product_stacking(self):
        """Test multi-product detection when 2 items are stacked during 1 scan."""
        boxes = [
            BoundingBox(0.1, 0.1, 0.4, 0.4, 0.9, "Puma T-Shirt"),
            BoundingBox(0.45, 0.15, 0.85, 0.6, 0.85, "Wild Stone Soap")
        ]
        incident = self.engine.evaluate_multi_product_scan(boxes, barcodes_registered_in_window=1)
        self.assertIsNotNone(incident)
        self.assertEqual(incident.scenario_type, "multi_product_identification")

    def test_hidden_items(self):
        """Test concealed items detection in bottom cart tray."""
        incident = self.engine.evaluate_hidden_items(
            bottom_rack_detected=True,
            hidden_items=[{"name": "Nike Revolution 6", "price": 2499.0}]
        )
        self.assertIsNotNone(incident)
        self.assertEqual(incident.scenario_type, "hidden_items")
        self.assertEqual(incident.price_discrepancy, 2499.0)

    def test_sweethearting_detection(self):
        """Test sweethearting / cashier barcode bypass."""
        incident = self.engine.evaluate_sweethearting(
            cashier_scan_motion=True,
            barcode_decoded=False,
            item_passed_to_bag=True
        )
        self.assertIsNotNone(incident)
        self.assertEqual(incident.scenario_type, "sweethearting")
        self.assertEqual(incident.risk_level, "CRITICAL")

    def test_age_verification_compliance(self):
        """Test age-restricted item detection."""
        incident = self.engine.evaluate_age_verification(
            product_category="Beverages / Alcohol",
            product_name="Kingfisher Premium Beer 650ml",
            customer_verified=False
        )
        self.assertIsNotNone(incident)
        self.assertEqual(incident.scenario_type, "age_verification")


class TestVLMDecisionAgent(unittest.TestCase):
    def test_catalog_matching_and_flagging(self):
        inventory = [
            {"id": "p1", "name": "Puma Classic Crewneck T-Shirt", "price": 1299.0},
            {"id": "p2", "name": "Wild Stone Forest Spice Deodorant Soap", "price": 40.0}
        ]
        agent = SoftwareDecisionAgent(inventory)

        # Match exact/substring
        res = agent.validate_candidate("Puma Classic Crewneck T-Shirt")
        self.assertTrue(res["matched"])
        self.assertEqual(res["decision"], "ACCEPT")

        # Flag unknown/contraband
        res2 = agent.validate_candidate("Contraband Uncatalogued Item XYZ")
        self.assertFalse(res2["matched"])
        self.assertEqual(res2["decision"], "FLAG_SUSPICIOUS")


class TestLossPreventionRestApi(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_stats_endpoint(self):
        resp = self.client.get("/loss-prevention/stats")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertIn("total_shrink_prevented_inr", data)
        self.assertIn("system_status", data)
        self.assertEqual(data["system_status"], "ONLINE")

    def test_scenarios_list(self):
        resp = self.client.get("/loss-prevention/scenarios")
        self.assertEqual(resp.status_code, 200)
        scenarios = resp.json()
        self.assertGreaterEqual(len(scenarios), 7)
        ids = [s["id"] for s in scenarios]
        self.assertIn("product_switching", ids)
        self.assertIn("fake_scan", ids)

    def test_simulate_product_switching(self):
        resp = self.client.post("/loss-prevention/simulate/product_switching")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["status"], "success")
        self.assertIsNotNone(data["incident"])
        self.assertEqual(data["incident"]["scenario_type"], "product_switching")
        self.assertEqual(data["incident"]["risk_level"], "CRITICAL")
        self.assertGreater(data["incident"]["price_discrepancy"], 1000.0)

    def test_simulate_fake_scan(self):
        resp = self.client.post("/loss-prevention/simulate/fake_scan")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["status"], "success")
        self.assertEqual(data["incident"]["scenario_type"], "fake_scan_detection")

    def test_incidents_list(self):
        resp = self.client.get("/loss-prevention/incidents")
        self.assertEqual(resp.status_code, 200)
        self.assertIsInstance(resp.json(), list)

    def test_instant_find_with_loss_prevention_alert(self):
        """Test instant-find triggers loss_prevention_alert when scanned barcode is cheap soap but visual is Puma."""
        wild_stone_barcode = "8901030383418"  # Wild stone soap
        resp = self.client.post(
            "/detect/instant-find",
            data={
                "query": "Puma Regular Fit T-Shirt",
                "scanned_barcode": wild_stone_barcode
            }
        )
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertIsNotNone(data.get("loss_prevention_alert"))
        alert = data["loss_prevention_alert"]
        self.assertTrue(alert["alert"])
        self.assertEqual(alert["scenario_type"], "product_switching")


if __name__ == "__main__":
    unittest.main()

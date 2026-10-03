# ScanSnap AI — Retail Loss Prevention & Shrinkage Shield (Pure Software)

## Overview
This module implements the **Loss Prevention & Theft Detection Suite** for ScanSnap AI.

**Hardware Decoupling:**
The original Intel hardware-specific dependencies (Intel GStreamer plugins `vaapi`, Intel OpenVINO Model Server `OVMS`, Intel NPU/iGPU sizing benchmarks, RabbitMQ/MinIO cluster requirements) have been **completely decoupled and removed**. 

The software logic now runs entirely on **standard Python runtime** (CPU, GPU, or mobile edge) seamlessly integrated with:
1. **ScanSnap AI Retail Classifier (`retail_classifier.py`)** — ORB invariant keypoint packshot matcher.
2. **YOLO Model (`models/best.pt`)** — Real-time bounding box and multi-class item detector.
3. **ScanSnap Inventory & Cart Engine** — Real-time POS reconciliation against SQLite database (`smartvendor.db`).

---

## The 7 Loss Prevention Software Scenarios

| Scenario ID | Name | Threat Model | Automated Action |
| :--- | :--- | :--- | :--- |
| `product_switching` | **Product Ticket Switching** | Shopper scans cheap barcode (₹40 soap) on costly item (₹1,299 Puma T-Shirt or ₹899 Beardo). | **CRITICAL ALERT** & Auto-lock lane |
| `fake_scan` | **Fake / Ghost Scan** | Item passed across scanner into bag without valid barcode trigger. | Prompt Rescan / Visual verify |
| `items_in_basket` | **Unscanned Items in Cart** | Payment triggered while items remain in bottom basket. | Prompt Rescan before charge |
| `multi_product` | **Multi-Product Stacking** | Multiple items in scan area during single barcode scan. | Split item scan warning |
| `hidden_items` | **Hidden / Bottom-Tray Item** | Merchandise detected in bottom rack or concealed inside containers. | Attendant notification |
| `sweethearting` | **Sweethearting / Bypass** | Hand motion obscuring barcode window or bypass pass. | Attendant intervention |
| `age_verification` | **Age-Restricted Compliance** | 18+ merchandise (alcohol, tobacco, blades) detected. | Supervisor ID sign-off |

---

## Module Layout
- `loss_prevention_engine.py`: Core rule engine for the 7 scenarios with risk scoring (0-100) and incident tracking.
- `vlm_decision_agent.py`: Pure software decision agent that cross-validates visual detection candidate labels with live store inventory.
- `scenarios.py`: Pre-configured demo test cases for judge presentations.
- Integrated into backend at `backend/routers/loss_prevention.py` and `backend/services/loss_prevention_service.py`.

"""
ScanSnap AI — Loss Prevention & Shrinkage Shield Router
Exposes REST endpoints for real-time shrinkage detection, scenario simulation,
and loss-prevention alert resolution.
"""

from typing import Optional, List, Dict, Any
from fastapi import APIRouter, HTTPException, Query, Body, Form, UploadFile, File
from pydantic import BaseModel

from services import loss_prevention_service

router = APIRouter(prefix="/loss-prevention", tags=["Loss Prevention & Shrink Shield"])


class EvaluateScanRequest(BaseModel):
    scanned_barcode: Optional[str] = None
    detected_product_name: Optional[str] = None
    detected_category: Optional[str] = None
    detected_price: Optional[float] = None
    detected_confidence: float = 0.85
    lane_id: str = "Lane-01"


class ResolveIncidentRequest(BaseModel):
    action: str  # "confirm" or "clear"
    note: Optional[str] = None


@router.get("/stats")
def get_loss_prevention_stats():
    """Returns real-time KPIs, total shrink value prevented, and detection health."""
    return loss_prevention_service.get_loss_prevention_kpis()


@router.get("/scenarios")
def list_available_scenarios():
    """Lists pre-configured retail loss-prevention demonstration scenarios."""
    from scenarios import DEMO_SCENARIOS
    return [
        {
            "id": s["id"],
            "name": s["name"],
            "category": s["category"],
            "description": s["description"],
            "expected_risk": s["expected_risk"]
        }
        for s in DEMO_SCENARIOS.values()
    ]


@router.get("/incidents")
def get_incidents(limit: int = Query(default=50, ge=1, le=200)):
    """Returns recent loss-prevention incidents from database."""
    return loss_prevention_service.list_incidents(limit=limit)


@router.post("/simulate/{scenario_id}")
def simulate_scenario(scenario_id: str, lane_id: str = Query(default="Lane-01")):
    """
    Executes a high-fidelity loss prevention simulation using realistic demo data.
    Available scenarios:
    - product_switching
    - fake_scan
    - items_in_basket
    - multi_product
    - hidden_items
    - sweethearting
    - age_verification
    """
    try:
        return loss_prevention_service.run_scenario_simulation(scenario_id=scenario_id, lane_id=lane_id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Simulation error: {str(e)}")


@router.post("/evaluate")
def evaluate_scan(request: EvaluateScanRequest):
    """
    Evaluates a live scan event in real-time.
    Compares the scanned barcode against the computer-vision detected item.
    """
    alert = loss_prevention_service.evaluate_scan_for_shrinkage(
        scanned_barcode=request.scanned_barcode,
        detected_product_name=request.detected_product_name,
        detected_category=request.detected_category,
        detected_price=request.detected_price,
        detected_confidence=request.detected_confidence,
        lane_id=request.lane_id
    )
    if alert:
        return {"status": "alert_triggered", "alert": alert}
    return {"status": "clean", "message": "Scan verified. No shrinkage detected."}


@router.post("/resolve/{incident_id}")
def resolve_incident(incident_id: str, request: ResolveIncidentRequest):
    """Resolves an incident as either confirmed theft or cleared by cashier/supervisor."""
    success = loss_prevention_service.resolve_incident(incident_id, action=request.action, note=request.note)
    if not success:
        raise HTTPException(status_code=404, detail="Incident not found")
    return {"status": "success", "incident_id": incident_id, "resolution": request.action}

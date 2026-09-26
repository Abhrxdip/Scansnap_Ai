"""
YOLO Object Detection Router
Runs best.pt against a camera frame image sent from the Android app.
Returns detected product labels + confidence scores.
"""
import io
import base64
import logging
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, File, UploadFile, HTTPException, Form
from fastapi.responses import JSONResponse
from PIL import Image
import numpy as np

router = APIRouter(prefix="/detect", tags=["YOLO Detection"])
logger = logging.getLogger(__name__)

# ── Load YOLO model once at startup ──────────────────────────────────────────
MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "best.pt"
_yolo_model = None


def _get_model():
    global _yolo_model
    if _yolo_model is None:
        try:
            from ultralytics import YOLO
            logger.info(f"Loading YOLO model from {MODEL_PATH}")
            _yolo_model = YOLO(str(MODEL_PATH))
            logger.info(f"YOLO model loaded. Classes: {_yolo_model.names}")
        except Exception as e:
            logger.error(f"Failed to load YOLO model: {e}")
            raise RuntimeError(f"YOLO model could not be loaded: {e}")
    return _yolo_model


# ── Schemas ───────────────────────────────────────────────────────────────────
from pydantic import BaseModel


class Detection(BaseModel):
    label: str
    class_name: Optional[str] = None
    confidence: float
    bbox: list[float]  # [x1, y1, x2, y2] normalised 0-1
    box: Optional[list[int]] = None  # [x1, y1, x2, y2] pixel coordinates
    product_match: Optional[dict] = None


class DetectResponse(BaseModel):
    detections: list[Detection]
    top_label: Optional[str] = None
    top_confidence: Optional[float] = None


FRIENDLY_NAMES = {
    "amul_ice_cream": "Amul Ice Cream Cup Vanilla Magic",
    "cake": "Britannia Treat Chocolate Cake",
    "cerave": "CeraVe Daily Moisturizing Lotion",
    "hns_shampoo": "Head & Shoulders Cool Menthol Shampoo",
    "nestle_milk_powder": "Nestle Everyday Dairy Whitener",
    "plum": "Plum Green Tea Face Wash",
    "thums_up": "Thums Up Charged Carbonated Drink",
    "wild_stone": "Wild Stone Code Platinum Deodorant",
    "nivea_deodorant": "Nivea Men Fresh Active Deodorant",
    "bourbon_biscuit": "Britannia Bourbon Chocolate Biscuits",
    "milky_biscuit": "Britannia Milk Bikis Biscuits",
    "maggi": "Maggi 2-Minute Masala Noodles",
    "surf_excel": "Surf Excel Easy Wash Detergent",
    "hide_and_seek": "Parle Hide & Seek Choco Chip Biscuits",
    "oreo": "Cadbury Oreo Original Biscuits",
    "appe_fizz": "Appy Fizz Sparkling Apple Juice",
    "jim_jam": "Britannia Treat Jim Jam Biscuits"
}


def _match_db_product(label: str) -> Optional[dict]:
    try:
        from database import SessionLocal
        import models
        db = SessionLocal()
        try:
            keywords_map = {
                "amul_ice_cream": ["Amul", "Ice Cream"],
                "cake": ["Cake"],
                "cerave": ["CeraVe"],
                "hns_shampoo": ["Head & Shoulders", "Shampoo"],
                "nestle_milk_powder": ["Nestle", "Milk Powder"],
                "plum": ["Plum"],
                "thums_up": ["Thums Up"],
                "wild_stone": ["Wild Stone"],
                "nivea_deodorant": ["Nivea"],
                "bourbon_biscuit": ["Bourbon"],
                "milky_biscuit": ["Milk"],
                "maggi": ["Maggi"],
                "surf_excel": ["Surf"],
                "hide_and_seek": ["Hide"],
                "oreo": ["Oreo"],
                "appe_fizz": ["Appy"],
                "jim_jam": ["Jim"]
            }
            kws = keywords_map.get(label.lower().strip(), [label])
            query = db.query(models.Product)
            for kw in kws:
                query = query.filter(models.Product.name.ilike(f"%{kw}%"))
            prod = query.first()
            if prod:
                return {
                    "id": prod.id,
                    "name": prod.name,
                    "price": prod.price,
                    "stock": prod.stock,
                    "category": prod.category,
                    "barcode": prod.barcode
                }
            return None
        finally:
            db.close()
    except Exception as e:
        logger.warning(f"Product DB lookup failed: {e}")
        return None


# ── Helper ────────────────────────────────────────────────────────────────────
def _verify_color_signature(crop_img: Image.Image, label: str) -> bool:
    """
    Sub-millisecond (<0.5ms) physical packaging color profile validator.
    Eliminates out-of-domain false positives using HSV color distributions.
    """
    try:
        small = crop_img.resize((64, 64)).convert("HSV")
        hsv_np = np.array(small)
        h = hsv_np[:, :, 0]
        s = hsv_np[:, :, 1]
        v = hsv_np[:, :, 2]

        saturated = (s > 40) & (v > 50)
        if not np.any(saturated):
            return True

        h_deg = (h[saturated] / 255.0) * 360.0
        total = len(h_deg)
        if total == 0:
            return True

        yellow_pct = (np.sum((h_deg >= 35) & (h_deg <= 75)) / total) * 100
        green_pct = (np.sum((h_deg >= 80) & (h_deg <= 165)) / total) * 100
        blue_pct = (np.sum((h_deg >= 170) & (h_deg <= 260)) / total) * 100
        red_pct = (np.sum((h_deg <= 25) | (h_deg >= 335)) / total) * 100
        purple_pct = (np.sum((h_deg > 260) & (h_deg < 335)) / total) * 100

        lbl = label.lower().strip()

        # Existing Kirana items
        if lbl == "maggi":
            if yellow_pct < 20.0 or green_pct > 25.0:
                return False
        elif lbl in ("surf_excel", "surf"):
            if blue_pct < 12.0 or yellow_pct > 30.0:
                return False
        elif lbl in ("bourbon", "bourbon_biscuit"):
            if blue_pct > 20.0 or green_pct > 20.0 or yellow_pct > 35.0:
                return False
        elif lbl == "oreo":
            if green_pct > 25.0 or yellow_pct > 30.0:
                return False
        elif lbl in ("appe_fizz", "appy_fizz", "appe", "appy"):
            if green_pct > 25.0 or blue_pct > 25.0:
                return False
        elif lbl in ("hide_and_seek", "hide_seek"):
            if green_pct > 25.0 or yellow_pct > 30.0:
                return False
        elif lbl in ("jim_jam", "jimjam"):
            if green_pct > 25.0 or blue_pct > 25.0:
                return False
        elif lbl in ("nivea_deodorant", "nivea"):
            if green_pct > 25.0 or yellow_pct > 30.0:
                return False

        # 8 New Retail Dataset items
        elif lbl == "amul_ice_cream":
            # Creamy / pale / red / blue accents. Rejects bright lime green dominance.
            if green_pct > 35.0:
                return False
        elif lbl == "cake":
            # Britannia Cake: chocolate / warm bakery hues. Rejects cyan/bright blue dominance.
            if blue_pct > 30.0:
                return False
        elif lbl == "cerave":
            # CeraVe: Clean white / cyan-blue clinical packaging. Rejects bright yellow or dark green dominance.
            if yellow_pct > 35.0 or green_pct > 35.0:
                return False
        elif lbl == "hns_shampoo":
            # Head & Shoulders: Distinctive crisp white bottle with royal blue cap.
            # Rejects dominant orange / yellow.
            if yellow_pct > 35.0:
                return False
        elif lbl == "nestle_milk_powder":
            # Nestle Everyday: Vibrant sunny yellow & sky blue packaging.
            # Reject if completely missing yellow/blue and dominated by pure red/purple.
            if purple_pct > 30.0:
                return False
        elif lbl == "plum":
            # Plum: Plum/purple & herbal green tones. Rejects cyan/bright blue dominance.
            if blue_pct > 30.0:
                return False
        elif lbl == "thums_up":
            # Thums Up: High-contrast red and deep navy blue. Rejects bright yellow/green dominance.
            if yellow_pct > 30.0 or green_pct > 25.0:
                return False
        elif lbl == "wild_stone":
            # Wild Stone product line includes Code Platinum (charcoal/silver), Forest Spice Soap (emerald green/black), etc.
            # Rejects bright neon purple/magenta dominance
            if purple_pct > 35.0:
                return False

        return True
    except Exception:
        return True


def _run_inference(img: Image.Image, conf_threshold: float = 0.25) -> DetectResponse:
    model = _get_model()
    rgb_img = img.convert("RGB")
    w, h = rgb_img.size

    # Run inference with class-specific NMS (agnostic_nms=False) so different adjacent/touching products are both detected
    results = model.predict(
        source=rgb_img,
        conf=conf_threshold,
        iou=0.45,
        agnostic_nms=False,
        verbose=False
    )

    detections: list[Detection] = []

    for result in results:
        for box in result.boxes:
            cls_id = int(box.cls[0])
            label = model.names[cls_id]
            conf = float(box.conf[0])
            x1, y1, x2, y2 = box.xyxy[0].tolist()

            logger.info(f"🎯 [YOLO Model Box] Found {label} ({conf*100:.1f}%)")
            print(f"🎯 [YOLO Model Box] Found {label} ({conf*100:.1f}%)")

            # Crop detection region
            crop_box = (max(0, int(x1)), max(0, int(y1)), min(w, int(x2)), min(h, int(y2)))
            crop = rgb_img.crop(crop_box)

            # Feature Packshot Descriptor Verification Guard:
            # Only run guard on borderline/low confidence detections (<0.60) to avoid rejecting genuine multi-object detections
            if conf < 0.60:
                from retail_classifier import verify_crop_matches_label
                if not verify_crop_matches_label(crop, label, min_matches=25):
                    logger.info(f"🛡️ [Verification Guard] Rejected out-of-domain candidate: {label} ({conf*100:.1f}%) on packaging descriptor mismatch")
                    print(f"🛡️ [Verification Guard] Rejected out-of-domain candidate: {label} ({conf*100:.1f}%) on packaging descriptor mismatch")
                    continue

                # Fast Color Signature Guard (<0.5ms) - only run on borderline detections (<0.60)
                if not _verify_color_signature(crop, label):
                    logger.info(f"[Color Guard] Rejected: {label} ({conf*100:.1f}%) on non-matching package color")
                    continue

            # Lookup catalog product match
            prod_match = _match_db_product(label)
            class_display_name = FRIENDLY_NAMES.get(label, label.replace("_", " ").title())

            detections.append(Detection(
                label=label,
                class_name=class_display_name,
                confidence=round(conf, 4),
                bbox=[round(x1 / w, 4), round(y1 / h, 4),
                      round(x2 / w, 4), round(y2 / h, 4)],
                box=[int(x1), int(y1), int(x2), int(y2)],
                product_match=prod_match
            ))

    # If YOLO produced no detections, apply fast visual package classifier
    if not detections:
        try:
            from retail_classifier import classify_product_image
            match_res = classify_product_image(rgb_img)
            if match_res:
                lbl, conf, bbox = match_res
                x1 = int(bbox[0] * w)
                y1 = int(bbox[1] * h)
                x2 = int(bbox[2] * w)
                y2 = int(bbox[3] * h)
                prod_match = _match_db_product(lbl)
                class_display_name = FRIENDLY_NAMES.get(lbl, lbl.replace("_", " ").title())
                detections.append(Detection(
                    label=lbl,
                    class_name=class_display_name,
                    confidence=conf,
                    bbox=[bbox[0], bbox[1], bbox[2], bbox[3]],
                    box=[x1, y1, x2, y2],
                    product_match=prod_match
                ))
                logger.info(f"🎯 [Retail Classifier Match] Found {lbl} ({conf*100:.1f}%)")
        except Exception as e:
            logger.warning(f"Retail classifier fallback error: {e}")

    # Sort by confidence descending
    detections.sort(key=lambda d: d.confidence, reverse=True)

    if detections:
        det_summary = ", ".join(f"{d.label} ({int(d.confidence*100)}%)" for d in detections)
        logger.info(f"[YOLO Inference] Found {len(detections)} product(s): {det_summary}")
        print(f"🎯 [YOLO] Detections: {det_summary}")

    top = detections[0] if detections else None
    return DetectResponse(
        detections=detections,
        top_label=top.label if top else None,
        top_confidence=top.confidence if top else None,
    )




# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/image", response_model=DetectResponse, summary="Detect products in an uploaded image")
async def detect_from_upload(
    file: UploadFile = File(...),
    conf: float = Form(default=0.25)
):
    """
    Accept a JPEG/PNG camera frame and return YOLO detections.
    The Android app sends a camera preview frame here.
    """
    try:
        contents = await file.read()
        img = Image.open(io.BytesIO(contents))
        return _run_inference(img, conf_threshold=conf)
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        logger.error(f"Detection error: {e}")
        raise HTTPException(status_code=400, detail=f"Invalid image: {e}")


@router.post("/base64", response_model=DetectResponse, summary="Detect products from a base64 image")
async def detect_from_base64(payload: dict):
    """
    Accept JSON with { "image": "<base64>", "conf": 0.65 } and return detections.
    Useful for the Android CameraX analysis use-case.
    """
    try:
        b64 = payload.get("image", "")
        conf = float(payload.get("conf", 0.35))
        img_bytes = base64.b64decode(b64)
        img = Image.open(io.BytesIO(img_bytes))
        return _run_inference(img, conf_threshold=conf)
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        logger.error(f"Detection base64 error: {e}")
        raise HTTPException(status_code=400, detail=f"Invalid payload: {e}")


@router.get("/classes", summary="List classes the YOLO model can detect")
async def get_classes():
    """Returns the product classes the trained model knows."""
    try:
        model = _get_model()
        return {"classes": model.names, "num_classes": len(model.names)}
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))

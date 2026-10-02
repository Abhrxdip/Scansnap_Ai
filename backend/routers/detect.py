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
from PIL import Image, ImageOps
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
from auth import CurrentUser


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
    "amul_ice_cream": "Amul Ice Cream Cup Vanilla Magic 100ml",
    "cake": "Britannia Cake Gobbles Choco Chill 65g",
    "cerave": "CeraVe Hydrating Cleanser 236ml",
    "hns_shampoo": "Head & Shoulders Cool Menthol Anti-Dandruff Shampoo 180ml",
    "nestle_milk_powder": "Nestle Everyday Dairy Whitener Milk Powder 20g",
    "plum": "Plum Green Tea Pore Cleansing Face Wash 100ml",
    "thums_up": "Thums Up Charged Carbonated Beverage 250ml Can",
    "wild_stone": "Wild Stone Forest Spice Deodorant Soap 125g",
    "nivea_deodorant": "Nivea Men Fresh Active Deodorant 150ml",
    "bourbon_biscuit": "Britannia Bourbon Chocolate Biscuits",
    "milky_biscuit": "Britannia Milk Bikis Biscuits",
    "maggi": "Maggi 2-Minute Masala Noodles",
    "surf_excel": "Surf Excel Easy Wash Detergent",
    "hide_and_seek": "Parle Hide & Seek Choco Chip Biscuits",
    "oreo": "Cadbury Oreo Original Biscuits",
    "appe_fizz": "Appy Fizz Sparkling Apple Juice",
    "jim_jam": "Britannia Treat Jim Jam Biscuits"
}


def _match_db_product(label: str, user_id: str) -> Optional[dict]:
    try:
        from database import SessionLocal
        import models
        db = SessionLocal()
        try:
            normalized_label = label.lower().strip()
            
            def format_prod(p):
                return {
                    "id": p.id,
                    "name": p.name,
                    "price": p.price,
                    "stock": p.stock,
                    "category": p.category,
                    "barcode": p.barcode
                }

            explicit_name = FRIENDLY_NAMES.get(normalized_label)
            normalized_name_str = normalized_label.replace("_", " ")

            keywords_map = {
                "amul_ice_cream": ["Amul", "Ice Cream"],
                "cake": ["Britannia", "Cake"],
                "cerave": ["CeraVe"],
                "hns_shampoo": ["Head & Shoulders"],
                "nestle_milk_powder": ["Nestle", "Everyday"],
                "plum": ["Plum"],
                "thums_up": ["Thums Up"],
                "wild_stone": ["Wild Stone"],
                "nivea_deodorant": ["Nivea", "Deodorant"],
                "bourbon_biscuit": ["Bourbon"],
                "milky_biscuit": ["Milk", "Bikis"],
                "maggi": ["Maggi"],
                "surf_excel": ["Surf", "Excel"],
                "hide_and_seek": ["Hide", "Seek"],
                "oreo": ["Oreo"],
                "appe_fizz": ["Appy"],
                "jim_jam": ["Jim", "Jam"]
            }
            kws = keywords_map.get(normalized_label, [normalized_name_str])

            # 1. Search in user's shelf (or demo_user fallback)
            target_user_ids = [user_id]
            if user_id != "demo_user":
                target_user_ids.append("demo_user")

            for uid in target_user_ids:
                if explicit_name:
                    prod = db.query(models.Product).filter(
                        models.Product.name.ilike(f"%{explicit_name}%"),
                        models.Product.user_id == uid
                    ).first()
                    if prod:
                        return format_prod(prod)

                prod = db.query(models.Product).filter(
                    models.Product.name.ilike(f"%{normalized_name_str}%"),
                    models.Product.user_id == uid
                ).first()
                if prod:
                    return format_prod(prod)

                query = db.query(models.Product).filter(models.Product.user_id == uid)
                for kw in kws:
                    query = query.filter(models.Product.name.ilike(f"%{kw}%"))
                prod = query.first()
                if prod:
                    return format_prod(prod)

            # Fallback for Nivea brand specifically
            if normalized_label == "nivea_deodorant":
                for uid in target_user_ids:
                    nivea_prod = db.query(models.Product).filter(
                        models.Product.name.ilike("%Nivea%"),
                        models.Product.user_id == uid
                    ).first()
                    if nivea_prod:
                        return format_prod(nivea_prod)

            # 2. Fallback to master catalog
            cat_query = db.query(models.MasterCatalog)
            if explicit_name:
                cat_prod = cat_query.filter(models.MasterCatalog.name.ilike(f"%{explicit_name}%")).first()
                if cat_prod:
                    return {
                        "id": cat_prod.id,
                        "name": cat_prod.name,
                        "price": cat_prod.suggested_price,
                        "stock": 50,
                        "category": cat_prod.category,
                        "barcode": cat_prod.barcode
                    }
            for kw in kws:
                cat_query = cat_query.filter(models.MasterCatalog.name.ilike(f"%{kw}%"))
            cat_item = cat_query.first()
            if cat_item:
                return {
                    "id": cat_item.id,
                    "name": cat_item.name,
                    "price": cat_item.suggested_price,
                    "stock": 50,
                    "category": cat_item.category,
                    "barcode": cat_item.barcode
                }
            
            return None
        finally:
            db.close()
    except Exception as e:
        logger.warning(f"Product DB lookup failed: {e}")
        return None


# ── Helper ────────────────────────────────────────────────────────────────────
def _verify_aspect_ratio(box: list[float], label: str) -> Optional[bool]:
    """
    Validates physical packaging aspect ratio (height / width).
    Filters out impossible geometric distortions (e.g. flat horizontal CeraVe bottles,
    or tall needle-thin biscuit packets).
    Returns True if valid, False if physically implausible, None if agnostic.
    """
    try:
        x1, y1, x2, y2 = box
        bw = max(1.0, float(x2 - x1))
        bh = max(1.0, float(y2 - y1))
        ratio = bh / bw  # height / width

        lbl = label.lower().strip()

        # Tall cylinders & bottles (must be vertically oriented)
        if lbl in ("cerave", "hns_shampoo", "nivea_deodorant"):
            if ratio < 0.75:  # Flat horizontal shape cannot physically be these bottles
                return False
        elif lbl == "thums_up":
            if ratio < 0.70:  # Beverage can cannot be extremely flat
                return False

        # Horizontal packs & biscuits (cannot be extremely tall skinny vertical slivers)
        elif lbl in ("bourbon", "bourbon_biscuit", "milky_biscuit", "hide_and_seek", "jim_jam", "oreo"):
            if ratio > 2.8:  # Extremely tall narrow sliver
                return False

        return True
    except Exception:
        return None


def _verify_color_signature(crop_img: Image.Image, label: str, conf: float = 0.5) -> Optional[bool]:
    """
    Sub-millisecond (<0.5ms) physical packaging color profile validator with
    adaptive lighting calibration and confidence awareness.
    Eliminates out-of-domain false positives using HSV color distributions.
    Returns True if verified, False if verification fails, and None if verification is unavailable.
    """
    try:
        # High confidence YOLO detections (>0.75) are generally trusted
        # unless an extreme color inversion occurs
        is_high_conf = conf >= 0.75

        small = crop_img.resize((64, 64)).convert("HSV")
        hsv_np = np.array(small)
        h = hsv_np[:, :, 0]
        s = hsv_np[:, :, 1]
        v = hsv_np[:, :, 2]

        # Adaptive saturation threshold: lowers saturation floor in dim/washed-out rooms
        mean_sat = float(np.mean(s))
        sat_floor = 24 if mean_sat < 45 else 36
        val_floor = 35

        saturated = (s > sat_floor) & (v > val_floor)
        total_sat = np.sum(saturated)
        
        # If less than 6% of pixels are saturated (mostly neutral white/gray/black/glare),
        # color verification is inconclusive. Return None (do not reject).
        if total_sat < (64 * 64 * 0.06):
            return None

        h_deg = (h[saturated] / 255.0) * 360.0
        total = float(len(h_deg))
        if total == 0:
            return None

        yellow_pct = (np.sum((h_deg >= 32) & (h_deg <= 78)) / total) * 100
        green_pct = (np.sum((h_deg >= 78) & (h_deg <= 165)) / total) * 100
        blue_pct = (np.sum((h_deg >= 165) & (h_deg <= 260)) / total) * 100
        red_pct = (np.sum((h_deg <= 28) | (h_deg >= 332)) / total) * 100
        purple_pct = (np.sum((h_deg > 260) & (h_deg < 332)) / total) * 100

        lbl = label.lower().strip()

        # Existing Kirana items
        if lbl == "maggi":
            if not is_high_conf and (yellow_pct < 15.0 or green_pct > 35.0):
                return False
        elif lbl in ("surf_excel", "surf"):
            if not is_high_conf and (blue_pct < 8.0 or yellow_pct > 35.0):
                return False
        elif lbl in ("bourbon", "bourbon_biscuit"):
            if not is_high_conf and (blue_pct > 30.0 or green_pct > 30.0 or yellow_pct > 40.0):
                return False
        elif lbl == "oreo":
            if not is_high_conf and (green_pct > 35.0 or yellow_pct > 35.0):
                return False
        elif lbl in ("appe_fizz", "appy_fizz", "appe", "appy"):
            if not is_high_conf and (green_pct > 35.0 or blue_pct > 35.0):
                return False
        elif lbl in ("hide_and_seek", "hide_seek"):
            if not is_high_conf and (green_pct > 35.0 or yellow_pct > 35.0):
                return False
        elif lbl in ("jim_jam", "jimjam"):
            if not is_high_conf and (green_pct > 35.0 or blue_pct > 35.0):
                return False
        elif lbl in ("nivea_deodorant", "nivea"):
            if not is_high_conf and (green_pct > 35.0 or yellow_pct > 35.0):
                return False

        # 8 New Retail Dataset items
        elif lbl == "amul_ice_cream":
            # Creamy / pale / red / blue accents. Rejects bright lime green dominance.
            if green_pct > 40.0:
                return False
        elif lbl == "cake":
            # Britannia Cake: chocolate / warm bakery hues. Rejects cyan/bright blue dominance.
            if blue_pct > 35.0:
                return False
        elif lbl == "cerave":
            # CeraVe: Clean white packaging with either cyan-blue (Lotion) or mint green (Hydrating Cleanser) accents.
            # Rejects dominant yellow or hot magenta.
            if yellow_pct > 45.0 or purple_pct > 40.0:
                return False
        elif lbl == "hns_shampoo":
            # Head & Shoulders: Distinctive crisp white bottle with royal blue cap.
            # Rejects dominant orange / yellow.
            if yellow_pct > 40.0:
                return False
        elif lbl == "nestle_milk_powder":
            # Nestle Everyday: Vibrant sunny yellow & sky blue packaging.
            if purple_pct > 35.0:
                return False
        elif lbl == "plum":
            # Plum: Plum/purple & herbal green tones. Rejects cyan/bright blue dominance.
            if blue_pct > 35.0:
                return False
        elif lbl == "thums_up":
            # Thums Up: High-contrast red and deep navy blue. Rejects bright yellow/green dominance.
            if yellow_pct > 35.0 or green_pct > 30.0:
                return False
        elif lbl == "wild_stone":
            # Wild Stone product line includes Code Platinum (charcoal/silver), Forest Spice Soap (emerald green/black).
            if purple_pct > 40.0:
                return False
        else:
            return None

        return True
    except Exception:
        return None


def _box_iou(b1: list[float], b2: list[float]) -> float:
    x1 = max(b1[0], b2[0])
    y1 = max(b1[1], b2[1])
    x2 = min(b1[2], b2[2])
    y2 = min(b1[3], b2[3])
    inter = max(0.0, x2 - x1) * max(0.0, y2 - y1)
    a1 = max(0.0, b1[2] - b1[0]) * max(0.0, b1[3] - b1[1])
    a2 = max(0.0, b2[2] - b2[0]) * max(0.0, b2[3] - b2[1])
    union = a1 + a2 - inter
    return inter / union if union > 0 else 0.0


def _box_containment(b_small: list[float], b_large: list[float]) -> float:
    """Measures how much of b_small is engulfed inside b_large."""
    x1 = max(b_small[0], b_large[0])
    y1 = max(b_small[1], b_large[1])
    x2 = min(b_small[2], b_large[2])
    y2 = min(b_small[3], b_large[3])
    inter = max(0.0, x2 - x1) * max(0.0, y2 - y1)
    a_small = max(1.0, (b_small[2] - b_small[0]) * (b_small[3] - b_small[1]))
    return inter / a_small


def _run_inference(img: Image.Image, user_id: str, conf_threshold: float = 0.25) -> DetectResponse:
    model = _get_model()
    rgb_img = img.convert("RGB")
    w, h = rgb_img.size

    raw_candidates = []

    # 1. Global Image Inference Pass with optimal imgsz=640
    results = model.predict(
        source=rgb_img,
        imgsz=640,
        conf=max(0.15, conf_threshold - 0.05),
        iou=0.45,
        agnostic_nms=False,
        verbose=False
    )

    for result in results:
        for box in result.boxes:
            cls_id = int(box.cls[0])
            label = model.names[cls_id]
            conf = float(box.conf[0])
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            raw_candidates.append((label, conf, [x1, y1, x2, y2]))

    # 2. Sort candidates by confidence descending
    raw_candidates.sort(key=lambda x: x[1], reverse=True)

    # 3. Intelligent Overlap Deduplication & Cross-Class Suppression
    deduped = []
    for cand in raw_candidates:
        lbl, conf, box = cand
        merged_or_suppressed = False
        for idx, kept in enumerate(deduped):
            k_lbl, k_conf, k_box = kept
            iou = _box_iou(box, k_box)
            cont_cand_in_kept = _box_containment(box, k_box)
            cont_kept_in_cand = _box_containment(k_box, box)

            if lbl == k_lbl:
                # Same class: if they overlap or one contains another, merge into full packaging bounding box
                if iou > 0.10 or cont_cand_in_kept > 0.25 or cont_kept_in_cand > 0.25:
                    new_box = [
                        min(box[0], k_box[0]),
                        min(box[1], k_box[1]),
                        max(box[2], k_box[2]),
                        max(box[3], k_box[3])
                    ]
                    deduped[idx] = (lbl, max(conf, k_conf), new_box)
                    merged_or_suppressed = True
                    break
            else:
                # Cross-class overlap on same physical item: suppress lower confidence candidate
                if iou > 0.25 or cont_cand_in_kept > 0.35 or cont_kept_in_cand > 0.35:
                    merged_or_suppressed = True
                    break

        if not merged_or_suppressed:
            deduped.append(cand)

    # 4. Multi-Pass Chain Merging (unify any newly expanded boxes that now overlap)
    changed = True
    while changed:
        changed = False
        for i in range(len(deduped)):
            for j in range(i + 1, len(deduped)):
                l1, c1, b1 = deduped[i]
                l2, c2, b2 = deduped[j]
                if l1 == l2:
                    iou = _box_iou(b1, b2)
                    c1_in_2 = _box_containment(b1, b2)
                    c2_in_1 = _box_containment(b2, b1)
                    if iou > 0.10 or c1_in_2 > 0.25 or c2_in_1 > 0.25:
                        new_box = [min(b1[0], b2[0]), min(b1[1], b2[1]), max(b1[2], b2[2]), max(b1[3], b2[3])]
                        deduped[i] = (l1, max(c1, c2), new_box)
                        deduped.pop(j)
                        changed = True
                        break
            if changed:
                break

    # 5. Candidate Verification & Packaging Validation
    detections: list[Detection] = []
    for lbl, conf, box in deduped:
        x1, y1, x2, y2 = box
        crop_box = (max(0, int(x1)), max(0, int(y1)), min(w, int(x2)), min(h, int(y2)))
        
        # Guard against zero-area crop
        if crop_box[2] <= crop_box[0] or crop_box[3] <= crop_box[1]:
            continue

        crop = rgb_img.crop(crop_box)

        # 1. Geometric Aspect Ratio Check (Eliminates physically impossible boxes)
        aspect_res = _verify_aspect_ratio(box, lbl)
        if aspect_res is False:
            if conf < 0.65:
                logger.info(f"📐 [Aspect Ratio Filter] Rejected implausible shape for {lbl} ({conf*100:.1f}%)")
                continue
            else:
                conf = conf * 0.80

        # 2. Packaging Color Verification (HSV profile with lighting tolerance)
        c_res = _verify_color_signature(crop, lbl, conf=conf)
        if c_res is False:
            if conf < 0.45:
                logger.info(f"🎨 [Color Guard] Rejected out-of-domain packaging color for {lbl} ({conf*100:.1f}%)")
                continue
            elif conf < 0.70:
                conf = conf * 0.85

        # 3. For very low confidence (borderline noise < 0.20), cross-check guards
        if conf < 0.20:
            try:
                from retail_classifier import verify_crop_matches_label
                v_res = verify_crop_matches_label(crop, lbl, min_matches=12)
                # Only suppress if both guards confirm mismatch
                if v_res is False and c_res is False:
                    logger.info(f"🛡️ [Verification Guard] Filtered low-confidence noise: {lbl} ({conf*100:.1f}%)")
                    continue
            except Exception as e:
                logger.warning(f"Verification guard check ignored: {e}")

        prod_match = _match_db_product(lbl, user_id)
        class_display_name = FRIENDLY_NAMES.get(lbl, lbl.replace("_", " ").title())

        detections.append(Detection(
            label=lbl,
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
                prod_match = _match_db_product(lbl, user_id)
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
    user_id: CurrentUser,
    file: UploadFile = File(...),
    conf: float = Form(default=0.25)
):
    """
    Accept a JPEG/PNG camera frame and return YOLO detections.
    The Android app and Admin Portal send an image frame here.
    """
    try:
        contents = await file.read()
        if not contents or len(contents) == 0:
            raise HTTPException(status_code=400, detail="Uploaded image file is empty")
        if len(contents) > 25 * 1024 * 1024:
            raise HTTPException(status_code=413, detail="Image size exceeds maximum limit of 25MB")
        try:
            img = Image.open(io.BytesIO(contents))
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Invalid image format: {e}")

        try:
            transposed = ImageOps.exif_transpose(img)
            if transposed is not None:
                img = transposed
        except Exception:
            pass

        if img.mode != "RGB":
            img = img.convert("RGB")

        orig_w, orig_h = img.width, img.height
        if orig_w <= 0 or orig_h <= 0:
            raise HTTPException(status_code=400, detail="Invalid image dimensions")

        if max(img.width, img.height) > 1920:
            img.thumbnail((1920, 1920), Image.Resampling.LANCZOS)

        safe_conf = max(0.05, min(0.95, float(conf)))
        res = _run_inference(img, user_id, conf_threshold=safe_conf)
        scale_x = orig_w / float(img.width) if img.width > 0 else 1.0
        scale_y = orig_h / float(img.height) if img.height > 0 else 1.0
        for det in res.detections:
            if det.box:
                det.box = [
                    int(round(det.box[0] * scale_x)),
                    int(round(det.box[1] * scale_y)),
                    int(round(det.box[2] * scale_x)),
                    int(round(det.box[3] * scale_y)),
                ]
        return res
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Detection error: {e}")
        raise HTTPException(status_code=400, detail=f"Detection failed: {e}")


@router.post("/base64", response_model=DetectResponse, summary="Detect products from a base64 image")
async def detect_from_base64(
    user_id: CurrentUser,
    payload: dict
):
    """
    Accept JSON with { "image": "<base64>", "conf": 0.65 } and return detections.
    Useful for the Android CameraX analysis use-case.
    """
    try:
        b64 = payload.get("image", "")
        if not b64:
            raise HTTPException(status_code=400, detail="Missing base64 image string")
        if len(b64) > 30 * 1024 * 1024:
            raise HTTPException(status_code=413, detail="Image size exceeds maximum limit of 30MB")
        conf = float(payload.get("conf", 0.35))
        try:
            img_bytes = base64.b64decode(b64)
            img = Image.open(io.BytesIO(img_bytes))
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Invalid base64 image: {e}")

        try:
            transposed = ImageOps.exif_transpose(img)
            if transposed is not None:
                img = transposed
        except Exception:
            pass

        if img.mode != "RGB":
            img = img.convert("RGB")

        orig_w, orig_h = img.width, img.height
        if orig_w <= 0 or orig_h <= 0:
            raise HTTPException(status_code=400, detail="Invalid image dimensions")

        if max(img.width, img.height) > 1920:
            img.thumbnail((1920, 1920), Image.Resampling.LANCZOS)

        safe_conf = max(0.05, min(0.95, float(conf)))
        res = _run_inference(img, user_id, conf_threshold=safe_conf)
        scale_x = orig_w / float(img.width) if img.width > 0 else 1.0
        scale_y = orig_h / float(img.height) if img.height > 0 else 1.0
        for det in res.detections:
            if det.box:
                det.box = [
                    int(round(det.box[0] * scale_x)),
                    int(round(det.box[1] * scale_y)),
                    int(round(det.box[2] * scale_x)),
                    int(round(det.box[3] * scale_y)),
                ]
        return res
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Detection base64 error: {e}")
        raise HTTPException(status_code=400, detail=f"Detection failed: {e}")


@router.get("/classes", summary="List classes the YOLO model can detect")
async def get_classes():
    """Returns the product classes the trained model knows."""
    try:
        model = _get_model()
        return {"classes": model.names, "num_classes": len(model.names)}
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))

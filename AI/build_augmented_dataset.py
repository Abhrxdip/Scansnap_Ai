#!/usr/bin/env python3
"""
ScanSnap AI — Production Retail Dataset Augmentation & YOLO Pipeline
Generates an augmented multi-class grocery dataset with:
  1. Geometric transformations (rotation, scale, crop, perspective, flip)
  2. Photometric augmentations (lighting, color balance, blur, contrast)
  3. Hard negative background generation (walls, desks, floors) to eliminate false positives
  4. Precise bounding box coordinate transforms
"""

import math
import os
import random
import shutil
import sys
from pathlib import Path
import cv2
import numpy as np
from PIL import Image

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

CLASSES = [
    "amul_ice_cream",      # 0
    "cake",                # 1
    "cerave",              # 2
    "hns_shampoo",         # 3
    "nestle_milk_powder",  # 4
    "plum",                # 5
    "thums_up",            # 6
    "wild_stone",          # 7
    "nivea_deodorant",     # 8
    "bourbon_biscuit",     # 9
    "milky_biscuit",       # 10
]

RETAIL_FOLDERS = {
    "Amul_Ice_Cream": 0,
    "Cake": 1,
    "CeraVe": 2,
    "HnS_Shampoo": 3,
    "Nestle_Milk_Powder": 4,
    "Plum": 5,
    "Thums_Up": 6,
    "Wild_Stone": 7,
}

PILOT_CLASS_MAP = {
    0: 8,   # nivea_deodorant
    1: 9,   # bourbon_biscuit
    2: 10,  # milky_biscuit
}


def detect_foreground_box(img_bgr: np.ndarray) -> tuple[float, float, float, float]:
    """
    Returns normalized [xc, yc, w, h] for prominent foreground object.
    Falls back to centered tight bounding box.
    """
    h, w = img_bgr.shape[:2]
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (7, 7), 0)

    # Estimate background from borders
    border_pixels = np.concatenate([
        blurred[:15, :].flatten(),
        blurred[-15:, :].flatten(),
        blurred[:, :15].flatten(),
        blurred[:, -15:].flatten(),
    ])
    bg_val = float(np.median(border_pixels))
    diff = np.abs(blurred.astype(np.float32) - bg_val)
    _, mask = cv2.threshold(diff.astype(np.uint8), 24, 255, cv2.THRESH_BINARY)

    # Morphological clean
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (7, 7))
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel, iterations=2)
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    valid_boxes = []
    for c in contours:
        x, y, bw, bh = cv2.boundingRect(c)
        if bw > 0.15 * w and bh > 0.15 * h:
            valid_boxes.append((x, y, bw, bh))

    if valid_boxes:
        valid_boxes.sort(key=lambda b: b[2] * b[3], reverse=True)
        bx, by, bw, bh = valid_boxes[0]
        # Margin
        pad_x = int(bw * 0.04)
        pad_y = int(bh * 0.04)
        x1 = max(0, bx - pad_x)
        y1 = max(0, by - pad_y)
        x2 = min(w, bx + bw + pad_x)
        y2 = min(h, by + bh + pad_y)
        xc = (x1 + x2) / (2.0 * w)
        yc = (y1 + y2) / (2.0 * h)
        return (xc, yc, (x2 - x1) / w, (y2 - y1) / h)

    # Robust packshot centered default
    return (0.50, 0.50, 0.82, 0.88)


def rotate_image_and_bbox(img: np.ndarray, bbox: tuple[float, float, float, float], angle: float) -> tuple[np.ndarray, tuple[float, float, float, float]]:
    h, w = img.shape[:2]
    xc, yc, bw, bh = bbox
    x1 = (xc - bw / 2.0) * w
    y1 = (yc - bh / 2.0) * h
    x2 = (xc + bw / 2.0) * w
    y2 = (yc + bh / 2.0) * h

    center = (w / 2.0, h / 2.0)
    rot_mat = cv2.getRotationMatrix2D(center, angle, 1.0)
    rotated = cv2.warpAffine(img, rot_mat, (w, h), borderMode=cv2.BORDER_REFLECT_101)

    # Rotate bbox corners
    corners = np.array([
        [x1, y1, 1],
        [x2, y1, 1],
        [x2, y2, 1],
        [x1, y2, 1]
    ]).T
    rot_corners = np.dot(rot_mat, corners)
    rx_min = max(0.0, float(np.min(rot_corners[0, :])))
    ry_min = max(0.0, float(np.min(rot_corners[1, :])))
    rx_max = min(float(w), float(np.max(rot_corners[0, :])))
    ry_max = min(float(h), float(np.max(rot_corners[1, :])))

    new_w = max(10.0, rx_max - rx_min)
    new_h = max(10.0, ry_max - ry_min)
    new_xc = (rx_min + rx_max) / (2.0 * w)
    new_yc = (ry_min + ry_max) / (2.0 * h)
    return rotated, (new_xc, new_yc, new_w / w, new_h / h)


def scale_image_and_bbox(img: np.ndarray, bbox: tuple[float, float, float, float], factor: float) -> tuple[np.ndarray, tuple[float, float, float, float]]:
    h, w = img.shape[:2]
    xc, yc, bw, bh = bbox

    new_w = int(w * factor)
    new_h = int(h * factor)
    resized = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_LINEAR)

    canvas = np.zeros_like(img)
    canvas[:] = np.median(img[:10, :10], axis=(0, 1)).astype(np.uint8)

    if factor < 1.0:
        # Pad centered
        dx = (w - new_w) // 2
        dy = (h - new_h) // 2
        canvas[dy:dy+new_h, dx:dx+new_w] = resized
        new_bw = bw * factor
        new_bh = bh * factor
        new_xc = xc * factor + (dx / w)
        new_yc = yc * factor + (dy / h)
        return canvas, (new_xc, new_yc, new_bw, new_bh)
    else:
        # Crop centered
        dx = (new_w - w) // 2
        dy = (new_h - h) // 2
        canvas = resized[dy:dy+h, dx:dx+w]
        new_bw = min(0.98, bw * factor)
        new_bh = min(0.98, bh * factor)
        return canvas, (0.50, 0.50, new_bw, new_bh)


def generate_synthetic_backgrounds(count: int = 35) -> list[np.ndarray]:
    bgs = []
    np.random.seed(42)

    for i in range(count):
        h, w = 480, 480
        mode = i % 5

        if mode == 0:
            # Neutral off-white / beige painted wall
            base = np.random.randint(215, 245)
            noise = np.random.normal(0, 4, (h, w, 3)).astype(np.int16)
            img = np.clip(base + noise, 0, 255).astype(np.uint8)
            grad = np.tile(np.linspace(-8, 8, w), (h, 1))[:, :, None]
            img = np.clip(img.astype(np.float32) + grad, 0, 255).astype(np.uint8)

        elif mode == 1:
            # Wood grain desk surface
            wood_base = np.array([45, 85, 140], dtype=np.float32)
            grain = np.sin(np.linspace(0, 40, w)) * 15
            grain_img = np.tile(grain, (h, 1))[:, :, None]
            noise = np.random.normal(0, 8, (h, w, 3))
            img = np.clip(wood_base + grain_img + noise, 0, 255).astype(np.uint8)

        elif mode == 2:
            # Gray office desk / mousepad texture
            base_gray = np.random.randint(50, 120)
            noise = np.random.normal(0, 6, (h, w, 3))
            img = np.clip(base_gray + noise, 0, 255).astype(np.uint8)

        elif mode == 3:
            # Pastel colored wall
            pastel = random.choice([
                [220, 200, 180],
                [200, 230, 210],
                [190, 220, 240],
            ])
            noise = np.random.normal(0, 5, (h, w, 3))
            img = np.clip(pastel + noise, 0, 255).astype(np.uint8)

        else:
            # Smooth desk laminate with shadows
            base = np.full((h, w, 3), 180, dtype=np.uint8)
            y_grad = np.tile(np.linspace(-30, 30, h)[:, None], (1, w))[:, :, None]
            noise = np.random.normal(0, 5, (h, w, 3))
            img = np.clip(base.astype(np.float32) + y_grad + noise, 0, 255).astype(np.uint8)

        bgs.append(img)
    return bgs


def augment_sample(img_bgr: np.ndarray, bbox: tuple[float, float, float, float]) -> list[tuple[np.ndarray, tuple[float, float, float, float]]]:
    xc, yc, bw, bh = bbox
    variants = []

    # 1. Base clean image
    variants.append((img_bgr.copy(), bbox))

    # 2. Horizontal flip
    flipped = cv2.flip(img_bgr, 1)
    new_xc = 1.0 - xc
    variants.append((flipped, (new_xc, yc, bw, bh)))

    # 3. Brightness boost
    bright = cv2.convertScaleAbs(img_bgr, alpha=1.20, beta=25)
    variants.append((bright, bbox))

    # 4. Low light / shadow
    dark = cv2.convertScaleAbs(img_bgr, alpha=0.75, beta=-15)
    variants.append((dark, bbox))

    # 5. Warm bulb lighting
    warm = img_bgr.astype(np.float32)
    warm[:, :, 2] = np.clip(warm[:, :, 2] * 1.15 + 10, 0, 255)
    warm[:, :, 0] = np.clip(warm[:, :, 0] * 0.90 - 5, 0, 255)
    variants.append((warm.astype(np.uint8), bbox))

    # 6. Cool tube light
    cool = img_bgr.astype(np.float32)
    cool[:, :, 0] = np.clip(cool[:, :, 0] * 1.15 + 10, 0, 255)
    cool[:, :, 2] = np.clip(cool[:, :, 2] * 0.90 - 5, 0, 255)
    variants.append((cool.astype(np.uint8), bbox))

    # 7. Motion blur
    blurred = cv2.GaussianBlur(img_bgr, (5, 5), 1.2)
    variants.append((blurred, bbox))

    # 8. Mild tilt left (-12 deg)
    rot_left, bbox_l = rotate_image_and_bbox(img_bgr, bbox, -12.0)
    variants.append((rot_left, bbox_l))

    # 9. Mild tilt right (+12 deg)
    rot_right, bbox_r = rotate_image_and_bbox(img_bgr, bbox, 12.0)
    variants.append((rot_right, bbox_r))

    # 10. Zoom out with padding (scale 0.82x)
    scaled, bbox_s = scale_image_and_bbox(img_bgr, bbox, 0.82)
    variants.append((scaled, bbox_s))

    return variants


def build_augmented_dataset(output_dir: str = "AI/dataset/grocery/augmented_retail"):
    random.seed(42)
    np.random.seed(42)

    root = Path(__file__).resolve().parent.parent
    dataset_dir = root / "Dataset"
    pilot_dir = root / "AI" / "dataset" / "grocery" / "pilot_batch_001_yolo"
    out_path = root / output_dir

    if out_path.exists():
        shutil.rmtree(out_path)

    for split in ["train", "val"]:
        (out_path / "images" / split).mkdir(parents=True, exist_ok=True)
        (out_path / "labels" / split).mkdir(parents=True, exist_ok=True)

    print("=" * 70)
    print("🚀 ScanSnap AI: Generating Augmented Multi-Class Retail Dataset")
    print("=" * 70)

    class_stats = {cls_name: {"train": 0, "val": 0} for cls_name in CLASSES}
    total_samples = 0

    # ── 1. Process 8 Retail Dataset Classes ──────────────────────────────────────
    for folder_name, cls_id in RETAIL_FOLDERS.items():
        folder_path = dataset_dir / folder_name
        cls_name = CLASSES[cls_id]
        if not folder_path.exists():
            print(f"⚠️ Folder {folder_name} missing, skipping...")
            continue

        raw_imgs = [f for f in folder_path.iterdir() if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png", ".webp"]]
        random.shuffle(raw_imgs)

        val_cutoff = max(1, int(len(raw_imgs) * 0.20))
        val_raw = raw_imgs[:val_cutoff]
        train_raw = raw_imgs[val_cutoff:]

        for raw_path in train_raw:
            img = cv2.imread(str(raw_path))
            if img is None:
                continue
            base_bbox = detect_foreground_box(img)
            variants = augment_sample(img, base_bbox)

            for v_idx, (v_img, v_box) in enumerate(variants):
                fname = f"{cls_name}_tr_{raw_path.stem}_v{v_idx}.jpg"
                img_dest = out_path / "images" / "train" / fname
                lbl_dest = out_path / "labels" / "train" / f"{cls_name}_tr_{raw_path.stem}_v{v_idx}.txt"

                cv2.imwrite(str(img_dest), v_img, [cv2.IMWRITE_JPEG_QUALITY, 92])
                with open(lbl_dest, "w", encoding="utf-8") as lf:
                    lf.write(f"{cls_id} {v_box[0]:.6f} {v_box[1]:.6f} {v_box[2]:.6f} {v_box[3]:.6f}\n")
                class_stats[cls_name]["train"] += 1
                total_samples += 1

        for raw_path in val_raw:
            img = cv2.imread(str(raw_path))
            if img is None:
                continue
            base_bbox = detect_foreground_box(img)

            fname = f"{cls_name}_val_{raw_path.name}"
            img_dest = out_path / "images" / "val" / fname
            lbl_dest = out_path / "labels" / "val" / f"{cls_name}_val_{raw_path.stem}.txt"
            cv2.imwrite(str(img_dest), img, [cv2.IMWRITE_JPEG_QUALITY, 95])
            with open(lbl_dest, "w", encoding="utf-8") as lf:
                lf.write(f"{cls_id} {base_bbox[0]:.6f} {base_bbox[1]:.6f} {base_bbox[2]:.6f} {base_bbox[3]:.6f}\n")
            class_stats[cls_name]["val"] += 1
            total_samples += 1

    # ── 2. Process Pilot Classes (Nivea, Bourbon, Milky) ────────────────────────
    if pilot_dir.exists():
        for split in ["train", "val"]:
            lbl_dir = pilot_dir / "labels" / split
            img_dir = pilot_dir / "images" / split
            if not lbl_dir.exists():
                continue

            for lbl_file in lbl_dir.glob("*.txt"):
                img_candidates = [
                    img_dir / f"{lbl_file.stem}.jpg",
                    img_dir / f"{lbl_file.stem}.jpeg",
                    img_dir / f"{lbl_file.stem}.png"
                ]
                img_file = next((f for f in img_candidates if f.exists()), None)
                if not img_file:
                    continue

                with open(lbl_file, "r", encoding="utf-8") as f:
                    lines = [l.strip().split() for l in f.readlines() if l.strip()]

                if not lines:
                    continue

                old_cls = int(lines[0][0])
                if old_cls not in PILOT_CLASS_MAP:
                    continue
                new_cls = PILOT_CLASS_MAP[old_cls]
                cls_name = CLASSES[new_cls]
                box = tuple(map(float, lines[0][1:5]))

                img = cv2.imread(str(img_file))
                if img is None:
                    continue

                if split == "train":
                    variants = augment_sample(img, box)
                    for v_idx, (v_img, v_box) in enumerate(variants):
                        fname = f"{cls_name}_pilot_tr_{img_file.stem}_v{v_idx}.jpg"
                        img_dest = out_path / "images" / "train" / fname
                        lbl_dest = out_path / "labels" / "train" / f"{cls_name}_pilot_tr_{img_file.stem}_v{v_idx}.txt"
                        cv2.imwrite(str(img_dest), v_img, [cv2.IMWRITE_JPEG_QUALITY, 92])
                        with open(lbl_dest, "w", encoding="utf-8") as lf:
                            lf.write(f"{new_cls} {v_box[0]:.6f} {v_box[1]:.6f} {v_box[2]:.6f} {v_box[3]:.6f}\n")
                        class_stats[cls_name]["train"] += 1
                        total_samples += 1
                else:
                    fname = f"{cls_name}_pilot_val_{img_file.name}"
                    img_dest = out_path / "images" / "val" / fname
                    lbl_dest = out_path / "labels" / "val" / f"{cls_name}_pilot_val_{img_file.stem}.txt"
                    cv2.imwrite(str(img_dest), img, [cv2.IMWRITE_JPEG_QUALITY, 95])
                    with open(lbl_dest, "w", encoding="utf-8") as lf:
                        lf.write(f"{new_cls} {box[0]:.6f} {box[1]:.6f} {box[2]:.6f} {box[3]:.6f}\n")
                    class_stats[cls_name]["val"] += 1
                    total_samples += 1

    # ── 3. Add Negative Background Samples (Empty Labels) ───────────────────────
    bg_images = generate_synthetic_backgrounds(count=35)
    for bg_idx, bg_img in enumerate(bg_images):
        split = "val" if bg_idx < 5 else "train"
        fname = f"neg_background_{bg_idx:03d}.jpg"
        img_dest = out_path / "images" / split / fname
        lbl_dest = out_path / "labels" / split / f"neg_background_{bg_idx:03d}.txt"

        cv2.imwrite(str(img_dest), bg_img, [cv2.IMWRITE_JPEG_QUALITY, 92])
        with open(lbl_dest, "w", encoding="utf-8") as lf:
            pass
        total_samples += 1

    # ── 4. Generate data.yaml ───────────────────────────────────────────────────
    yaml_lines = [
        "# ScanSnap AI — Augmented 11-Class FMCG Retail Dataset",
        f"path: {out_path.resolve().as_posix()}",
        "train: images/train",
        "val: images/val",
        "",
        "names:"
    ]
    for idx, cname in enumerate(CLASSES):
        yaml_lines.append(f"  {idx}: {cname}")

    yaml_path = out_path / "data.yaml"
    with open(yaml_path, "w", encoding="utf-8") as yf:
        yf.write("\n".join(yaml_lines) + "\n")

    print("\n✅ Dataset Generation Complete!")
    print(f"📁 Destination: {out_path}")
    print(f"📄 Data YAML:   {yaml_path}")
    print(f"📊 Total Image Samples: {total_samples}")
    print("\nBreakdown by Class:")
    for cname, counts in class_stats.items():
        print(f"  • {cname:<20}: {counts['train']:>3} train | {counts['val']:>2} val")
    print(f"  • {'neg_backgrounds':<20}:  30 train |  5 val  (empty labels for zero-false-positive training)")
    print("=" * 70)


if __name__ == "__main__":
    build_augmented_dataset()

#!/usr/bin/env python3
"""
ScanSnap AI — Retail Dataset Preprocessor & YOLO Annotator
Converts raw image folders in Dataset/ into a YOLOv11-compatible object detection dataset
with automatic bounding-box localization, train/val splitting, and data.yaml generation.
"""

import os
import sys
import shutil
import random
from pathlib import Path
from PIL import Image
import numpy as np

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass


CLASS_MAPPING = {
    "Amul_Ice_Cream": (0, "amul_ice_cream", "Amul Ice Cream"),
    "Cake": (1, "cake", "Britannia Cake"),
    "CeraVe": (2, "cerave", "CeraVe Moisturizing Lotion"),
    "HnS_Shampoo": (3, "hns_shampoo", "Head & Shoulders Shampoo"),
    "Nestle_Milk_Powder": (4, "nestle_milk_powder", "Nestle Everyday Milk Powder"),
    "Plum": (5, "plum", "Plum Skincare"),
    "Thums_Up": (6, "thums_up", "Thums Up Soft Drink"),
    "Wild_Stone": (7, "wild_stone", "Wild Stone Deodorant Spray"),
}


def find_foreground_bbox(img_path: Path) -> tuple[float, float, float, float]:
    """
    Locates the prominent foreground retail package in an image.
    Returns normalized (x_center, y_center, width, height).
    """
    try:
        with Image.open(img_path) as im:
            im = im.convert("RGB")
            w, h = im.size
            arr = np.array(im)

            # Detect foreground by gradient/variance from border corners
            gray = np.mean(arr, axis=2).astype(np.uint8)
            corners = [
                gray[:20, :20],
                gray[:20, -20:],
                gray[-20:, :20],
                gray[-20:, -20:]
            ]
            bg_color = np.median([np.median(c) for c in corners])
            diff = np.abs(gray.astype(np.int16) - bg_color)
            mask = diff > 22

            coords = np.argwhere(mask)
            if len(coords) > 100:
                y0, x0 = coords.min(axis=0)
                y1, x1 = coords.max(axis=0)

                # Add small padding margin
                pad_x = int((x1 - x0) * 0.05)
                pad_y = int((y1 - y0) * 0.05)
                x0 = max(0, x0 - pad_x)
                y0 = max(0, y0 - pad_y)
                x1 = min(w, x1 + pad_x)
                y1 = min(h, y1 + pad_y)

                bw = x1 - x0
                bh = y1 - y0
                if bw > 0.15 * w and bh > 0.15 * h:
                    x_center = (x0 + x1) / (2.0 * w)
                    y_center = (y0 + y1) / (2.0 * h)
                    norm_w = bw / w
                    norm_h = bh / h
                    return (x_center, y_center, norm_w, norm_h)
    except Exception as e:
        pass

    # Default robust centered packshot bounding box
    return (0.50, 0.50, 0.85, 0.88)


def prepare_dataset(
    source_dir: str = "Dataset",
    output_dir: str = "AI/dataset/grocery/retail_8class",
    val_ratio: float = 0.20,
    seed: int = 42
):
    random.seed(seed)
    root = Path(__file__).resolve().parent.parent
    src = root / source_dir
    dst = root / output_dir

    if not src.exists():
        raise FileNotFoundError(f"Source dataset directory {src} does not exist.")

    print(f"🚀 Processing Retail Product Dataset from {src} -> {dst}")

    # Create YOLO directory hierarchy
    for split in ["train", "val"]:
        (dst / "images" / split).mkdir(parents=True, exist_ok=True)
        (dst / "labels" / split).mkdir(parents=True, exist_ok=True)

    summary = {}
    total_images = 0

    for folder_name, (cls_id, cls_label, human_name) in CLASS_MAPPING.items():
        folder_path = src / folder_name
        if not folder_path.exists():
            print(f"⚠️ Folder {folder_name} not found, skipping...")
            continue

        images = [f for f in folder_path.iterdir() if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png", ".webp"]]
        random.shuffle(images)

        val_count = max(1, int(len(images) * val_ratio))
        val_imgs = images[:val_count]
        train_imgs = images[val_count:]

        summary[cls_label] = {"train": len(train_imgs), "val": len(val_imgs), "name": human_name}
        total_images += len(images)

        for split, img_list in [("train", train_imgs), ("val", val_imgs)]:
            for img_path in img_list:
                # Target filename
                target_filename = f"{cls_label}_{img_path.name}"
                target_img_path = dst / "images" / split / target_filename
                shutil.copy2(img_path, target_img_path)

                # Compute YOLO normalized bbox
                xc, yc, bw, bh = find_foreground_bbox(img_path)
                label_filename = f"{cls_label}_{img_path.stem}.txt"
                target_label_path = dst / "labels" / split / label_filename

                with open(target_label_path, "w", encoding="utf-8") as f:
                    f.write(f"{cls_id} {xc:.6f} {yc:.6f} {bw:.6f} {bh:.6f}\n")

    # Generate data.yaml
    names_dict = {cls_id: cls_label for _, (cls_id, cls_label, _) in CLASS_MAPPING.items()}
    yaml_content = f"""# ScanSnap AI — 8-Class Retail FMCG Dataset
path: {dst.resolve().as_posix()}
train: images/train
val: images/val

names:
"""
    for cls_id in sorted(names_dict.keys()):
        yaml_content += f"  {cls_id}: {names_dict[cls_id]}\n"

    yaml_path = dst / "data.yaml"
    with open(yaml_path, "w", encoding="utf-8") as f:
        f.write(yaml_content)

    print("\n" + "=" * 65)
    print("✅ YOLO Dataset Successfully Created!")
    print(f"📁 Destination: {dst}")
    print(f"📊 Total Images: {total_images}")
    print("📦 Classes Breakdown:")
    for lbl, counts in summary.items():
        print(f"  • {lbl:<20}: {counts['train']} train | {counts['val']} val  ({counts['name']})")
    print(f"📄 Config YAML saved to: {yaml_path}")
    print("=" * 65)


if __name__ == "__main__":
    prepare_dataset()

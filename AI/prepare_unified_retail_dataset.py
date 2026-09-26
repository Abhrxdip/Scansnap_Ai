#!/usr/bin/env python3
"""
ScanSnap AI — Unified Retail Dataset Builder
Merges pilot batch products and the 8 new retail dataset products
into a single unified YOLOv11 training dataset.
"""

import os
import sys
import shutil
from pathlib import Path

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

UNIFIED_CLASSES = [
    # 8 New Retail Dataset items
    "amul_ice_cream",       # 0
    "cake",                 # 1
    "cerave",               # 2
    "hns_shampoo",          # 3
    "nestle_milk_powder",   # 4
    "plum",                 # 5
    "thums_up",             # 6
    "wild_stone",           # 7
    # Prior Pilot & Core Kirana items
    "nivea_deodorant",      # 8
    "bourbon_biscuit",      # 9
    "milky_biscuit",        # 10
    "maggi",                # 11
    "surf_excel",           # 12
    "hide_and_seek",        # 13
    "oreo",                 # 14
    "appe_fizz",            # 15
    "jim_jam",              # 16
]

PILOT_REMAP = {
    0: 8,   # nivea_deodorant
    1: 9,   # bourbon_biscuit
    2: 10,  # milky_biscuit
}

RETAIL_REMAP = {
    0: 0,
    1: 1,
    2: 2,
    3: 3,
    4: 4,
    5: 5,
    6: 6,
    7: 7,
}


def build_unified_dataset():
    root = Path(__file__).resolve().parent.parent
    retail_dir = root / "AI" / "dataset" / "grocery" / "retail_8class"
    pilot_dir = root / "AI" / "dataset" / "grocery" / "pilot_batch_001_yolo"
    unified_dir = root / "AI" / "dataset" / "grocery" / "unified_retail"

    for split in ["train", "val"]:
        (unified_dir / "images" / split).mkdir(parents=True, exist_ok=True)
        (unified_dir / "labels" / split).mkdir(parents=True, exist_ok=True)

    counts = {cls_name: 0 for cls_name in UNIFIED_CLASSES}

    # 1. Copy retail_8class
    if retail_dir.exists():
        for split in ["train", "val"]:
            lbl_dir = retail_dir / "labels" / split
            img_dir = retail_dir / "images" / split
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

                # Read and re-write label with unified class ID
                new_lines = []
                with open(lbl_file, "r", encoding="utf-8") as f:
                    for line in f:
                        parts = line.strip().split()
                        if not parts:
                            continue
                        old_cls = int(parts[0])
                        new_cls = RETAIL_REMAP.get(old_cls, old_cls)
                        counts[UNIFIED_CLASSES[new_cls]] += 1
                        new_lines.append(f"{new_cls} " + " ".join(parts[1:]))

                # Copy image and write label
                shutil.copy2(img_file, unified_dir / "images" / split / img_file.name)
                with open(unified_dir / "labels" / split / lbl_file.name, "w", encoding="utf-8") as f:
                    f.write("\n".join(new_lines) + "\n")

    # 2. Copy pilot_batch_001_yolo
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

                new_lines = []
                with open(lbl_file, "r", encoding="utf-8") as f:
                    for line in f:
                        parts = line.strip().split()
                        if not parts:
                            continue
                        old_cls = int(parts[0])
                        if old_cls in PILOT_REMAP:
                            new_cls = PILOT_REMAP[old_cls]
                            counts[UNIFIED_CLASSES[new_cls]] += 1
                            new_lines.append(f"{new_cls} " + " ".join(parts[1:]))

                shutil.copy2(img_file, unified_dir / "images" / split / f"pilot_{img_file.name}")
                with open(unified_dir / "labels" / split / f"pilot_{lbl_file.name}", "w", encoding="utf-8") as f:
                    f.write("\n".join(new_lines) + "\n")

    # Generate unified data.yaml
    yaml_lines = [
        "# ScanSnap AI — Unified Multi-Product Retail YOLO Dataset",
        f"path: {unified_dir.resolve().as_posix()}",
        "train: images/train",
        "val: images/val",
        "",
        "names:"
    ]
    for idx, name in enumerate(UNIFIED_CLASSES):
        yaml_lines.append(f"  {idx}: {name}")

    yaml_path = unified_dir / "data.yaml"
    with open(yaml_path, "w", encoding="utf-8") as f:
        f.write("\n".join(yaml_lines) + "\n")

    print("=" * 65)
    print("✅ Unified Multi-Product Dataset Built Successfully!")
    print(f"📁 Path: {unified_dir}")
    print(f"📄 Config YAML: {yaml_path}")
    print("📊 Samples per class:")
    for cls_name, cnt in counts.items():
        if cnt > 0:
            print(f"  • {cls_name:<20}: {cnt} annotations")
    print("=" * 65)


if __name__ == "__main__":
    build_unified_dataset()

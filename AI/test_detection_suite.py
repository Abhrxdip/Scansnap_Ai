#!/usr/bin/env python3
"""
Test Visual Object Detection on Retail Samples using calibrated HSV & Aspect-Ratio filters.
"""
import sys
from pathlib import Path
from PIL import Image

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / "backend"))

from routers.detect import _run_inference

sample_dir = root / "AdminPortal" / "public" / "samples"

print("=" * 75)
print("🚀 ScanSnap AI: Visual Object Detection Benchmark (Retail Dataset Samples)")
print("=" * 75)
print(f"{'Image Sample':<26} | {'Status':<8} | {'Detections (Label & Conf)'}")
print("-" * 75)

for img_path in sorted(sample_dir.glob("*.jpg")):
    img = Image.open(img_path)
    res = _run_inference(img, user_id="demo_user", conf_threshold=0.20)
    dets = [f"{d.label} ({d.confidence*100:.1f}%)" for d in res.detections]
    status = "✅ PASS" if dets else "❌ FAIL"
    det_str = ", ".join(dets) if dets else "No detection"
    print(f"{img_path.name:<26} | {status:<8} | {det_str}")

print("=" * 75)

#!/usr/bin/env python3
"""
Custom YOLO Training Pipeline for ScanSnap AI (Smart Vendor AI)
Fine-tunes YOLOv11 nano on grocery datasets (including Bourbon biscuit, Nivea, etc.)
and automatically deploys the best checkpoint to backend/models/best.pt.

Usage:
    python train_custom_yolo.py --epochs 25 --batch 8 --imgsz 416 --export_best
"""
import argparse
import os
import shutil
import sys
from pathlib import Path

def train_yolo(
    data_yaml: str = "dataset/grocery/pilot_batch_001_yolo/data.yaml",
    base_model: str = "yolo11n.pt",
    epochs: int = 30,
    batch_size: int = 8,
    img_size: int = 416,
    device: str = "cpu",
    project_name: str = "runs/detect",
    run_name: str = "grocery_custom_yolo",
    export_best: bool = True
):
    try:
        from ultralytics import YOLO
    except ImportError:
        print("❌ Error: ultralytics is not installed. Run: pip install ultralytics")
        sys.exit(1)

    ai_dir = Path(__file__).resolve().parent
    data_path = ai_dir / data_yaml
    base_weights = ai_dir / base_model

    if not data_path.exists():
        print(f"❌ Error: Dataset config not found at: {data_path}")
        sys.exit(1)

    if not base_weights.exists():
        print(f"⚠️ Base weights not found at {base_weights}, YOLO will download base model.")
        base_weights_str = "yolo11n.pt"
    else:
        base_weights_str = str(base_weights)

    print("=" * 70)
    print("🚀 ScanSnap AI: YOLO Custom Grocery Training Engine")
    print("=" * 70)
    print(f"  • Base Model:    {base_weights_str}")
    print(f"  • Dataset YAML:  {data_path}")
    print(f"  • Epochs:        {epochs}")
    print(f"  • Batch Size:    {batch_size}")
    print(f"  • Image Size:    {img_size}")
    print(f"  • Compute Device:{device}")
    print("=" * 70)

    model = YOLO(base_weights_str)

    results = model.train(
        data=str(data_path),
        epochs=epochs,
        batch=batch_size,
        imgsz=img_size,
        device=device,
        project=str(ai_dir / project_name),
        name=run_name,
        exist_ok=True,
        plots=True,
        save=True,
        verbose=True
    )

    best_weights_path = ai_dir / project_name / run_name / "weights" / "best.pt"
    if best_weights_path.exists():
        print(f"\n✅ Training completed successfully!")
        print(f"🎯 Best weights saved at: {best_weights_path}")

        if export_best:
            backend_model_dir = ai_dir.parent / "backend" / "models"
            backend_model_dir.mkdir(parents=True, exist_ok=True)
            target_pt = backend_model_dir / "best.pt"
            shutil.copy2(best_weights_path, target_pt)
            print(f"📦 Deployed best weights directly to backend: {target_pt}")
            print(f"   Now restart FastAPI server to load the new model with Bourbon support!")
    else:
        print(f"⚠️ Warning: best.pt was not found at {best_weights_path}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train custom YOLO model for ScanSnap AI")
    parser.add_argument("--data", type=str, default="dataset/grocery/pilot_batch_001_yolo/data.yaml", help="Path to data.yaml")
    parser.add_argument("--model", type=str, default="yolo11n.pt", help="Base model weights")
    parser.add_argument("--epochs", type=int, default=30, help="Number of training epochs")
    parser.add_argument("--batch", type=int, default=8, help="Batch size (e.g. 8 for CPU, 16/32 for GPU)")
    parser.add_argument("--imgsz", type=int, default=416, help="Image resolution for training")
    parser.add_argument("--device", type=str, default="cpu", help="Device: 'cpu' or '0' (for CUDA GPU)")
    parser.add_argument("--export_best", action="store_true", default=True, help="Automatically copy best.pt to backend/models/best.pt")

    args = parser.parse_args()
    train_yolo(
        data_yaml=args.data,
        base_model=args.model,
        epochs=args.epochs,
        batch_size=args.batch,
        img_size=args.imgsz,
        device=args.device,
        export_best=args.export_best
    )

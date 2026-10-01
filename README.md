# ScanSnap AI

ScanSnap AI is a comprehensive retail point-of-sale (POS) and inventory management system powered by edge-optimized computer vision. It leverages YOLO object detection, Google ML Kit, and an automated analytics engine to streamline checkout, stock tracking, and business intelligence for retailers.

## System Architecture

The application comprises a native Android client and a Python-based FastAPI backend, seamlessly integrated via REST APIs and secured using Firebase Authentication.

`mermaid
flowchart TD
    subgraph Android Client
        Camera[CameraX Scanner]
        LocalCart[Local Cart Engine]
        AuthClient[Firebase Auth SDK]
        Dashboard[Jetpack Compose UI]
    end

    subgraph FastAPI Backend
        API[REST API Gateway]
        YOLO[YOLOv8 Inference Engine]
        AuthMiddleware[JWT Verifier]
        DB[(SQLAlchemy Database)]
    end

    Camera -->|Upload Image + Token| API
    API -->|Validate Token| AuthMiddleware
    AuthMiddleware -->|Verify via Firebase| API
    API -->|Run Inference| YOLO
    YOLO -->|Predict Bounding Boxes| API
    API -->|Lookup Product & User Scope| DB
    DB -->|Return Product Data| API
    API -->|Return Detections| LocalCart
    LocalCart -->|Submit Checkout + Idempotency Key| API
    API -->|Atomic Stock Deduction| DB
    API -->|Generate Bill| DB
    DB -->|Aggregate Analytics| API
    API -->|Sync Data| Dashboard
`

## Key Features

### Dual-Mode Hybrid Scanner
- **Computer Vision Recognition:** Detects packaged goods instantly using fine-tuned lightweight YOLO models.
- **Physical Color Profile Verification:** HSV-space color signature checking ensures accurate classification and rejects out-of-domain false positives.
- **Barcode & 2D Code Scanning:** Built-in ML Kit fallback for high-density barcodes and QR codes.

### Smart Fast Billing
- Real-time cart management with quantity adjustments, auto-calculated tax, and one-tap checkout.
- Checkout idempotency guards against duplicate network requests and double billing.
- Atomic stock concurrency ensures accurate inventory deduction during simultaneous checkouts.
- Instant digital receipt generation with unique invoice IDs.

### Master Catalog & Inventory Management
- Pre-seeded database with popular retail items categorized by department.
- Real-time stock decrement upon sale completion.
- Automated Low-Stock Alert Badges when inventory dips below safety thresholds.
- Strict tenant data isolation ensuring secure user-scoped product and billing records.

### Sales Analytics & Retail Intelligence
- Revenue dashboard with customizable time filters (Today, Yesterday, Last 7 Days, Last 30 Days).
- Breakdown of Top Best-Selling Products by units sold and gross revenue.
- Visual sales velocity trends and transaction history logs.

## Technology Stack

| Layer | Technologies Used |
| :--- | :--- |
| **Mobile App (Frontend)** | Android (Kotlin), Jetpack Compose, CameraX, Coroutines, Retrofit2, OkHttp3 |
| **Computer Vision & AI** | YOLOv8 / YOLO11 (Ultralytics), PyTorch, OpenCV, Google ML Kit |
| **Backend API** | FastAPI (Python 3.10+), Pydantic v2, Uvicorn, SQLAlchemy ORM |
| **Database** | SQLite (Development) / PostgreSQL (Production) |
| **Security** | Firebase Auth (JWT), Idempotency Keys, Tenant Query Scoping |

## Repository Structure

`
Smart-vendor-AI/
+-- AI/                          # Model training scripts, weights & datasets
¦   +-- yolo11n.pt               # YOLOv11 base weights
¦   +-- yolo26n.pt               # Fine-tuned model checkpoint
¦   +-- requirements.txt         # AI pipeline dependencies
¦   +-- dataset/                 # Training and validation annotations
+-- backend/                     # FastAPI backend application
¦   +-- main.py                  # Server entry point & CORS configuration
¦   +-- database.py              # SQLAlchemy DB session & engine
¦   +-- models.py                # Database tables (Product, Bill, StoreProfile, MasterCatalog)
¦   +-- schemas.py               # Pydantic validation schemas
¦   +-- auth.py                  # Authentication & JWT dependency
¦   +-- seed_6000_indian_products.py # Pre-seeded catalog initialization script
¦   +-- models/best.pt           # Deployed YOLO inference model
¦   +-- routers/
¦       +-- detect.py            # YOLO inference + HSV color signature verification
¦       +-- products.py          # Inventory CRUD endpoints
¦       +-- bills.py             # Billing & checkout engine
¦       +-- analytics.py         # Business intelligence & revenue analytics
¦       +-- catalog.py           # Master catalog search & lookup
¦       +-- store.py             # Store profile & configuration
+-- Frontend/                    # Native Android Mobile Application
    +-- app/src/main/java/com/smartvendor/ai/
    ¦   +-- activities/          # MainActivity & SplashActivity
    ¦   +-- ai/                  # DetectionOverlayView, Classifier, Utils
    ¦   +-- network/             # Retrofit API clients & endpoints
    ¦   +-- repository/          # Repository data layer
    ¦   +-- ui/screens/          # Compose Screens (Scan, Billing, Inventory, Dashboard, Reports)
    +-- build.gradle.kts         # Android Gradle build scripts
    +-- AndroidManifest.xml      # Android configuration
`

## Getting Started

### 1. Backend Setup
`ash
# Navigate to backend directory
cd backend

# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows:
.\venv\Scripts\activate
# On Linux/macOS:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Seed the master catalog
python seed_6000_indian_products.py

# Start the FastAPI server
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
`
API Documentation will be accessible at: http://localhost:8000/docs

### 2. Android App Setup
1. Open the Frontend directory in **Android Studio** (Hedgehog or newer).
2. The application utilizes a dynamic Gradle Build Config for networking. To deploy locally, verify the base URL in Frontend/app/build.gradle.kts.
3. Connect your Android device via USB (with Developer Options & USB Debugging enabled) or start an Emulator with camera support.
4. Build and deploy to the device.

## License
Licensed under the [MIT License](LICENSE).

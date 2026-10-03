# 🚀 ScanSnap AI — Pitch Deck & Live Demo Guide

> **The AI-Powered Smart Retail Operating System for India's 12 Million Kirana Stores**

---

## 📋 Table of Contents

1. [Elevator Pitch](#-elevator-pitch)
2. [The Problem](#-the-problem)
3. [Our Solution](#-our-solution)
4. [Product Overview](#-product-overview)
5. [Complete Tech Stack](#-complete-tech-stack)
6. [System Architecture](#-system-architecture)
7. [Feature Breakdown (A to Z)](#-feature-breakdown-a-to-z)
8. [AI / ML Pipeline Deep Dive](#-ai--ml-pipeline-deep-dive)
9. [Live Demo Script](#-live-demo-script-step-by-step)
10. [Deployment & Infrastructure](#-deployment--infrastructure)
11. [Database & Data Strategy](#-database--data-strategy)
12. [Security & Authentication](#-security--authentication)
13. [Competitive Advantages](#-competitive-advantages)
14. [Business Model & Market](#-business-model--market)
15. [Roadmap](#-roadmap)
16. [Team & Repository](#-team--repository)

---

## 🎯 Elevator Pitch

> **ScanSnap AI** is a full-stack, AI-powered smart retail platform that transforms any Indian kirana store into an intelligent, connected retail operation. Point your phone at a product — our custom-trained YOLOv11 vision model instantly identifies it, pulls pricing from a 117K+ product master catalog, and generates a GST-compliant bill in under 2 seconds. When a product is out of stock, our AI Chat Assistant locates it at nearby partner stores and lets you order it in one tap — creating India's first **inter-store commerce network** for unorganized retail.

**One-liner:** *"Scan. Bill. Sell. Smarter."*

---

## 🔴 The Problem

India's unorganized retail sector faces critical operational bottlenecks:

| Problem | Impact |
|---|---|
| **Manual billing** — handwritten receipts, no digital trail | Lost revenue, tax non-compliance, zero analytics |
| **Zero inventory visibility** — shopkeepers don't know what's in stock | Customers leave empty-handed, lost sales |
| **No product identification tech** — barcode scanners are expensive and fragile | Slow checkout, human errors in pricing |
| **Isolated stores** — each kirana operates as a silo | Cannot source products from nearby stores when out of stock |
| **No data-driven decisions** — no sales trends, no demand forecasting | Overstocking some items, understocking fast-movers |
| **Festival pricing chaos** — manual discount calculations during Diwali, Holi, etc. | Pricing errors, margin erosion |

**Market Size:** 12M+ kirana stores in India, contributing ~80% of India's $900B retail market. Less than 5% have any digital POS system.

---

## 💡 Our Solution

**ScanSnap AI** is a **3-platform ecosystem** that digitizes the entire retail operation:

```
┌─────────────────────────────────────────────────────────────┐
│                    ScanSnap AI Ecosystem                     │
├───────────────────┬────────────────────┬────────────────────┤
│   📱 Android App   │   🖥️ Admin Portal   │   ⚙️ Backend API    │
│  (Kotlin/Compose) │   (React + Vite)   │  (FastAPI + YOLO)  │
├───────────────────┼────────────────────┼────────────────────┤
│ • Camera Scan     │ • Revenue Dashboard│ • YOLOv11 Detection│
│ • Barcode + OCR   │ • Inventory CRUD   │ • ORB Classifier   │
│ • Instant Billing │ • Bills Archive    │ • NLP Chat Engine  │
│ • AI Chat         │ • AI Vision Studio │ • Analytics Engine  │
│ • Inter-store     │ • Festival Offers  │ • 117K Catalog DB   │
│   Ordering        │ • Settings & GST   │ • Inter-store API   │
└───────────────────┴────────────────────┴────────────────────┘
```

---

## 🛍️ Product Overview

### Platform 1: 📱 Android Mobile App (Store Owner's Daily Tool)
- **Language:** Kotlin with Jetpack Compose (Material 3)
- **Camera:** CameraX for real-time product detection
- **Key Screens:** Login → Dashboard → Scan → Billing → Inventory → AI Chat → Reports → History → Settings

### Platform 2: 🖥️ Admin Portal (Business Intelligence Dashboard)
- **Framework:** React 19 + Vite 8
- **Deployed on:** Vercel (static SPA)
- **Key Pages:** Dashboard (Command Center) → Inventory Studio → Bills Archive → Offers & Combos → AI Vision Studio → Settings

### Platform 3: ⚙️ Backend API (AI + Data Engine)
- **Framework:** FastAPI (Python 3.11)
- **Deployed on:** Railway (Docker container)
- **ML Models:** YOLOv11 (custom-trained) + ORB Feature Matcher + HSV Color Validator
- **Database:** SQLite (dev) / PostgreSQL (production via Railway)

---

## 🛠️ Complete Tech Stack

### Frontend — Android App
| Technology | Purpose |
|---|---|
| **Kotlin** | Primary language |
| **Jetpack Compose** | Declarative UI framework (Material 3 Design) |
| **CameraX** | Real-time camera feed for product scanning |
| **Google ML Kit — Barcode Scanning** | Hardware-accelerated barcode/QR code reading |
| **Google ML Kit — Text Recognition (OCR)** | On-device text extraction from product packaging |
| **TensorFlow Lite** | On-device ML model inference capability |
| **Firebase Auth** | Email/password + Google Sign-In authentication |
| **Firebase Firestore** | Real-time cloud database for user profiles |
| **Firebase Storage** | Product image uploads |
| **Retrofit 2 + OkHttp** | REST API client for backend communication |
| **Coil** | Async image loading & caching |
| **Kotlin Coroutines** | Asynchronous programming |
| **Navigation Compose** | Type-safe screen navigation |

### Frontend — Admin Portal
| Technology | Purpose |
|---|---|
| **React 19** | UI component framework |
| **Vite 8** | Ultra-fast build tool & dev server |
| **Lucide React** | Modern icon library |
| **Vanilla CSS** | Custom design system with glassmorphism effects |
| **Vercel** | Static deployment with CDN edge network |

### Backend — API & ML Engine
| Technology | Purpose |
|---|---|
| **FastAPI** | High-performance Python REST API (async capable) |
| **Uvicorn** | ASGI server for production |
| **SQLAlchemy 2.0** | ORM for database operations |
| **Pydantic v2** | Request/response validation & serialization |
| **Ultralytics YOLOv11** | Custom-trained object detection model |
| **OpenCV (cv2)** | ORB keypoint feature matching & image processing |
| **Pillow (PIL)** | Image preprocessing, EXIF correction, thumbnailing |
| **NumPy** | HSV color signature analysis for packaging validation |
| **Firebase Admin SDK** | Server-side JWT token verification |
| **SQLite + WAL mode** | Local development database |
| **PostgreSQL** | Production database (Railway managed) |
| **Docker** | Containerized deployment (Python 3.11-slim base) |
| **Railway** | Cloud PaaS for backend hosting |

### AI / ML Stack
| Technology | Purpose |
|---|---|
| **YOLOv11n** | Custom-trained real-time object detection (17 product classes) |
| **ORB (Oriented FAST and Rotated BRIEF)** | Keypoint feature matching against authentic packshot dataset |
| **Lowe's Ratio Test** | False-positive elimination for feature matching |
| **HSV Color Signature Analysis** | Packaging color profile verification (sub-millisecond) |
| **Aspect Ratio Geometric Validation** | Physical packaging shape plausibility check |
| **IoU + Containment-based NMS** | Multi-pass deduplication and cross-class suppression |
| **NLP Intent Extraction** | Rule-based intent classification for AI chat |

### Infrastructure & DevOps
| Technology | Purpose |
|---|---|
| **Railway** | Backend PaaS (auto-deploy from GitHub, Docker support) |
| **Vercel** | Admin Portal CDN deployment |
| **GitHub** | Version control & CI/CD trigger |
| **Docker** | Backend containerization |
| **Firebase** | Auth + Realtime database + Analytics |

---

## 🏗️ System Architecture

```
                    ┌──────────────────────────────────────┐
                    │          Firebase Cloud               │
                    │  ┌─────────┐  ┌─────────┐  ┌──────┐ │
                    │  │  Auth   │  │Firestore│  │Storage│ │
                    │  └────┬────┘  └────┬────┘  └───┬──┘ │
                    └───────┼────────────┼───────────┼─────┘
                            │            │           │
          ┌─────────────────┼────────────┼───────────┼─────────────────┐
          │                 │ JWT Token  │           │                 │
          ▼                 ▼            ▼           ▼                 ▼
   ┌──────────────┐   ┌─────────────────────────────────┐   ┌──────────────┐
   │  📱 Android   │   │    ⚙️ FastAPI Backend (Railway)  │   │  🖥️ Admin     │
   │  Mobile App   │◄─►│                                 │◄─►│  Portal      │
   │  (Kotlin)     │   │  ┌───────┐  ┌────────────────┐ │   │  (React)     │
   │               │   │  │ YOLO  │  │ Product Router │ │   │  (Vercel)    │
   │ • CameraX     │   │  │ v11n  │  │ Bill Router    │ │   │              │
   │ • ML Kit      │   │  │detect │  │ Analytics      │ │   │ • Dashboard  │
   │ • Barcode     │   │  └───────┘  │ Store Router   │ │   │ • Inventory  │
   │ • OCR         │   │  ┌───────┐  │ Catalog Router │ │   │ • AI Studio  │
   │ • AI Chat     │   │  │  ORB  │  │ Offers Router  │ │   │ • Offers     │
   │ • Billing     │   │  │Classif│  │ AI Router      │ │   │ • Bills      │
   └──────────────┘   │  └───────┘  └────────────────┘ │   └──────────────┘
                      │  ┌──────────────────────────┐   │
                      │  │  SQLite / PostgreSQL DB   │   │
                      │  │  • Products (inventory)   │   │
                      │  │  • Bills (transactions)   │   │
                      │  │  • StoreProfiles (GPS)    │   │
                      │  │  • MasterCatalog (117K)   │   │
                      │  └──────────────────────────┘   │
                      └─────────────────────────────────┘
```

**API Base URL (Production):** `https://scansnapai-production.up.railway.app`

---

## ⭐ Feature Breakdown (A to Z)

### 1. 🔍 AI Product Detection (YOLOv11 Vision)
**What it does:** Point your phone camera at any product on the shelf — our custom-trained YOLOv11 model identifies it in real-time with bounding boxes and confidence scores.

**How it works:**
- Camera frame captured via CameraX → sent as base64/multipart to `/detect/image` or `/detect/base64`
- YOLOv11 runs inference at `imgsz=640` with confidence thresholds
- Multi-pass deduplication (IoU + containment-based NMS) merges overlapping detections
- **3-layer verification pipeline:**
  1. **Geometric Aspect Ratio Check** — rejects physically impossible shapes
  2. **HSV Color Signature Validation** — verifies packaging color profiles (sub-millisecond)
  3. **ORB Feature Matching** — cross-references against authentic packshot dataset using Lowe's ratio test

**Trained Classes (17 Products):**
| # | Class | Friendly Name |
|---|---|---|
| 1 | `amul_ice_cream` | Amul Ice Cream Cup Vanilla Magic 100ml |
| 2 | `cake` | Britannia Cake Gobbles Choco Chill 65g |
| 3 | `cerave` | CeraVe Hydrating Cleanser 236ml |
| 4 | `hns_shampoo` | Head & Shoulders Cool Menthol Shampoo 180ml |
| 5 | `nestle_milk_powder` | Nestle Everyday Dairy Whitener 20g |
| 6 | `plum` | Plum Green Tea Pore Cleansing Face Wash 100ml |
| 7 | `thums_up` | Thums Up Charged Carbonated Beverage 250ml |
| 8 | `wild_stone` | Wild Stone Forest Spice Deodorant Soap 125g |
| 9 | `nivea_deodorant` | Nivea Men Fresh Active Deodorant 150ml |
| 10 | `bourbon_biscuit` | Britannia Bourbon Chocolate Biscuits |
| 11 | `milky_biscuit` | Britannia Milk Bikis Biscuits |
| 12 | `maggi` | Maggi 2-Minute Masala Noodles |
| 13 | `surf_excel` | Surf Excel Easy Wash Detergent |
| 14 | `hide_and_seek` | Parle Hide & Seek Choco Chip Biscuits |
| 15 | `oreo` | Cadbury Oreo Original Biscuits |
| 16 | `appe_fizz` | Appy Fizz Sparkling Apple Juice |
| 17 | `jim_jam` | Britannia Treat Jim Jam Biscuits |

**Demo Flow:**
> Hold a Maggi packet, Bourbon biscuit, or Thums Up can in front of the camera → watch real-time bounding boxes appear with product name + confidence % → tap to add to bill.

---

### 2. 📷 Barcode Scanning (ML Kit)
**What it does:** Instant barcode/QR code scanning using Google ML Kit's on-device processing. Matches against the **117,000+ Indian product master catalog**.

**How it works:**
- ML Kit processes the camera feed on-device (no network needed for scan)
- Detected barcode string → sent to `/products/barcode/{barcode}` API
- 3-tier lookup: User's inventory → Any store inventory → Master Catalog (117K products)

**Demo Flow:**
> Scan any Indian FMCG barcode → product details appear instantly with name, price, category, stock.

---

### 3. 📝 OCR Text Recognition (ML Kit)
**What it does:** Reads text directly from product packaging using Google ML Kit's text recognition. Extracts product names, MRP, weight, ingredients, and more.

**How it works:**
- On-device ML Kit text recognition processes camera frames
- Extracted text blocks displayed in real-time overlay
- Can be used as fallback when barcode is damaged or YOLO doesn't recognize the product

---

### 4. 🧾 Smart Billing & POS System
**What it does:** Full point-of-sale billing system. Products detected by AI/barcode/OCR are added to a cart, and a GST-compliant bill is generated with one tap.

**How it works:**
- Bill created via `POST /bills` with items, quantities, payment mode
- Authoritative pricing from DB (overrides client-side prices for integrity)
- Auto-deducts stock from inventory on bill creation
- Idempotency key prevents duplicate bills from network retries
- Assigns realistic customer profiles if not provided
- Supports **Cash, UPI, and Card** payment modes

**Key Features:**
- Real-time stock deduction on sale
- Bill number generation (INV-XXXXXXXX format)
- Tax amount calculation
- Customer name & phone tracking
- Bill history with search & filtering

**Demo Flow:**
> Scan 2-3 products → review cart → select UPI payment → generate bill → show the invoice with line items, totals, and tax.

---

### 5. 🤖 AI Chat Assistant (NLP-Powered)
**What it does:** Natural language interface for store owners to query inventory, check product availability, find out-of-stock items at nearby stores, and place inter-store orders.

**How it works:**
- User types a message → `POST /ai/chat`
- Rule-based NLP intent extraction classifies the message:
  - `FIND_ELSEWHERE` — "Where else can I find Maggi?"
  - `PRODUCT_AVAILABILITY` — "Is Maggi available?"
  - `PRODUCT_PRICE` — "What's the price of Oreo?"
  - `CATEGORY_SEARCH` — "Show me all snacks"
- For out-of-stock products, automatically queries nearby partner stores with **GPS-based distance sorting (Haversine formula)**
- Returns `nearby_options` with store name, price, distance, stock, and last update time

**Supported Queries:**
```
"Is Maggi available?"
"Where else can I find Maggi?"
"What's the price of Oreo?"
"Show me all snacks"
"Do you have any dairy products?"
```

**Demo Flow:**
> Type "Is Maggi available?" → AI responds "Out of stock (0 available)" → automatically shows nearby stores with interactive cards → tap "Add to Cart" from cheapest store → inter-store order placed!

---

### 6. 🏬 Inter-Store Commerce Network
**What it does:** India's first inter-store ordering system for kirana stores. When a product is out of stock, order it from a nearby partner store in one tap.

**How it works:**
- AI Chat detects out-of-stock → surfaces nearby options with interactive cards
- User selects a store → `POST /ai/order` creates an inter-store order
- Stock is deducted from the partner store's inventory
- Order ID generated (ORD-XXXXXXXX format)
- Confirmation message with delivery/pickup note

**Demo Data (Pre-seeded):**
| Store | Distance | Maggi Price | Stock |
|---|---|---|---|
| Krishna Supermarket | 0.5 km | ₹14.00 | 42 |
| Apna Bazaar Mart | 1.6 km | ₹13.50 | 75 |
| Reliance Smart Point | 2.6 km | ₹14.00 | 110 |

---

### 7. 📊 Analytics & Business Intelligence
**What it does:** Real-time sales analytics dashboard with revenue metrics, top products, payment breakdown, stock recommendations, and cross-vendor market trends.

**Endpoint:** `GET /analytics/summary?days=30`

**Data Points:**
- **Total Revenue** — sum of all bills in the date range
- **Total Bills** — transaction count
- **Average Bill Value** — revenue / bills
- **Payment Breakdown** — Cash vs UPI vs Card split
- **Top 5 Products** — by quantity sold
- **Daily Revenue Chart** — day-by-day revenue graph
- **AI Stock Recommendations** — urgency-prioritized restock alerts
- **Cross-Vendor Market Trends** — what's selling across ALL stores on the platform

**Smart AI Stock Recommendations:**
| Badge | Meaning |
|---|---|
| 🚨 Out of Stock | Product has 0 units, immediate restock |
| ⚠️ Low Stock | Below safety threshold |
| ⚡ Sudden Surge | Week-over-week sales spike detected (e.g., +150%) |
| 🔥 Fast Moving | High daily sales velocity |
| ⏰ Peak Hour | Sales concentrated during specific time windows |
| 📈 Steady Demand | Consistent weekly sales |

**Peak Hour Detection:**
- 🌅 Morning Rush (6-11 AM)
- ☀️ Afternoon Demand (12-4 PM)
- 🌆 Evening Peak (5-9 PM)
- 🌙 Night Demand (10 PM-5 AM)

---

### 8. 🎉 Festival Offers & Dynamic Pricing Engine
**What it does:** Automated festival discounts for 9 major Indian festivals + smart combo deal suggestions.

**Festival Calendar:**
| Festival | Dates | Discount |
|---|---|---|
| 🇮🇳 Republic Day Special | Jan 24-26 | 10% |
| 🎨 Holi Festival of Colors | Mar 10-16 | 20% |
| 🎁 Raksha Bandhan | Aug 7-11 | 12% |
| 🇮🇳 Independence Day | Aug 13-16 | 10% |
| 🌺 Ganesh Chaturthi | Aug 25-30 | 18% |
| 🏹 Dussehra Festive Bonanza | Sep 28 - Oct 5 | 20% |
| 🪔 Diwali Grand Dhamaka | Oct 15-26 | **25%** |
| 🎄 Christmas Joy Sale | Dec 23-26 | 10% |
| 🎉 New Year Kickoff | Dec 30 - Jan 2 | 12% |
| 🛍️ Default (always-on) | Year-round | 5% |

**Smart Combo Offers:**
| Combo | Products | Discount |
|---|---|---|
| Chilled Snack Combo | Amul Ice Cream + Thums Up | 3% |
| Morning Breakfast Combo | Nestle Milk Powder + Britannia Cake | 4% |
| Men's Fresh Grooming | Wild Stone + Head & Shoulders | 8% |
| Premium Personal Care Trio | H&S + Plum + CeraVe | **15%** |

**Key Feature:** The offers engine evaluates your cart in real-time. If you have 1 of 2 combo items, it **suggests** adding the missing item to unlock the deal.

**Manual Festival Override (for demo):**
```
POST /offers/override-festival
{ "festival_name": "Diwali Grand Dhamaka" }
```

---

### 9. 📦 Inventory Management (Full CRUD)
**What it does:** Complete product inventory management with create, read, update, delete operations. Supports barcode lookup, category filtering, and stock level management.

**API Endpoints:**
| Method | Endpoint | Action |
|---|---|---|
| `GET` | `/products` | List all products (with search/category filters) |
| `POST` | `/products` | Add new product |
| `GET` | `/products/{id}` | Get product details |
| `PUT` | `/products/{id}` | Update product |
| `PUT` | `/products/{id}/stock` | Update stock level |
| `DELETE` | `/products/{id}` | Delete product |
| `GET` | `/products/barcode/{barcode}` | Lookup by barcode (3-tier) |
| `GET` | `/products/{id}/nearby` | Find same product at nearby stores |

**Auto-Seeding:** When a new store owner opens the app for the first time, 65+ sample Indian products are automatically seeded into their inventory across categories: Snacks, Chocolates, Instant Food, Groceries, Dairy, Oils, Breakfast, Cleaning, Hygiene, Beverages, Spices, Skincare, Personal Care.

---

### 10. 🏪 Store Profile & GPS Location
**What it does:** Store identity management with geo-location for distance-based nearby store features.

**Fields:** Store name, address, phone, GST number, UPI ID, latitude, longitude.

**GPS Integration:** The Haversine formula calculates real-world distances between stores for the "Find Elsewhere" and "Nearby Stores" features.

---

### 11. 🔍 Master Catalog (117,000+ Indian Products)
**What it does:** A comprehensive database of 117K+ Indian FMCG/grocery products with names, categories, suggested prices, and barcodes. Acts as the "knowledge base" when a product isn't yet in the store's inventory.

**Endpoint:** `GET /catalog/search?q=maggi`

**How it's used:**
- Barcode lookup fallback (when product isn't in store inventory)
- YOLO detection product matching
- Bill creation auto-seeding (unknown product? check master catalog first)

---

### 12. 🖥️ AI Vision Studio (Admin Portal)
**What it does:** A web-based testing lab for the YOLOv11 detection model. Upload any product image and see the full detection pipeline in action — bounding boxes, confidence scores, color analysis, and product matching.

**Features:**
- Drag & drop image upload
- Real-time detection results with visual bounding boxes
- Confidence score visualization
- Product match information from database
- Hackathon barcode test lab

---

## 🧠 AI / ML Pipeline Deep Dive

### Detection Pipeline (3-Stage Verification)

```
Camera Frame → [Stage 1: YOLOv11 Inference]
                    ↓
              Raw Detections
                    ↓
            [Stage 2: Smart NMS]
            • IoU-based dedup
            • Same-class box merging
            • Cross-class suppression
            • Multi-pass chain merging
                    ↓
            Deduplicated Candidates
                    ↓
            [Stage 3: 3-Layer Verification]
            ├── Geometric Aspect Ratio Check
            │   (Is this box physically plausible?)
            ├── HSV Color Signature Validation
            │   (Does the packaging color match?)
            └── ORB Feature Matching (Fallback)
                (Keypoint match against packshot dataset)
                    ↓
            Verified Detections + DB Product Match
```

### Custom YOLO Training Pipeline

```
Raw Product Images (Dataset/)
    ↓
[build_augmented_dataset.py]
    → Rotation, flipping, brightness, noise augmentations
    → YOLO annotation format (normalized bbox)
    ↓
[train_custom_yolo.py]
    → Base model: yolo11n.pt (YOLOv11 Nano)
    → Custom training on 17 Indian FMCG classes
    → Output: models/best.pt
    ↓
[test_detection_suite.py]
    → Validation & accuracy metrics
```

### ORB Feature Matcher (Retail Classifier)

```
Camera Crop → ORB keypoint extraction (750 features)
    ↓
For each product class in Dataset/:
    → Load all authentic packshot descriptors
    → BFMatcher (Hamming distance)
    → Lowe's Ratio Test (0.75 threshold)
    → Count good matches
    ↓
Best class with matches >= 50 → Classification
```

### NLP Chat Intent Engine

```
User Message: "Where else can I find Maggi?"
    ↓
[extract_intent_and_entities()]
    → Keyword matching against intent categories
    → Stop-word removal
    → Entity extraction (product name / category)
    ↓
Intent: FIND_ELSEWHERE
Entity: { product_name: "maggi" }
    ↓
[handle_chat_request()]
    → Query partner stores for matching products
    → Haversine distance calculation
    → Sort by distance, then price
    → Return nearby_options[] with Add-to-Cart cards
```

---

## 🎬 Live Demo Script (Step by Step)

### Pre-Demo Setup
1. **Backend is live at:** `https://scansnapai-production.up.railway.app`
   - Verify: Visit `https://scansnapai-production.up.railway.app/docs` for Swagger UI
2. **Admin Portal is live at:** Vercel deployment URL
3. **Android App:** Install the APK on demo phone, sign in with Firebase

### Demo Flow (10-Minute Pitch)

---

#### 🔹 ACT 1: The Problem (1 min)
> *"Imagine you walk into a kirana store. The shopkeeper writes your bill on paper. He doesn't know what's in stock. He can't tell you if the nearby store has Maggi. This is the reality for 12 million stores in India."*

---

#### 🔹 ACT 2: AI Product Detection (2 min)

**On the Android phone:**
1. Open ScanSnap AI app → Tap **Scan** on the dashboard
2. Point camera at a **Maggi packet**
   - 🎯 Show the real-time bounding box appearing with "Maggi 2-Minute Masala Noodles" and confidence (e.g., 92%)
3. Point at a **Bourbon biscuit** packet
   - 🎯 Show second detection alongside the first
4. Tap a detection → it auto-fills the billing cart with product name, price, and quantity

> *"Our custom YOLOv11 model, trained on Indian FMCG products, identifies products in under 200ms. No barcode needed."*

**Also demonstrate Barcode scanning:**
5. Tap the **Barcode** mode toggle
6. Scan any product barcode → show it matching from the 117K catalog

---

#### 🔹 ACT 3: Smart Billing (1.5 min)

1. With 2-3 products in the cart, review the **Billing Screen**
2. Show line items with quantities, unit prices, subtotals
3. Select **UPI** as payment mode
4. Tap **Generate Bill**
5. Show the **Checkout Success** screen with invoice number, total, tax breakdown
6. **Key point:** Stock was automatically deducted in real-time!

> *"One scan. One tap. GST-compliant digital bill. Stock auto-updated across the system."*

---

#### 🔹 ACT 4: AI Chat — "Where else can I find Maggi?" (2 min)

**This is the WOW moment! 🚀**

1. Open the **AI Chat** screen on the Android app
2. Type: **"Is Maggi available?"**
   - AI responds: *"Maggi 2-Minute Noodles is out of stock (0 available)"*
   - Automatically shows nearby stores with interactive cards!
3. Show the 3 nearby stores:
   - **Krishna Supermarket** — Rs.14, 0.5 km away, 42 in stock
   - **Apna Bazaar Mart** — Rs.13.50, 1.6 km, 75 in stock (cheapest!)
   - **Reliance Smart Point** — Rs.14, 2.6 km, 110 in stock
4. Tap **"Add to Cart"** on Apna Bazaar (cheapest option)
5. Tap **Checkout** → Inter-store order placed!
   - Order confirmed: `ORD-A3B7C9D1`

> *"When your store is out of stock, our AI finds it at nearby partner stores and lets you order in one tap. This creates India's first inter-store commerce network for kirana stores."*

---

#### 🔹 ACT 5: Admin Dashboard — Business Intelligence (2 min)

**Switch to the Admin Portal (laptop/browser):**

1. Open the **Dashboard** (Store Command Center)
   - Show total revenue, bill count, average bill value
   - Show the payment breakdown pie chart (Cash vs UPI vs Card)
   - Show live invoice monitor with recent bills
2. Navigate to **Inventory Studio**
   - Show the product grid with stock levels, categories
   - Show low-stock alerts (red badges)
3. Navigate to **Reports/Analytics**
   - Show **AI Stock Recommendations** with urgency badges:
     - 🚨 "Maggi is OUT OF STOCK! Immediate restock required"
     - ⚡ "Bourbon has Sudden Surge (+150% vs last week)"
   - Show **Cross-Vendor Market Trends**
   - Show **Peak Hour** analysis

> *"Data-driven decisions. Know what to restock, when, and how much — powered by AI analytics."*

---

#### 🔹 ACT 6: Festival Offers (1 min)

1. Open the **Offers & Dynamic Pricing** page
2. Show the active festival offer (or override to Diwali for demo):
   ```
   POST /offers/override-festival
   { "festival_name": "Diwali Grand Dhamaka" }
   ```
3. Show the 25% Diwali discount being applied to a cart
4. Show **Combo Suggestions**: "Add Thums Up to get the Chilled Snack Combo — 3% extra off!"

> *"Automatic festival pricing for 9 Indian festivals. Smart combo upselling. Zero manual effort."*

---

#### 🔹 ACT 7: AI Vision Studio (30 sec)

1. Open **AI Vision Studio** in Admin Portal
2. Drag & drop a product image
3. Show YOLO detection results with bounding boxes and confidence scores

---

#### 🔹 ACT 8: Close (1 min)
> *"ScanSnap AI transforms a Rs.2 lakh/year kirana store into a smart, connected, AI-powered retail operation. One app. Zero hardware. Infinite potential."*

---

## 🚀 Deployment & Infrastructure

### Backend (Railway)
| Property | Value |
|---|---|
| **Platform** | Railway (Docker-based PaaS) |
| **URL** | `https://scansnapai-production.up.railway.app` |
| **Dockerfile** | Python 3.11-slim + OpenCV system deps |
| **Auto-Deploy** | On push to `main` branch |
| **Environment Variables** | `DATABASE_URL`, `FIREBASE_CREDENTIALS_BASE64`, `ALLOWED_ORIGINS`, `ENVIRONMENT` |
| **Model** | `models/best.pt` (YOLOv11 custom) bundled in Docker image |
| **Health Check** | `GET /health` → `{"status": "healthy"}` |
| **API Docs** | `GET /docs` → Swagger UI |

### Admin Portal (Vercel)
| Property | Value |
|---|---|
| **Platform** | Vercel (static SPA hosting) |
| **Framework** | Vite 8 + React 19 |
| **Build Command** | `vite build` |
| **SPA Routing** | `vercel.json` rewrites all routes to `index.html` |
| **CDN** | Vercel Edge Network (global) |

### Android App
| Property | Value |
|---|---|
| **Min SDK** | 24 (Android 7.0 Nougat) |
| **Target SDK** | 35 (Android 15) |
| **Build** | Gradle + Kotlin DSL |
| **API URL** | `https://scansnapai-production.up.railway.app/` (hardcoded in `build.gradle.kts`) |
| **Distribution** | APK sideload or Play Store |

---

## 💾 Database & Data Strategy

### Schema

```
┌─────────────────┐     ┌──────────────────┐     ┌───────────────┐
│    products      │     │      bills        │     │  bill_items    │
├─────────────────┤     ├──────────────────┤     ├───────────────┤
│ id (PK)         │     │ id (PK)          │     │ id (PK)       │
│ user_id (FK)    │     │ user_id          │     │ bill_id (FK)  │
│ name            │     │ customer_name    │     │ product_id(FK)│
│ barcode         │     │ customer_phone   │     │ product_name  │
│ category        │     │ total_amount     │     │ quantity      │
│ price           │     │ tax_amount       │     │ unit_price    │
│ stock           │     │ payment_mode     │     │ total_price   │
│ low_stock_thresh│     │ created_at       │     └───────────────┘
│ image_url       │     └──────────────────┘
│ created_at      │
│ updated_at      │     ┌──────────────────┐     ┌───────────────┐
└─────────────────┘     │  store_profiles   │     │master_catalog │
                        ├──────────────────┤     ├───────────────┤
                        │ user_id (PK)     │     │ id (PK)       │
                        │ name             │     │ name          │
                        │ address          │     │ category      │
                        │ phone            │     │ suggested_price│
                        │ gst              │     │ barcode       │
                        │ upi              │     └───────────────┘
                        │ latitude         │       117,000+ rows
                        │ longitude        │
                        │ updated_at       │
                        └──────────────────┘
```

### Data Scale
- **Master Catalog:** 117,000+ Indian products (seeded from curated datasets)
- **Sample Store Inventory:** 65+ products per store across 14 categories
- **Demo Partner Stores:** 3 nearby stores with GPS coordinates for inter-store flow
- **SQLite WAL Mode:** Write-Ahead Logging for concurrent read/write performance

---

## 🔐 Security & Authentication

| Layer | Implementation |
|---|---|
| **Mobile Auth** | Firebase Authentication (Email + Google Sign-In) |
| **API Auth** | Bearer JWT token verification via Firebase Admin SDK |
| **Token Flow** | Android → Firebase → JWT → FastAPI `get_current_user_id()` |
| **Fallback Auth** | Dev bypass with `DEV_AUTH_BYPASS=true` (disabled in production) |
| **Production Guard** | `ENVIRONMENT=production` disables debug endpoints (`/view-data`) |
| **CORS** | Strict `ALLOWED_ORIGINS` in production, permissive regex in dev |
| **Idempotency** | Bill creation uses UUID5 idempotency keys to prevent duplicate transactions |
| **Firebase Credentials** | Base64-encoded `serviceAccountKey.json` via environment variable |

---

## 🏆 Competitive Advantages

| Feature | ScanSnap AI | Traditional POS | Competitors |
|---|---|---|---|
| **AI Product Detection** | YOLOv11 (17 classes) | None | None |
| **Multi-Modal Input** | Camera + Barcode + OCR | Barcode only | Barcode only |
| **Inter-Store Commerce** | First in India | None | None |
| **AI Chat Assistant** | NLP-powered | None | None |
| **Festival Dynamic Pricing** | 9 festivals + combos | Manual | Limited |
| **117K Master Catalog** | Yes | None | Partial |
| **AI Stock Recommendations** | Peak-hour + surge detection | None | Basic |
| **Cross-Vendor Trends** | Platform-wide analytics | None | None |
| **Hardware Required** | None (just a phone) | Expensive POS terminal | Mixed |
| **Setup Time** | Less than 2 minutes | Days to weeks | Hours |

---

## 💰 Business Model & Market

### Target Audience
- **Primary:** 12M+ Indian kirana store owners
- **Secondary:** Small-medium retail chains, D2C brand distributors

### Revenue Model (Future)
1. **Freemium SaaS** — Free basic POS, paid AI features (Rs.299-999/month)
2. **Transaction Fees** — 0.5% on inter-store orders
3. **Data Insights** — Anonymized market trend reports for FMCG brands
4. **Advertising** — In-app product recommendations from brands
5. **Catalog Licensing** — API access to 117K product database

### TAM/SAM/SOM
- **TAM:** Rs.900B (India retail market)
- **SAM:** Rs.108B (12M kiranas x Rs.9K annual digitization spend)
- **SOM:** Rs.540M (Year 1: 60K stores x Rs.750/month)

---

## 🗺️ Roadmap

| Phase | Features | Timeline |
|---|---|---|
| **v1.0 (Current)** | AI Detection, Billing, Barcode, OCR, Analytics, Offers | Done |
| **v1.1 (Current)** | AI Chat, Inter-Store Commerce, Nearby Stores | Done |
| **v2.0** | Live inter-store delivery tracking, payment integration (Razorpay) | Q1 2027 |
| **v2.1** | Voice-based AI assistant (Hindi/Bengali), multi-language support | Q2 2027 |
| **v3.0** | Expand YOLO model to 100+ classes, supplier marketplace | Q3 2027 |
| **v3.1** | WhatsApp bot integration for customer orders | Q4 2027 |

---

## 👨‍💻 Team & Repository

| | |
|---|---|
| **Project** | ScanSnap AI |
| **Repository** | [github.com/Abhrxdip/Scansnap_Ai](https://github.com/Abhrxdip/Scansnap_Ai) |
| **Backend Live** | [scansnapai-production.up.railway.app](https://scansnapai-production.up.railway.app) |
| **API Documentation** | [scansnapai-production.up.railway.app/docs](https://scansnapai-production.up.railway.app/docs) |
| **License** | MIT |

---

## 📎 Quick API Reference (for Demo)

```bash
# Health check
curl https://scansnapai-production.up.railway.app/

# List YOLO classes
curl https://scansnapai-production.up.railway.app/detect/classes

# Search master catalog
curl https://scansnapai-production.up.railway.app/catalog/search?q=maggi

# Get analytics summary
curl https://scansnapai-production.up.railway.app/analytics/summary?days=30

# Get active festival
curl https://scansnapai-production.up.railway.app/offers/festivals

# Override festival (for demo)
curl -X POST https://scansnapai-production.up.railway.app/offers/override-festival \
  -H "Content-Type: application/json" \
  -d '{"festival_name": "Diwali Grand Dhamaka"}'

# AI Chat
curl -X POST https://scansnapai-production.up.railway.app/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Where else can I find Maggi?"}'

# Seed demo data (if needed)
curl https://scansnapai-production.up.railway.app/ai/seed
```

---

> **ScanSnap AI** — *Scan. Bill. Sell. Smarter.* 🚀

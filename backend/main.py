import os
import uuid
from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
import models
from database import engine, Base, get_db, SessionLocal

# Create all tables on startup
Base.metadata.create_all(bind=engine)

def run_migrations():
    from sqlalchemy import text
    with engine.connect() as conn:
        for col in ["customer_name", "customer_phone"]:
            try:
                conn.execute(text(f"ALTER TABLE bills ADD COLUMN {col} VARCHAR"))
                conn.commit()
            except Exception:
                pass
        for col in ["latitude", "longitude"]:
            try:
                conn.execute(text(f"ALTER TABLE store_profiles ADD COLUMN {col} FLOAT"))
                conn.commit()
            except Exception:
                pass
        try:
            conn.execute(text("UPDATE bills SET customer_name = 'Abhradeep Das' WHERE customer_name IS NULL OR customer_name = ''"))
            conn.execute(text("UPDATE bills SET customer_phone = '+91 98301 24510' WHERE customer_phone IS NULL OR customer_phone = ''"))
            conn.commit()
        except Exception:
            pass

run_migrations()

app = FastAPI(
    title="ScanSnap AI API",
    description="Backend API for ScanSnap AI — Billing, Inventory, Analytics, Store Profiles, and Master Catalog",
    version="1.0.0"
)

@app.on_event("startup")
def startup_event():
    run_migrations()
    try:
        from seed_demo_data import seed
        seed()
    except Exception as e:
        print(f"Notice: Demo data seeding: {e}")
    try:
        from routers.detect import _get_model
        _get_model()
    except Exception as e:
        print(f"Notice: Model warm-up skipped: {e}")


ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS")
if ALLOWED_ORIGINS:
    origins = [o.strip() for o in ALLOWED_ORIGINS.split(",")]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
elif os.getenv("ENVIRONMENT", "").lower() != "production":
    app.add_middleware(
        CORSMiddleware,
        allow_origin_regex=r"^https?://.*",
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

# Register routers
from routers import products, bills, analytics, store, catalog, detect, offers, ai
app.include_router(products.router)
app.include_router(bills.router)
app.include_router(analytics.router)
app.include_router(store.router)
app.include_router(catalog.router)
app.include_router(detect.router)
app.include_router(offers.router)
app.include_router(ai.router)



@app.get("/", tags=["Health"])
def root():
    return {
        "status": "ok",
        "app": "ScanSnap AI API",
        "version": "1.0.0",
        "docs": "/docs",
        "view_data": "/view-data"
    }


@app.get("/health", tags=["Health"])
def health():
    return {"status": "healthy"}


@app.get("/view-data", tags=["Database Inspection"])
def view_data(db: Session = Depends(get_db)):
    if os.getenv("ENVIRONMENT", "").lower() == "production":
        from fastapi import HTTPException
        raise HTTPException(status_code=403, detail="Endpoint disabled in production")
    products_list = db.query(models.Product).all()
    bills_list = db.query(models.Bill).all()

    return {
        "total_inventory_items": len(products_list),
        "total_completed_bills": len(bills_list),
        "inventory_products": [
            {
                "id": p.id,
                "name": p.name,
                "category": p.category,
                "price": p.price,
                "stock": p.stock,
                "barcode": p.barcode
            }
            for p in products_list
        ],
        "completed_bills": [
            {
                "bill_id": b.id,
                "total_amount": b.total_amount,
                "payment_mode": b.payment_mode,
                "created_at": str(b.created_at),
                "items": [
                    {
                        "product_name": item.product_name,
                        "quantity": item.quantity,
                        "unit_price": item.unit_price,
                        "total_price": item.total_price
                    }
                    for item in b.items
                ]
            }
            for b in bills_list
        ]
    }

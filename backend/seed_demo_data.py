"""
Seed demo stores and inventory for ScanSnap AI.
Sets up:
1. Primary demo store with Maggi (out of stock, stock=0) to trigger Find Elsewhere flow.
2. Three nearby partner stores with varying distances (0.5 km, 1.6 km, 2.6 km) and prices.
3. Automatically updates any existing store profiles in smartvendor.db so active phone users benefit immediately.
"""

from datetime import datetime, timedelta
import models
from database import engine, SessionLocal, Base

def seed():
    # 1. Ensure all tables exist
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    
    try:
        print("[SEED] Seeding ScanSnap AI demo data...")

        # 2. Check and seed demo stores
        stores_data = [
            {
                "user_id": "demo_user",
                "name": "ScanSnap Express Kirana",
                "address": "Shop #4, Brigade Road, Bangalore",
                "phone": "+91 9876543210",
                "latitude": 12.9716,
                "longitude": 77.5946
            },
            {
                "user_id": "store_nearby_1",
                "name": "Krishna Supermarket",
                "address": "MG Road, 500m from Metro, Bangalore",
                "phone": "+91 9811122233",
                "latitude": 12.9750,
                "longitude": 77.5980
            },
            {
                "user_id": "store_nearby_2",
                "name": "Apna Bazaar Mart",
                "address": "Residency Road, Bangalore",
                "phone": "+91 9822233344",
                "latitude": 12.9820,
                "longitude": 77.6050
            },
            {
                "user_id": "store_nearby_3",
                "name": "Reliance Smart Point",
                "address": "Commercial Street, Bangalore",
                "phone": "+91 9833344455",
                "latitude": 12.9900,
                "longitude": 77.6100
            }
        ]

        for s in stores_data:
            existing = db.query(models.StoreProfile).filter(models.StoreProfile.user_id == s["user_id"]).first()
            if not existing:
                store_obj = models.StoreProfile(
                    user_id=s["user_id"],
                    name=s["name"],
                    address=s["address"],
                    phone=s["phone"],
                    latitude=s["latitude"],
                    longitude=s["longitude"]
                )
                db.add(store_obj)
                print(f"  + Created store: {s['name']} ({s['user_id']})")
            else:
                existing.name = s["name"]
                existing.latitude = s["latitude"]
                existing.longitude = s["longitude"]
                print(f"  * Updated store: {s['name']} ({s['user_id']})")

        # Also, check any user already in StoreProfile (e.g. from physical device)
        all_stores = db.query(models.StoreProfile).all()
        for s in all_stores:
            if s.latitude is None or s.longitude is None:
                s.latitude = 12.9716
                s.longitude = 77.5946
                print(f"  * Assigned demo coordinates to active store: {s.name or s.user_id}")


        db.commit()

        # 3. Seed inventory
        # For demo_user (and any user in DB without Maggi), add Maggi with stock = 0
        target_store_ids = ["demo_user"]
        for s in all_stores:
            if s.user_id not in ["store_nearby_1", "store_nearby_2", "store_nearby_3"] and s.user_id not in target_store_ids:
                target_store_ids.append(s.user_id)

        for uid in target_store_ids:
            # Primary store products
            primary_products = [
                {"name": "Maggi 2-Minute Noodles 70g", "barcode": "8901058852394", "category": "Instant Food", "price": 14.0, "stock": 0}, # Out of stock!
                {"name": "Amul Taaza Homogenised Toned Milk 500ml", "barcode": "8901262150020", "category": "Dairy", "price": 27.0, "stock": 25},
                {"name": "Parle-G Gold Biscuits 100g", "barcode": "8901719114138", "category": "Biscuits & Snacks", "price": 10.0, "stock": 40},
                {"name": "Britannia Good Day Butter Biscuits", "barcode": "8901063142275", "category": "Biscuits & Snacks", "price": 30.0, "stock": 18},
                {"name": "Tata Salt Vacuum Evaporated 1kg", "barcode": "8901072002447", "category": "Grocery", "price": 28.0, "stock": 30},
                {"name": "Aashirvaad Shudh Chakki Atta 5kg", "barcode": "8901725181223", "category": "Grocery", "price": 245.0, "stock": 12},
            ]
            for p_data in primary_products:
                p = db.query(models.Product).filter(
                    models.Product.user_id == uid,
                    models.Product.name == p_data["name"]
                ).first()
                if not p:
                    p = models.Product(
                        user_id=uid,
                        name=p_data["name"],
                        barcode=p_data["barcode"],
                        category=p_data["category"],
                        price=p_data["price"],
                        stock=p_data["stock"],
                        updated_at=datetime.utcnow()
                    )
                    db.add(p)
                else:
                    p.stock = p_data["stock"]
                    p.price = p_data["price"]

        # Partner Store 1 (Krishna Supermarket - 0.5 km away)
        nearby_1_products = [
            {"name": "Maggi 2-Minute Noodles 70g", "barcode": "8901058852394", "category": "Instant Food", "price": 14.0, "stock": 42},
            {"name": "Amul Taaza Homogenised Toned Milk 500ml", "barcode": "8901262150020", "category": "Dairy", "price": 27.0, "stock": 15},
            {"name": "Kurkure Masala Munch 90g", "barcode": "8901491101831", "category": "Snacks", "price": 20.0, "stock": 50},
        ]
        for p_data in nearby_1_products:
            p = db.query(models.Product).filter(
                models.Product.user_id == "store_nearby_1",
                models.Product.name == p_data["name"]
            ).first()
            if not p:
                db.add(models.Product(
                    user_id="store_nearby_1",
                    name=p_data["name"],
                    barcode=p_data["barcode"],
                    category=p_data["category"],
                    price=p_data["price"],
                    stock=p_data["stock"],
                    updated_at=datetime.utcnow() - timedelta(minutes=15)
                ))
            else:
                p.stock = p_data["stock"]
                p.price = p_data["price"]

        # Partner Store 2 (Apna Bazaar Mart - 1.6 km away, discount on Maggi!)
        nearby_2_products = [
            {"name": "Maggi 2-Minute Noodles 70g", "barcode": "8901058852394", "category": "Instant Food", "price": 13.5, "stock": 75},
            {"name": "Aashirvaad Shudh Chakki Atta 5kg", "barcode": "8901725181223", "category": "Grocery", "price": 240.0, "stock": 16},
            {"name": "Lays Magic Masala 50g", "barcode": "8901491001223", "category": "Snacks", "price": 20.0, "stock": 60},
        ]
        for p_data in nearby_2_products:
            p = db.query(models.Product).filter(
                models.Product.user_id == "store_nearby_2",
                models.Product.name == p_data["name"]
            ).first()
            if not p:
                db.add(models.Product(
                    user_id="store_nearby_2",
                    name=p_data["name"],
                    barcode=p_data["barcode"],
                    category=p_data["category"],
                    price=p_data["price"],
                    stock=p_data["stock"],
                    updated_at=datetime.utcnow() - timedelta(hours=1)
                ))
            else:
                p.stock = p_data["stock"]
                p.price = p_data["price"]

        # Partner Store 3 (Reliance Smart Point - 2.6 km away)
        nearby_3_products = [
            {"name": "Maggi 2-Minute Noodles 70g", "barcode": "8901058852394", "category": "Instant Food", "price": 14.0, "stock": 110},
            {"name": "Tata Tea Gold 500g", "barcode": "8901072001556", "category": "Beverages", "price": 310.0, "stock": 25},
        ]
        for p_data in nearby_3_products:
            p = db.query(models.Product).filter(
                models.Product.user_id == "store_nearby_3",
                models.Product.name == p_data["name"]
            ).first()
            if not p:
                db.add(models.Product(
                    user_id="store_nearby_3",
                    name=p_data["name"],
                    barcode=p_data["barcode"],
                    category=p_data["category"],
                    price=p_data["price"],
                    stock=p_data["stock"],
                    updated_at=datetime.utcnow() - timedelta(hours=3)
                ))
            else:
                p.stock = p_data["stock"]
                p.price = p_data["price"]

        db.commit()
        print("[SUCCESS] Demo seeding completed successfully!")
        
        # Print summary
        total_stores = db.query(models.StoreProfile).count()
        total_products = db.query(models.Product).count()
        print(f"[SUMMARY] Total stores in DB: {total_stores}")
        print(f"[SUMMARY] Total products in DB: {total_products}")


    finally:
        db.close()

if __name__ == "__main__":
    seed()

"""
Seed demo stores and inventory for ScanSnap AI.
Sets up:
1. Primary demo store with Maggi (out of stock, stock=0) to trigger Find Elsewhere flow.
2. Three nearby partner stores with varying distances (0.5 km, 1.6 km, 2.6 km) and prices.
3. Automatically updates any existing store profiles in smartvendor.db so active phone users benefit immediately.
"""

import sys
from datetime import datetime, timedelta
from sqlalchemy import text
import models
from database import engine, SessionLocal, Base

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

def seed(db=None):
    # 1. Ensure all tables and columns exist
    Base.metadata.create_all(bind=engine)
    with engine.connect() as conn:
        for col in ["latitude", "longitude"]:
            try:
                conn.execute(text(f"ALTER TABLE store_profiles ADD COLUMN {col} FLOAT"))
                conn.commit()
            except Exception:
                pass

    close_db = False
    if db is None:
        db = SessionLocal()
        close_db = True
    
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
            # Primary store products (Comprehensive Indian Retail FMCG with Barcodes)
            primary_products = [
                # Retail Dataset Packshots
                {"name": "Amul Ice Cream Cup Vanilla Magic 100ml", "barcode": "8901262010014", "category": "Dairy & Bakery", "price": 30.0, "stock": 85},
                {"name": "Britannia Cake Gobbles Choco Chill 65g", "barcode": "8901063142018", "category": "Dairy & Bakery", "price": 30.0, "stock": 75},
                {"name": "CeraVe Hydrating Cleanser 236ml", "barcode": "3337875597371", "category": "Personal Care", "price": 900.0, "stock": 40},
                {"name": "Head & Shoulders Cool Menthol Anti-Dandruff Shampoo 180ml", "barcode": "4902430730013", "category": "Personal Care", "price": 250.0, "stock": 65},
                {"name": "Nestle Everyday Dairy Whitener Milk Powder 20g", "barcode": "8901058852314", "category": "Dairy & Bakery", "price": 10.0, "stock": 140},
                {"name": "Plum Green Tea Pore Cleansing Face Wash 100ml", "barcode": "8906118410214", "category": "Personal Care", "price": 350.0, "stock": 50},
                {"name": "Thums Up Charged Carbonated Beverage 250ml Can", "barcode": "8901764012211", "category": "Beverages", "price": 20.0, "stock": 120},
                {"name": "Wild Stone Forest Spice Deodorant Soap 125g", "barcode": "8904006304218", "category": "Personal Care", "price": 70.0, "stock": 90},
                {"name": "Nivea Men Fresh Active Deodorant 150ml", "barcode": "4005808816033", "category": "Personal Care", "price": 199.0, "stock": 55},

                # Instant Food
                {"name": "Maggi 2-Minute Noodles 70g", "barcode": "8901058852394", "category": "Instant Food", "price": 14.0, "stock": 0}, # Out of stock to trigger Find Elsewhere
                {"name": "Yippee Magic Masala Noodles 70g", "barcode": "8901262010171", "category": "Instant Food", "price": 14.0, "stock": 115},
                {"name": "Top Ramen Curry Veg Noodles 70g", "barcode": "8901262010172", "category": "Instant Food", "price": 15.0, "stock": 80},
                {"name": "Chings Secret Schezwan Noodles 60g", "barcode": "8901595852109", "category": "Instant Food", "price": 15.0, "stock": 60},

                # Snacks & Biscuits
                {"name": "Cadbury Oreo Vanilla Creme Biscuits 120g", "barcode": "8901262010160", "category": "Snacks & Biscuits", "price": 30.0, "stock": 80},
                {"name": "Britannia Treat Jim Jam Biscuits 100g", "barcode": "8901262010162", "category": "Snacks & Biscuits", "price": 35.0, "stock": 75},
                {"name": "Britannia Bourbon Chocolate Biscuits 150g", "barcode": "8901262010210", "category": "Snacks & Biscuits", "price": 30.0, "stock": 70},
                {"name": "Britannia Milk Bikis Biscuits 100g", "barcode": "8901262010211", "category": "Snacks & Biscuits", "price": 25.0, "stock": 80},
                {"name": "Britannia Good Day Butter Biscuits 100g", "barcode": "8901063142275", "category": "Snacks & Biscuits", "price": 30.0, "stock": 65},
                {"name": "Parle-G Gold Biscuits 100g", "barcode": "8901719114138", "category": "Snacks & Biscuits", "price": 10.0, "stock": 150},
                {"name": "Parle Hide & Seek Chocolate Chip Cookies 120g", "barcode": "8901262010060", "category": "Snacks & Biscuits", "price": 30.0, "stock": 60},
                {"name": "Parle Monaco Salted Crackers 75g", "barcode": "8901262010164", "category": "Snacks & Biscuits", "price": 15.0, "stock": 100},
                {"name": "Parle KrackJack Sweet & Salty Crackers 75g", "barcode": "8901262010165", "category": "Snacks & Biscuits", "price": 15.0, "stock": 95},
                {"name": "Britannia Little Hearts Biscuits 75g", "barcode": "8901262010166", "category": "Snacks & Biscuits", "price": 20.0, "stock": 85},
                {"name": "Balaji Soya Sticks 65g", "barcode": "8901262010161", "category": "Snacks & Biscuits", "price": 20.0, "stock": 90},
                {"name": "Lay's India's Magic Masala Potato Chips 50g", "barcode": "8901262010141", "category": "Snacks & Biscuits", "price": 20.0, "stock": 100},
                {"name": "Kurkure Masala Munch Crispy Snacks 75g", "barcode": "8901491101831", "category": "Snacks & Biscuits", "price": 20.0, "stock": 110},
                {"name": "Haldiram's Nagpur Bhujia Sev 200g", "barcode": "8901262010143", "category": "Snacks & Biscuits", "price": 55.0, "stock": 50},

                # Chocolates
                {"name": "Cadbury Dairy Milk Chocolate 50g", "barcode": "8901262010063", "category": "Chocolates", "price": 40.0, "stock": 100},
                {"name": "Cadbury Dairy Milk Silk 60g", "barcode": "8901262010064", "category": "Chocolates", "price": 80.0, "stock": 45},
                {"name": "Cadbury 5 Star Chocolate Bar 20g", "barcode": "8901262010167", "category": "Chocolates", "price": 15.0, "stock": 110},
                {"name": "Cadbury Perk Wafer Chocolate 15g", "barcode": "8901262010169", "category": "Chocolates", "price": 10.0, "stock": 130},
                {"name": "Nestle KitKat 4 Finger Chocolate 38g", "barcode": "8901262010065", "category": "Chocolates", "price": 20.0, "stock": 90},
                {"name": "Nestle Munch Chocolate Wafer 18g", "barcode": "8901262010168", "category": "Chocolates", "price": 10.0, "stock": 140},
                {"name": "Snickers Peanut Chocolate Bar 45g", "barcode": "8901262010170", "category": "Chocolates", "price": 45.0, "stock": 50},

                # Dairy
                {"name": "Amul Taaza Homogenised Toned Milk 500ml", "barcode": "8901262150020", "category": "Dairy", "price": 27.0, "stock": 35},
                {"name": "Amul Butter Pasteurized 100g", "barcode": "8901262010016", "category": "Dairy", "price": 56.0, "stock": 50},
                {"name": "Amul Processed Cheese Blocks 200g", "barcode": "8901262010173", "category": "Dairy", "price": 75.0, "stock": 40},
                {"name": "Amul Pure Ghee 500ml", "barcode": "8901262010174", "category": "Dairy", "price": 290.0, "stock": 25},

                # Groceries & Cooking Essentials
                {"name": "Aashirvaad Shudh Chakki Atta 5kg", "barcode": "8901725181223", "category": "Groceries", "price": 245.0, "stock": 18},
                {"name": "Patanjali Whole Wheat Chakki Atta 5kg", "barcode": "8901058000002", "category": "Groceries", "price": 240.0, "stock": 35},
                {"name": "Fortune Everyday Basmati Rice 1kg", "barcode": "8901058000003", "category": "Groceries", "price": 180.0, "stock": 30},
                {"name": "Tata Salt Vacuum Evaporated 1kg", "barcode": "8901072002447", "category": "Groceries", "price": 28.0, "stock": 45},
                {"name": "Madhur Pure Refined Sugar 1kg", "barcode": "8901262010039", "category": "Groceries", "price": 44.0, "stock": 120},
                {"name": "Tata Sampann Unpolished Toor Dal 1kg", "barcode": "8901058000004", "category": "Groceries", "price": 160.0, "stock": 50},
                {"name": "Tata Sampann Unpolished Chana Dal 1kg", "barcode": "8901058000005", "category": "Groceries", "price": 95.0, "stock": 60},
                {"name": "Tata Sampann Moong Dal Split 1kg", "barcode": "8901058000006", "category": "Groceries", "price": 120.0, "stock": 45},
                {"name": "Tata Sampann Rajma Red 1kg", "barcode": "8901058000007", "category": "Groceries", "price": 140.0, "stock": 40},
                {"name": "Fortune Sunlite Refined Sunflower Oil 1L", "barcode": "8901262010084", "category": "Groceries", "price": 145.0, "stock": 35},
                {"name": "Dabur 100% Pure Honey Squeezy 250g", "barcode": "8901262010175", "category": "Groceries", "price": 195.0, "stock": 30},
                {"name": "Kissan Fresh Tomato Ketchup 500g", "barcode": "8901262010176", "category": "Groceries", "price": 125.0, "stock": 40},
                {"name": "Quaker Rolled Wholegrain Oats 400g", "barcode": "8901262010077", "category": "Groceries", "price": 110.0, "stock": 30},
                {"name": "Everest Turmeric Powder Haldi 100g", "barcode": "8901262010151", "category": "Groceries", "price": 32.0, "stock": 60},

                # Beverages
                {"name": "Coca-Cola Original Taste 300ml Can", "barcode": "8901262010131", "category": "Beverages", "price": 40.0, "stock": 65},
                {"name": "Sprite Lime Flavored Soft Drink 300ml Can", "barcode": "8901262010132", "category": "Beverages", "price": 40.0, "stock": 60},
                {"name": "Thums Up Soft Drink 300ml Can", "barcode": "8901262010133", "category": "Beverages", "price": 40.0, "stock": 70},
                {"name": "Parle Agro Appy Fizz Sparkling Apple Drink 250ml", "barcode": "8901262010185", "category": "Beverages", "price": 35.0, "stock": 80},
                {"name": "Parle Frooti Fresh Mango Drink 250ml", "barcode": "8901262010183", "category": "Beverages", "price": 35.0, "stock": 90},
                {"name": "Maaza Real Mango Juice Drink 250ml", "barcode": "8901262010184", "category": "Beverages", "price": 35.0, "stock": 85},
                {"name": "Brooke Bond Taj Mahal Tea 250g", "barcode": "8901262010121", "category": "Beverages", "price": 180.0, "stock": 35},
                {"name": "Brooke Bond Red Label Tea 250g", "barcode": "8901262010122", "category": "Beverages", "price": 140.0, "stock": 40},

                # Personal Care & Hygiene
                {"name": "Dettol Original Germ Protection Bathing Soap 75g", "barcode": "8901262010114", "category": "Personal Care", "price": 38.0, "stock": 70},
                {"name": "Colgate Strong Teeth Anticavity Toothpaste 150g", "barcode": "8901262010180", "category": "Personal Care", "price": 65.0, "stock": 60},
                {"name": "Pepsodent Expert Protection Toothpaste 140g", "barcode": "8901262010181", "category": "Personal Care", "price": 55.0, "stock": 50},
                {"name": "Clinic Plus Strong & Long Health Shampoo 175ml", "barcode": "8901262010182", "category": "Personal Care", "price": 70.0, "stock": 45},

                # Household & Cleaning
                {"name": "Surf Excel Easy Wash Detergent Powder 1kg", "barcode": "8901262010107", "category": "Household & Cleaning", "price": 140.0, "stock": 45},
                {"name": "Vim Dishwash Bar with Lemon 300g", "barcode": "8901262010177", "category": "Household & Cleaning", "price": 15.0, "stock": 150},
                {"name": "Lizol Disinfectant Floor Cleaner Citrus 500ml", "barcode": "8901262010178", "category": "Household & Cleaning", "price": 110.0, "stock": 35},
                {"name": "Harpic Power Plus Toilet Cleaner 500ml", "barcode": "8901262010179", "category": "Household & Cleaning", "price": 95.0, "stock": 40},
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
        if close_db:
            db.close()

if __name__ == "__main__":
    seed()

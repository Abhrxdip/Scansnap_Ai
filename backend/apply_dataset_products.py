#!/usr/bin/env python3
"""
Seed & Sync Retail Dataset Products to Smart Vendor Database
Ensures all 8 retail dataset items (Amul Ice Cream, Cake, CeraVe, HnS Shampoo,
Nestle Milk Powder, Plum, Thums Up, Wild Stone) are present in the inventory
and master catalog with accurate pricing, barcodes, and stock levels.
"""

import sys
from pathlib import Path
from datetime import datetime

# Add backend directory to sys.path
backend_dir = Path(__file__).resolve().parent
sys.path.insert(0, str(backend_dir))

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass


from database import SessionLocal, engine
import models

NEW_DATASET_PRODUCTS = [
    {
        "key": "amul_ice_cream",
        "name": "Amul Ice Cream Cup Vanilla Magic 100ml",
        "category": "Dairy & Bakery",
        "price": 30.0,
        "barcode": "8901262010014",
        "stock": 85,
        "low_stock_threshold": 10,
        "aliases": ["Amul Ice Cream", "Amul Vanilla Cup", "Amul Icecream"]
    },
    {
        "key": "cake",
        "name": "Britannia Cake Gobbles Choco Chill 65g",
        "category": "Dairy & Bakery",
        "price": 30.0,
        "barcode": "8901063142018",
        "stock": 75,
        "low_stock_threshold": 10,
        "aliases": ["Britannia Cake Gobbles", "Britannia Treat Chocolate Cake", "Britannia Cake", "Gobbles Choco Chill"]
    },
    {
        "key": "cerave",
        "name": "CeraVe Hydrating Cleanser 236ml",
        "category": "Personal Care & Hygiene",
        "price": 900.0,
        "barcode": "3337875597371",
        "stock": 40,
        "low_stock_threshold": 5,
        "aliases": ["CeraVe Hydrating Cleanser", "CeraVe Daily Moisturizing Lotion", "CeraVe Cleanser", "CeraVe Lotion"]
    },
    {
        "key": "hns_shampoo",
        "name": "Head & Shoulders Cool Menthol Anti-Dandruff Shampoo 180ml",
        "category": "Personal Care & Hygiene",
        "price": 250.0,
        "barcode": "4902430730013",
        "stock": 65,
        "low_stock_threshold": 10,
        "aliases": ["Head & Shoulders Shampoo", "HnS Shampoo", "Head and Shoulders"]
    },
    {
        "key": "nestle_milk_powder",
        "name": "Nestle Everyday Dairy Whitener Milk Powder 20g",
        "category": "Dairy & Bakery",
        "price": 10.0,
        "barcode": "8901058852314",
        "stock": 140,
        "low_stock_threshold": 20,
        "aliases": ["Nestle Milk Powder", "Everyday Dairy Whitener", "Nestle Everyday"]
    },
    {
        "key": "plum",
        "name": "Plum Green Tea Pore Cleansing Face Wash 100ml",
        "category": "Personal Care & Hygiene",
        "price": 350.0,
        "barcode": "8906118410214",
        "stock": 50,
        "low_stock_threshold": 8,
        "aliases": ["Plum Skincare", "Plum Face Wash", "Plum Green Tea"]
    },
    {
        "key": "thums_up",
        "name": "Thums Up Charged Carbonated Beverage 250ml Can",
        "category": "Beverages & Drinks",
        "price": 20.0,
        "barcode": "8901764012211",
        "stock": 120,
        "low_stock_threshold": 15,
        "aliases": ["Thums Up Can", "Thums Up Cold Drink", "Thums Up"]
    },
    {
        "key": "wild_stone",
        "name": "Wild Stone Forest Spice Deodorant Soap 125g",
        "category": "Personal Care & Hygiene",
        "price": 70.0,
        "barcode": "8904006304218",
        "stock": 90,
        "low_stock_threshold": 12,
        "aliases": ["Wild Stone Forest Spice Deodorant Soap", "Wild Stone Code Platinum Body Perfume", "Wild Stone Soap", "Wild Stone Deodorant"]
    },
]


def sync_products():
    models.Base.metadata.create_all(bind=engine)
    db = SessionLocal()

    try:
        # Determine existing user_ids in products table
        user_ids = [r[0] for r in db.query(models.Product.user_id).distinct().all()]
        if not user_ids:
            user_ids = ["user_default", "admin"]
        else:
            if "user_default" not in user_ids:
                user_ids.append("user_default")

        print(f"📦 Synchronizing {len(NEW_DATASET_PRODUCTS)} products for users: {user_ids}")

        added_prods = 0
        updated_prods = 0
        catalog_added = 0

        for item in NEW_DATASET_PRODUCTS:
            # 1. Ensure in Master Catalog
            cat_entry = db.query(models.MasterCatalog).filter(
                (models.MasterCatalog.barcode == item["barcode"]) |
                (models.MasterCatalog.name == item["name"])
            ).first()

            if not cat_entry:
                new_cat = models.MasterCatalog(
                    name=item["name"],
                    category=item["category"],
                    suggested_price=item["price"],
                    barcode=item["barcode"]
                )
                db.add(new_cat)
                catalog_added += 1

            # 2. Ensure in Products for each user
            for uid in user_ids:
                prod = db.query(models.Product).filter(
                    models.Product.user_id == uid,
                    (models.Product.barcode == item["barcode"]) |
                    (models.Product.name == item["name"])
                ).first()

                if not prod:
                    prod = models.Product(
                        user_id=uid,
                        name=item["name"],
                        barcode=item["barcode"],
                        category=item["category"],
                        price=item["price"],
                        stock=item["stock"],
                        low_stock_threshold=item["low_stock_threshold"],
                        created_at=datetime.utcnow()
                    )
                    db.add(prod)
                    added_prods += 1
                else:
                    # Update stock and price if 0 or mismatch
                    prod.price = item["price"]
                    if prod.stock <= 0:
                        prod.stock = item["stock"]
                    updated_prods += 1

        db.commit()
        print(f"✅ Sync complete!")
        print(f"  • Inventory products added: {added_prods}")
        print(f"  • Inventory products refreshed: {updated_prods}")
        print(f"  • Master Catalog additions: {catalog_added}")

    except Exception as e:
        db.rollback()
        print(f"❌ Error syncing products: {e}")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    sync_products()

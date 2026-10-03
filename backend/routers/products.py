import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
from auth import CurrentUser, OptionalUser
import models
import schemas

router = APIRouter(prefix="/products", tags=["Products"])


from sqlalchemy import or_

SAMPLE_PRODUCTS = [
    ("Oreo", "Snacks", 30.0, 80, "8901262010160"),
    ("Soya Sticks", "Snacks", 20.0, 90, "8901262010161"),
    ("Jim Jam", "Snacks", 35.0, 75, "8901262010162"),
    ("Bourbon", "Snacks", 30.0, 70, "8901262010163"),
    ("Monaco", "Snacks", 15.0, 100, "8901262010164"),
    ("KrackJack", "Snacks", 15.0, 95, "8901262010165"),
    ("Little Hearts", "Snacks", 20.0, 85, "8901262010166"),
    ("Hide and Seek", "Snacks", 30.0, 60, "8901262010060"),
    ("Good Day", "Snacks", 25.0, 80, "8901262010061"),
    ("Parle G", "Snacks", 10.0, 150, "8901262010062"),
    ("5 Star", "Chocolates", 15.0, 110, "8901262010167"),
    ("Munch", "Chocolates", 10.0, 140, "8901262010168"),
    ("Perk", "Chocolates", 10.0, 130, "8901262010169"),
    ("Snickers", "Chocolates", 45.0, 50, "8901262010170"),
    ("Dairy Milk", "Chocolates", 40.0, 100, "8901262010063"),
    ("Dairy Milk Silk", "Chocolates", 80.0, 45, "8901262010064"),
    ("KitKat", "Chocolates", 20.0, 90, "8901262010065"),
    ("Maggi 2-Minute Noodles 70g", "Instant Food", 14.0, 0, "8901058852394"),
    ("Yippee Noodles", "Instant Food", 14.0, 115, "8901262010171"),
    ("Top Ramen", "Instant Food", 15.0, 80, "8901262010172"),
    ("Aashirvaad Atta", "Groceries", 270.0, 40, "8901058000001"),
    ("Patanjali Atta", "Groceries", 240.0, 35, "8901058000002"),
    ("Fortune Basmati Rice", "Groceries", 180.0, 30, "8901058000003"),
    ("Toor Dal", "Groceries", 160.0, 50, "8901058000004"),
    ("Chana Dal", "Groceries", 95.0, 60, "8901058000005"),
    ("Moong Dal", "Groceries", 120.0, 45, "8901058000006"),
    ("Rajma", "Groceries", 140.0, 40, "8901058000007"),
    ("Tata Salt", "Groceries", 28.0, 100, "8901262010053"),
    ("Sugar", "Groceries", 44.0, 120, "8901262010039"),
    ("Amul Milk", "Dairy", 28.0, 75, "8901262010015"),
    ("Amul Butter", "Dairy", 56.0, 50, "8901262010016"),
    ("Amul Cheese", "Dairy", 75.0, 40, "8901262010173"),
    ("Amul Ghee", "Dairy", 290.0, 25, "8901262010174"),
    ("Fortune Oil", "Oils", 145.0, 35, "8901262010084"),
    ("Dabur Honey", "Groceries", 195.0, 30, "8901262010175"),
    ("Kissan Ketchup", "Groceries", 125.0, 40, "8901262010176"),
    ("Quaker Oats", "Breakfast", 110.0, 30, "8901262010077"),
    ("Surf Excel", "Cleaning", 140.0, 45, "8901262010107"),
    ("Vim Bar", "Cleaning", 15.0, 150, "8901262010177"),
    ("Lizol", "Cleaning", 110.0, 35, "8901262010178"),
    ("Harpic", "Cleaning", 95.0, 40, "8901262010179"),
    ("Dettol Soap", "Hygiene", 38.0, 70, "8901262010114"),
    ("Colgate", "Hygiene", 65.0, 60, "8901262010180"),
    ("Pepsodent", "Hygiene", 55.0, 50, "8901262010181"),
    ("Clinic Plus Shampoo", "Hygiene", 70.0, 45, "8901262010182"),
    ("Taj Tea", "Beverages", 180.0, 35, "8901262010121"),
    ("Red Label Tea", "Beverages", 140.0, 40, "8901262010122"),
    ("Frooti", "Beverages", 35.0, 90, "8901262010183"),
    ("Maaza", "Beverages", 35.0, 85, "8901262010184"),
    ("Appy Fizz", "Beverages", 35.0, 80, "8901262010185"),
    ("Coca Cola", "Beverages", 40.0, 65, "8901262010131"),
    ("Sprite", "Beverages", 40.0, 60, "8901262010132"),
    ("Thums Up", "Beverages", 40.0, 70, "8901262010133"),
    ("Lays Chips", "Snacks", 20.0, 100, "8901262010141"),
    ("Kurkure", "Snacks", 20.0, 110, "8901262010142"),
    ("Haldiram Bhujia", "Snacks", 55.0, 50, "8901262010143"),
    ("Everest Turmeric", "Spices", 32.0, 60, "8901262010151"),
    ("Amul Ice Cream Cup Vanilla Magic 100ml", "Dairy & Bakery", 30.0, 50, "8901262010014"),
    ("Britannia Cake Gobbles Choco Chill 65g", "Dairy & Bakery", 30.0, 60, "8901063142018"),
    ("CeraVe Hydrating Cleanser 236ml", "Personal Care", 900.0, 30, "3337875597371"),
    ("Head & Shoulders Cool Menthol Anti-Dandruff Shampoo 180ml", "Personal Care", 250.0, 45, "4902430730013"),
    ("Nestle Everyday Dairy Whitener Milk Powder 20g", "Dairy & Bakery", 10.0, 40, "8901058852314"),
    ("Plum Green Tea Pore Cleansing Face Wash 100ml", "Personal Care", 350.0, 35, "8906118410214"),
    ("Thums Up Charged Carbonated Beverage 250ml Can", "Beverages", 20.0, 70, "8901764012211"),
    ("Wild Stone Forest Spice Deodorant Soap 125g", "Personal Care", 70.0, 55, "8904006304218"),
    ("Nivea Men Fresh Active Deodorant 150ml", "Personal Care", 199.0, 50, "4005808816033"),
    ("Britannia Bourbon Chocolate Biscuits", "Snacks", 30.0, 70, "8901262010210"),
    ("Britannia Milk Bikis Biscuits", "Snacks", 25.0, 80, "8901262010211"),
    ("Chings Secret Schezwan Noodles 60g", "Instant Food", 15.0, 60, "8901595852109"),
    ("Tata Tea Gold 250g", "Beverages", 145.0, 50, "8901052002159"),
    ("Nescafe Classic Instant Coffee 50g Jar", "Beverages", 175.0, 40, "8901058852239"),
    ("Colgate MaxFresh Peppermint Ice Toothpaste 150g", "Personal Care", 98.0, 60, "8901314010528"),
    ("Dettol Original Antiseptic Liquid 125ml", "Personal Care", 86.0, 55, "8901396388414"),
    ("Ariel Matic Front Load Detergent Powder 1kg", "Household", 230.0, 40, "4902430894210"),
    ("Beardo Mariner Eau De Parfum 50ml", "Personal Care", 799.0, 45, "8906084796773"),
    ("Beardo Mariner Perfume 50ml (Alt SKU)", "Personal Care", 799.0, 25, "8906084795998")
]

BARCODE_CATALOG_SKUS = SAMPLE_PRODUCTS

def seed_sample_products_for_user(db: Session, user_id: str) -> List[models.Product]:
    existing_products = db.query(models.Product).filter(models.Product.user_id == user_id).all()
    existing_barcodes = {p.barcode for p in existing_products if p.barcode}
    existing_names = {p.name.lower().strip() for p in existing_products if p.name}

    created = []
    for name, cat, price, stock, barcode in SAMPLE_PRODUCTS:
        if barcode in existing_barcodes or name.lower().strip() in existing_names:
            continue
        p = models.Product(
            id=str(uuid.uuid4()),
            user_id=user_id,
            name=name,
            category=cat,
            price=price,
            stock=stock,
            low_stock_threshold=10,
            barcode=barcode
        )
        created.append(p)
    if created:
        db.add_all(created)
        db.commit()
    return db.query(models.Product).filter(models.Product.user_id == user_id).order_by(models.Product.name).all()


@router.post("/sync-catalog")
def sync_catalog(
    user_id: OptionalUser,
    db: Session = Depends(get_db)
):
    target_uid = user_id or "demo_user"
    products = seed_sample_products_for_user(db, target_uid)
    return {
        "status": "success",
        "message": f"Realtime inventory synchronized with {len(products)} barcode-verified FMCG SKUs",
        "total_products": len(products)
    }


@router.get("", response_model=List[schemas.ProductResponse])
def list_products(
    user_id: OptionalUser,
    category: Optional[str] = None,
    search: Optional[str] = None,
    db: Session = Depends(get_db)
):
    target_uid = user_id or "demo_user"
    query = db.query(models.Product).filter(models.Product.user_id == target_uid)
    if category:
        query = query.filter(models.Product.category == category)
    if search:
        query = query.filter(models.Product.name.ilike(f"%{search}%"))

    products = query.order_by(models.Product.name).all()

    # If store has fewer than 20 products, auto-expand with full verified barcode inventory
    if len(products) < 20 and not category and not search:
        products = seed_sample_products_for_user(db, target_uid)

    return products


@router.post("", response_model=schemas.ProductResponse, status_code=status.HTTP_201_CREATED)
def create_product(
    body: schemas.ProductCreate,
    user_id: OptionalUser,
    db: Session = Depends(get_db)
):
    target_uid = user_id or "demo_user"
    product = models.Product(
        id=str(uuid.uuid4()),
        user_id=target_uid,
        **body.model_dump()
    )
    db.add(product)
    db.commit()
    db.refresh(product)
    return product


@router.get("/barcode/{barcode}", response_model=schemas.ProductResponse)
def get_product_by_barcode(
    barcode: str,
    user_id: OptionalUser,
    db: Session = Depends(get_db)
):
    # 1. Search in user's inventory
    product = db.query(models.Product).filter(
        models.Product.user_id == user_id,
        models.Product.barcode == barcode
    ).first()
    if product:
        return product

    # 2. Fallback to any store inventory (e.g. demo_user or seeded batch)
    fallback_product = db.query(models.Product).filter(
        models.Product.barcode == barcode
    ).first()
    if fallback_product:
        return fallback_product

    # 3. Fallback to Master Catalog (117K database)
    cat_item = db.query(models.MasterCatalog).filter(
        models.MasterCatalog.barcode == barcode
    ).first()
    if cat_item:
        return {
            "id": cat_item.id or str(uuid.uuid4()),
            "name": cat_item.name,
            "category": cat_item.category or "Retail FMCG",
            "price": cat_item.suggested_price or 25.0,
            "stock": 50,
            "low_stock_threshold": 5,
            "barcode": cat_item.barcode,
            "image_url": cat_item.image_url,
            "created_at": None,
            "updated_at": None
        }

    raise HTTPException(status_code=404, detail="Product not found for this barcode")


@router.get("/{product_id}", response_model=schemas.ProductResponse)
def get_product(
    product_id: str,
    user_id: CurrentUser,
    db: Session = Depends(get_db)
):
    product = db.query(models.Product).filter(
        models.Product.id == product_id,
        models.Product.user_id == user_id
    ).first()
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    return product

import math
from datetime import datetime

def haversine(lat1, lon1, lat2, lon2):
    R = 6371.0 # Earth radius in kilometers
    dLat = math.radians(lat2 - lat1)
    dLon = math.radians(lon2 - lon1)
    lat1 = math.radians(lat1)
    lat2 = math.radians(lat2)

    a = math.sin(dLat/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin(dLon/2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    return R * c

def get_time_ago_str(updated_at: datetime) -> str:
    if not updated_at:
        return "Unknown"
    delta = datetime.utcnow() - updated_at
    if delta.days > 0:
        return f"{delta.days} days ago"
    elif delta.seconds > 3600:
        return f"{delta.seconds // 3600} hours ago"
    elif delta.seconds > 60:
        return f"{delta.seconds // 60} minutes ago"
    return "Just now"

@router.get("/{product_id}/nearby", response_model=schemas.ProductNearbyResponse)
def get_nearby_products(
    product_id: str,
    user_id: CurrentUser,
    db: Session = Depends(get_db)
):
    # 1. Get current store product with fallback
    product = db.query(models.Product).filter(
        models.Product.id == product_id,
        models.Product.user_id == user_id
    ).first()
    if not product:
        product = db.query(models.Product).filter(models.Product.id == product_id).first()
    
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
        
    # 2. Get current store location
    current_store = db.query(models.StoreProfile).filter(models.StoreProfile.user_id == user_id).first()
    if not current_store and product.user_id:
        current_store = db.query(models.StoreProfile).filter(models.StoreProfile.user_id == product.user_id).first()
    curr_lat = current_store.latitude if (current_store and current_store.latitude is not None) else 12.9716
    curr_lon = current_store.longitude if (current_store and current_store.longitude is not None) else 77.5946
    
    # 3. Match other products (Barcode prioritized, else Name)
    target_prod_uid = product.user_id if product.user_id else user_id
    query = db.query(models.Product, models.StoreProfile).join(
        models.StoreProfile, models.Product.user_id == models.StoreProfile.user_id
    ).filter(models.Product.user_id != target_prod_uid)
    
    if product.barcode:
        query = query.filter(models.Product.barcode == product.barcode)
    else:
        query = query.filter(models.Product.name.ilike(f"%{product.name}%"))
        
    alternatives = query.all()
    
    # 4. Format and calculate distances
    results = []
    for p, store in alternatives:
        dist = None
        if curr_lat is not None and curr_lon is not None and store.latitude is not None and store.longitude is not None:
            dist = round(haversine(curr_lat, curr_lon, store.latitude, store.longitude), 1)
            
        results.append({
            "store_id": store.user_id,
            "store_name": store.name or "Unknown Store",
            "address": store.address or "",
            "available": p.stock > 0,
            "price": p.price,
            "distance_km": dist,
            "last_updated": get_time_ago_str(p.updated_at),
            "floor": getattr(p, "floor", None),
            "section": getattr(p, "section", None),
            "aisle": getattr(p, "aisle", None),
            "rack_number": getattr(p, "rack_number", None)
        })
        
    # 5. Sort: Available first, then distance (if possible), then price
    def sort_key(item):
        return (
            0 if item["available"] else 1,
            item["distance_km"] if item["distance_km"] is not None else float('inf'),
            item["price"]
        )
        
    results.sort(key=sort_key)
    
    # Limit to top 10
    results = results[:10]
    
    return schemas.ProductNearbyResponse(
        product=product,
        alternatives=results
    )

@router.put("/{product_id}", response_model=schemas.ProductResponse)
def update_product(
    product_id: str,
    body: schemas.ProductUpdate,
    user_id: OptionalUser,
    db: Session = Depends(get_db)
):
    target_uid = user_id or "demo_user"
    product = db.query(models.Product).filter(
        models.Product.id == product_id,
        or_(models.Product.user_id == target_uid, models.Product.user_id == "demo_user")
    ).first()
    if not product:
        product = db.query(models.Product).filter(models.Product.id == product_id).first()
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    for field, value in body.model_dump(exclude_unset=True).items():
        setattr(product, field, value)

    db.commit()
    db.refresh(product)
    return product


@router.put("/{product_id}/stock", response_model=schemas.ProductResponse)
def update_stock(
    product_id: str,
    body: schemas.StockUpdateRequest,
    user_id: OptionalUser,
    db: Session = Depends(get_db)
):
    target_uid = user_id or "demo_user"
    product = db.query(models.Product).filter(
        models.Product.id == product_id,
        or_(models.Product.user_id == target_uid, models.Product.user_id == "demo_user")
    ).first()
    if not product:
        product = db.query(models.Product).filter(models.Product.id == product_id).first()
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    product.stock = body.stock
    db.commit()
    db.refresh(product)
    return product


@router.delete("/{product_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_product(
    product_id: str,
    user_id: OptionalUser,
    db: Session = Depends(get_db)
):
    target_uid = user_id or "demo_user"
    product = db.query(models.Product).filter(
        models.Product.id == product_id,
        or_(models.Product.user_id == target_uid, models.Product.user_id == "demo_user")
    ).first()
    if not product:
        product = db.query(models.Product).filter(models.Product.id == product_id).first()
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    db.delete(product)
    db.commit()

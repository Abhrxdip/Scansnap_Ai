import re
import math
from sqlalchemy.orm import Session
from sqlalchemy import or_
import models
from datetime import datetime, timedelta
import logging

logger = logging.getLogger(__name__)

def haversine(lat1, lon1, lat2, lon2):
    R = 6371.0
    dLat = math.radians(lat2 - lat1)
    dLon = math.radians(lon2 - lon1)
    lat1 = math.radians(lat1)
    lat2 = math.radians(lat2)
    a = math.sin(dLat/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin(dLon/2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    return R * c

def extract_intent_and_entities(message: str):
    raw_message = message.lower().strip()
    
    find_else_keywords = [
        "where else", "other store", "other stores", "another store",
        "nearby store", "nearby stores", "elsewhere", "where can i find",
        "where can i get", "locate", "different store", "other shop", "find it",
        "near me", "nearby", "anywhere else", "other places", "where to find"
    ]
    price_keywords = ["price", "cost", "how much", "rate"]
    avail_keywords = ["available", "availability", "have", "stock", "in stock", "got"]
    cat_keywords = ["show me", "what kind of", "types of", "list", "category"]
    
    stop_words = {
        "is", "the", "of", "do", "you", "have", "what", "are", "show", "me",
        "in", "this", "store", "stores", "shop", "shops", "can", "i", "where",
        "else", "elsewhere", "find", "it", "get", "locate", "tell", "about",
        "any", "please", "check", "for", "at", "a", "an", "all", "some",
        "near", "nearby", "anywhere", "places", "place"
    }
    
    cleaned = re.sub(r"[^\w\s]", " ", raw_message)
    words = cleaned.split()
    
    excluded_keywords = set(find_else_keywords + price_keywords + avail_keywords + cat_keywords + ["other", "another", "nearby", "stores", "store", "shop", "places", "near"])
    entities = [w for w in words if w not in stop_words and w not in excluded_keywords]
    extracted_name = " ".join(entities).strip()
    
    if any(k in raw_message for k in find_else_keywords):
        return {"intent": "FIND_ELSEWHERE", "entities": {"product_name": extracted_name}}
    elif any(k in raw_message for k in price_keywords):
        return {"intent": "PRODUCT_PRICE", "entities": {"product_name": extracted_name}}
    elif any(k in raw_message for k in cat_keywords):
        return {"intent": "CATEGORY_SEARCH", "entities": {"category": extracted_name}}
    elif any(k in raw_message for k in avail_keywords) or extracted_name:
        return {"intent": "PRODUCT_AVAILABILITY", "entities": {"product_name": extracted_name}}
    
    return {"intent": "UNKNOWN", "entities": {}}

def get_time_ago_str(updated_at: datetime) -> str:
    if not updated_at:
        return "recently"
    delta = datetime.utcnow() - updated_at
    if delta.days > 0:
        return f"{delta.days}d ago"
    elif delta.seconds > 3600:
        return f"{delta.seconds // 3600}h ago"
    elif delta.seconds > 60:
        return f"{delta.seconds // 60}m ago"
    return "just now"

def handle_chat_request(user_message: str, db: Session, store_id: str):
    # Ensure demo partner stores exist in DB
    try:
        partner_exists = db.query(models.Product).filter(
            models.Product.user_id.in_(["store_nearby_1", "store_nearby_2", "store_nearby_3"])
        ).first()
        if not partner_exists:
            from seed_demo_data import seed
            seed(db=db)
    except Exception as e:
        logger.warning(f"Demo auto-seed failed: {e}")

    parsed = extract_intent_and_entities(user_message)
    
    intent = parsed.get("intent", "UNKNOWN")
    entities = parsed.get("entities", {})
    product_name = entities.get("product_name", "").strip()
    category = entities.get("category", "").strip()
    
    response_lines = []
    
    if intent == "FIND_ELSEWHERE":
        if not product_name:
            response_lines.append("Which product are you looking to find in nearby stores? (e.g., 'Where else can I find Maggi?')")
            return {"success": True, "response": "\n".join(response_lines)}
            
        # Get current store coords (with default fallback to demo coords 12.9716, 77.5946)
        current_store = db.query(models.StoreProfile).filter(models.StoreProfile.user_id == store_id).first()
        curr_lat = current_store.latitude if (current_store and current_store.latitude is not None) else 12.9716
        curr_lon = current_store.longitude if (current_store and current_store.longitude is not None) else 77.5946

        # Search other stores for this product name with stock > 0
        alternatives = db.query(models.Product, models.StoreProfile).outerjoin(
            models.StoreProfile, models.Product.user_id == models.StoreProfile.user_id
        ).filter(
            models.Product.user_id != store_id,
            models.Product.name.ilike(f"%{product_name}%"),
            models.Product.stock > 0
        ).all()

        if not alternatives:
            terms = [t for t in product_name.split() if len(t) > 1]
            if terms:
                filters = [models.Product.name.ilike(f"%{t}%") for t in terms]
                alternatives = db.query(models.Product, models.StoreProfile).outerjoin(
                    models.StoreProfile, models.Product.user_id == models.StoreProfile.user_id
                ).filter(
                    models.Product.user_id != store_id,
                    models.Product.stock > 0,
                    *filters
                ).all()
                if not alternatives:
                    alternatives = db.query(models.Product, models.StoreProfile).outerjoin(
                        models.StoreProfile, models.Product.user_id == models.StoreProfile.user_id
                    ).filter(
                        models.Product.user_id != store_id,
                        models.Product.stock > 0,
                        or_(*filters)
                    ).all()
        
        if not alternatives:
            response_lines.append(f"I couldn't find '{product_name}' in any nearby stores.")
        else:
            response_lines.append(f"Found '{product_name}' at nearby stores:")
            results = []
            for p, store in alternatives:
                dist = None
                if curr_lat is not None and curr_lon is not None and store and store.latitude is not None and store.longitude is not None:
                    dist = haversine(curr_lat, curr_lon, store.latitude, store.longitude)
                results.append({"store": store, "product": p, "dist": dist})
                
            # Sort by distance (nearest first), then lowest price
            results.sort(key=lambda x: (x["dist"] if x["dist"] is not None else float('inf'), x["product"].price))
            results = results[:5]
            
            for res in results:
                dist_str = f"{res['dist']:.1f} km" if res['dist'] is not None else "Nearby"
                time_ago = get_time_ago_str(res['product'].updated_at)
                store_display_name = res['store'].name if (res['store'] and res['store'].name) else "Nearby Partner Store"
                response_lines.append(
                    f"• {store_display_name}: ₹{res['product'].price} | {dist_str} | Updated {time_ago}"
                )

    elif intent in ["PRODUCT_AVAILABILITY", "PRODUCT_PRICE"]:
        if not product_name:
            response_lines.append("Which product would you like to check? (e.g. 'Is Maggi available?')")
            return {"success": True, "response": "\n".join(response_lines)}
            
        products = db.query(models.Product).filter(
            models.Product.user_id == store_id,
            models.Product.name.ilike(f"%{product_name}%")
        ).limit(5).all()

        if not products:
            terms = [t for t in product_name.split() if len(t) > 1]
            if terms:
                filters = [models.Product.name.ilike(f"%{t}%") for t in terms]
                products = db.query(models.Product).filter(
                    models.Product.user_id == store_id,
                    *filters
                ).limit(5).all()
                if not products:
                    products = db.query(models.Product).filter(
                        models.Product.user_id == store_id,
                        or_(*filters)
                    ).limit(5).all()


        
        if not products:
            response_lines.append(f"I'm sorry, but we couldn't find any product matching '{product_name}' in this store's inventory.")
            response_lines.append(f"💡 Tip: Try asking \"Where else can I find {product_name}?\" to check neighboring stores!")
        else:
            response_lines.append(f"Here is what I found for '{product_name}':")
            has_out_of_stock = False
            for p in products:
                stale_check = ""
                if p.updated_at and datetime.utcnow() - p.updated_at > timedelta(days=2):
                    stale_check = f" (Note: This inventory was last updated on {p.updated_at.strftime('%Y-%m-%d')} and may be stale.)"
                
                status = "in stock" if p.stock > 0 else "out of stock"
                if p.stock <= 0:
                    has_out_of_stock = True
                    
                response_lines.append(f"• {p.name} is {status} ({p.stock} available). The price is ₹{p.price}.{stale_check}")
                
            if has_out_of_stock:
                response_lines.append(f"\n💡 Tip: Since it's out of stock here, ask \"Where else can I find {product_name}?\" to locate nearby stores!")
                
    elif intent == "CATEGORY_SEARCH" and category:
        products = db.query(models.Product).filter(
            models.Product.user_id == store_id,
            or_(
                models.Product.category.ilike(f"%{category}%"),
                models.Product.name.ilike(f"%{category}%")
            ),
            models.Product.stock > 0
        ).limit(10).all()
        
        if not products:
            response_lines.append(f"I couldn't find any available products for the category '{category}'.")
        else:
            response_lines.append(f"Here are some {category} currently available:")
            for p in products:
                response_lines.append(f"• {p.name} - ₹{p.price} ({p.stock} in stock)")
                
    else:
        response_lines.append("I'm not quite sure what you're looking for. Could you please specify the product or category you want to check?")
        
    return {"success": True, "response": "\n".join(response_lines)}

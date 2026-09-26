"""
ScanSnap AI — Offers, Festival Discounts & Smart Combo Intelligence Router
Implements dynamic seasonal festival pricing and multi-product combo bundles
derived from the smart retail checkout dataset.
"""

from datetime import datetime, date
from typing import List, Optional
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session
from database import get_db
import models

router = APIRouter(prefix="/offers", tags=["Offers & Dynamic Pricing"])

# ── Dynamic Festival Discounts Configuration ────────────────────────────────
FESTIVAL_OFFERS = [
    {
        "name": "Republic Day Special",
        "month_start": (1, 24),
        "month_end": (1, 26),
        "discount_percent": 10.0,
        "applicable_products": "all",
        "banner": "🇮🇳 Republic Day Super Saver: Flat 10% Off Sitewide!"
    },
    {
        "name": "Holi Festival of Colors",
        "month_start": (3, 10),
        "month_end": (3, 16),
        "discount_percent": 20.0,
        "applicable_products": "all",
        "banner": "🎨 Holi Celebration Splash: 20% Off Retail FMCG!"
    },
    {
        "name": "Independence Day Dhamaka",
        "month_start": (8, 13),
        "month_end": (8, 16),
        "discount_percent": 10.0,
        "applicable_products": "all",
        "banner": "🇮🇳 Freedom Sale: 10% Off Storewide!"
    },
    {
        "name": "Raksha Bandhan Celebration",
        "month_start": (8, 7),
        "month_end": (8, 11),
        "discount_percent": 12.0,
        "applicable_products": "all",
        "banner": "🎁 Rakhi Gifts & Sweets: 12% Discount!"
    },
    {
        "name": "Ganesh Chaturthi Fest",
        "month_start": (8, 25),
        "month_end": (8, 30),
        "discount_percent": 18.0,
        "applicable_products": "all",
        "banner": "🌺 Ganesh Chaturthi Special: 18% Off Grocery & Dairy!"
    },
    {
        "name": "Dussehra Festive Bonanza",
        "month_start": (9, 28),
        "month_end": (10, 5),
        "discount_percent": 20.0,
        "applicable_products": "all",
        "banner": "🏹 Dussehra Festive Week: Flat 20% Off!"
    },
    {
        "name": "Diwali Grand Dhamaka",
        "month_start": (10, 15),
        "month_end": (10, 26),
        "discount_percent": 25.0,
        "applicable_products": "all",
        "banner": "🪔 Diwali Grand Festival: Up to 25% Off Everything!"
    },
    {
        "name": "Christmas Joy Sale",
        "month_start": (12, 23),
        "month_end": (12, 26),
        "discount_percent": 10.0,
        "applicable_products": "all",
        "banner": "🎄 Christmas Joy Sale: Flat 10% Off Cakes & Goodies!"
    },
    {
        "name": "New Year Kickoff",
        "month_start": (12, 30),
        "month_end": (1, 2),
        "discount_percent": 12.0,
        "applicable_products": "all",
        "banner": "🎉 New Year Kickoff: 12% Off Grocery & Essentials!"
    }
]

# ── Smart Combo Offers Configuration ─────────────────────────────────────────
COMBO_OFFERS = [
    {
        "id": "combo_snack",
        "name": "Chilled Snack Combo",
        "items": ["amul_ice_cream", "thums_up"],
        "display_items": ["Amul Ice Cream", "Thums Up"],
        "discount_percent": 3.0,
        "badge": "Popular Quick Bite",
        "description": "Buy Amul Ice Cream + Thums Up and get extra 3% off!"
    },
    {
        "id": "combo_breakfast",
        "name": "Morning Breakfast Combo",
        "items": ["nestle_milk_powder", "cake"],
        "display_items": ["Nestle Milk Powder", "Britannia Cake"],
        "discount_percent": 4.0,
        "badge": "Breakfast Favorite",
        "description": "Nestle Milk Powder + Britannia Cake with 4% combo savings!"
    },
    {
        "id": "combo_personal_care",
        "name": "Premium Personal Care Trio",
        "items": ["hns_shampoo", "plum", "cerave"],
        "display_items": ["Head & Shoulders Shampoo", "Plum Face Wash", "CeraVe Moisturizer"],
        "discount_percent": 15.0,
        "badge": "Mega 15% Saver",
        "description": "Complete skincare & hair care routine with massive 15% off!"
    },
    {
        "id": "combo_grooming",
        "name": "Men's Fresh Grooming Combo",
        "items": ["wild_stone", "hns_shampoo"],
        "display_items": ["Wild Stone Deodorant", "Head & Shoulders Shampoo"],
        "discount_percent": 8.0,
        "badge": "Daily Grooming",
        "description": "Wild Stone Deodorant + H&S Shampoo combo with 8% discount!"
    }
]


# ── Schemas ───────────────────────────────────────────────────────────────────
class CartItemInput(BaseModel):
    product_id: Optional[str] = None
    product_name: str
    quantity: int = 1
    unit_price: float


class CartOffersRequest(BaseModel):
    items: List[CartItemInput]
    apply_festival: bool = True
    apply_combos: bool = True


class AppliedOffer(BaseModel):
    name: str
    offer_type: str  # "combo" or "festival"
    discount_percent: float
    saved_amount: float
    description: str


class ComboSuggestion(BaseModel):
    combo_name: str
    missing_items: List[str]
    potential_discount_percent: float
    prompt: str


class CartOffersResponse(BaseModel):
    original_total: float
    discounted_total: float
    total_savings: float
    applied_offers: List[AppliedOffer]
    combo_suggestions: List[ComboSuggestion]
    active_festival: Optional[dict] = None


_MANUAL_FESTIVAL_OVERRIDE: Optional[str] = None


class FestivalOverrideRequest(BaseModel):
    festival_name: Optional[str] = None


# ── Helpers ───────────────────────────────────────────────────────────────────
def get_current_festival(target_date: Optional[date] = None) -> Optional[dict]:
    global _MANUAL_FESTIVAL_OVERRIDE
    if _MANUAL_FESTIVAL_OVERRIDE:
        for fest in FESTIVAL_OFFERS:
            if fest["name"].lower() == _MANUAL_FESTIVAL_OVERRIDE.lower():
                return fest

    today = target_date or datetime.utcnow().date()
    month, day = today.month, today.day

    for fest in FESTIVAL_OFFERS:
        sm, sd = fest["month_start"]
        em, ed = fest["month_end"]

        # Single month range
        if sm == em and sm == month and sd <= day <= ed:
            return fest
        # Cross month range (e.g. Dec 30 -> Jan 2)
        elif sm > em:
            if (month == sm and day >= sd) or (month == em and day <= ed):
                return fest
        # Multi-month range within same year
        elif sm < em:
            if (month == sm and day >= sd) or (month == em and day <= ed) or (sm < month < em):
                return fest

    # Default fallback: active seasonal promotional offer
    return {
        "name": "Smart Vendor Retail Special",
        "discount_percent": 5.0,
        "applicable_products": "all",
        "banner": "🛍️ ScanSnap Smart Retail: 5% Digital Checkout Discount Active!"
    }


def _match_product_key(name: str) -> Optional[str]:
    n = name.lower()
    if "amul" in n and ("ice" in n or "cream" in n):
        return "amul_ice_cream"
    if "cake" in n or "britannia" in n and "treat" in n:
        return "cake"
    if "cerave" in n:
        return "cerave"
    if "head" in n and "shoulders" in n or "hns" in n:
        return "hns_shampoo"
    if "milk powder" in n or ("nestle" in n and ("everyday" in n or "whitener" in n)):
        return "nestle_milk_powder"
    if "plum" in n:
        return "plum"
    if "thums" in n and "up" in n:
        return "thums_up"
    if "wild" in n and "stone" in n:
        return "wild_stone"
    return None


# ── Endpoints ─────────────────────────────────────────────────────────────────
@router.get("/festivals", summary="List all festival discount seasons")
def list_festivals():
    active = get_current_festival()
    return {
        "active_festival": active,
        "is_manual_override": _MANUAL_FESTIVAL_OVERRIDE is not None,
        "all_festivals": FESTIVAL_OFFERS
    }


@router.post("/override-festival", summary="Activate a festival season on-demand for live demonstrations")
def override_festival(payload: FestivalOverrideRequest):
    global _MANUAL_FESTIVAL_OVERRIDE
    _MANUAL_FESTIVAL_OVERRIDE = payload.festival_name
    active = get_current_festival()
    return {
        "status": "success",
        "is_manual_override": _MANUAL_FESTIVAL_OVERRIDE is not None,
        "active_festival": active
    }


@router.get("/combos", summary="List available smart combo deals")
def list_combos():
    return {"combos": COMBO_OFFERS}


@router.post("/calculate", response_model=CartOffersResponse, summary="Calculate combo & festival discounts for a cart")
def calculate_cart_discounts(payload: CartOffersRequest):
    subtotal = sum(item.unit_price * item.quantity for item in payload.items)
    if subtotal <= 0:
        return CartOffersResponse(
            original_total=0.0,
            discounted_total=0.0,
            total_savings=0.0,
            applied_offers=[],
            combo_suggestions=[]
        )

    # Detect product keys present in cart
    cart_keys = set()
    key_to_item = {}
    for item in payload.items:
        k = _match_product_key(item.product_name)
        if k:
            cart_keys.add(k)
            key_to_item[k] = item

    applied_offers: List[AppliedOffer] = []
    combo_suggestions: List[ComboSuggestion] = []
    total_savings = 0.0

    # 1. Evaluate Combos
    if payload.apply_combos:
        for combo in COMBO_OFFERS:
            combo_items = set(combo["items"])
            intersection = combo_items.intersection(cart_keys)

            # Fully satisfied combo!
            if combo_items.issubset(cart_keys):
                combo_subtotal = sum(key_to_item[k].unit_price * key_to_item[k].quantity for k in combo["items"])
                combo_discount = round(combo_subtotal * (combo["discount_percent"] / 100.0), 2)
                total_savings += combo_discount
                applied_offers.append(AppliedOffer(
                    name=combo["name"],
                    offer_type="combo",
                    discount_percent=combo["discount_percent"],
                    saved_amount=combo_discount,
                    description=combo["description"]
                ))
            # Partial combo: suggest completing the combo
            elif len(intersection) > 0 and len(intersection) < len(combo_items):
                missing = [name for k, name in zip(combo["items"], combo["display_items"]) if k not in cart_keys]
                combo_suggestions.append(ComboSuggestion(
                    combo_name=combo["name"],
                    missing_items=missing,
                    potential_discount_percent=combo["discount_percent"],
                    prompt=f"Add {' + '.join(missing)} to get {combo['discount_percent']}% off the {combo['name']}!"
                ))

    # 2. Evaluate Active Festival Offer
    active_festival = get_current_festival()
    if payload.apply_festival and active_festival:
        fest_pct = active_festival.get("discount_percent", 0.0)
        if fest_pct > 0:
            remaining_subtotal = max(0.0, subtotal - total_savings)
            fest_discount = round(remaining_subtotal * (fest_pct / 100.0), 2)
            total_savings += fest_discount
            applied_offers.append(AppliedOffer(
                name=active_festival["name"],
                offer_type="festival",
                discount_percent=fest_pct,
                saved_amount=fest_discount,
                description=active_festival.get("banner", f"{fest_pct}% Festival Discount")
            ))

    discounted_total = max(0.0, round(subtotal - total_savings, 2))
    return CartOffersResponse(
        original_total=round(subtotal, 2),
        discounted_total=discounted_total,
        total_savings=round(total_savings, 2),
        applied_offers=applied_offers,
        combo_suggestions=combo_suggestions,
        active_festival=active_festival
    )

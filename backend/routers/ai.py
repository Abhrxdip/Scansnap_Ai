from fastapi import APIRouter, Depends, HTTPException, status
from auth import CurrentUser
import uuid
from schemas import (
    AIChatRequest, AIChatResponse,
    InterStoreOrderRequest, InterStoreOrderResponse
)
from database import get_db
from sqlalchemy.orm import Session
from services.inventory_chat_service import handle_chat_request
import models

router = APIRouter(prefix="/ai", tags=["AI"])

@router.post("/chat", response_model=AIChatResponse)
def ai_chat(
    request: AIChatRequest,
    user_id: CurrentUser,
    db: Session = Depends(get_db)
):
    if not request.message.strip():
        raise HTTPException(status_code=400, detail="Message cannot be empty")

    result = handle_chat_request(
        user_message=request.message,
        db=db,
        store_id=user_id
    )

    if not result.get("success"):
        err_msg = result.get("error", "AI service error")
        status_code = 503 if "timeout" in err_msg or "connection" in err_msg else 500
        raise HTTPException(status_code=status_code, detail=err_msg)

    return AIChatResponse(
        success=True,
        response=result.get("response"),
        nearby_options=result.get("nearby_options")
    )

@router.post("/order", response_model=InterStoreOrderResponse)
def create_inter_store_order(
    order_req: InterStoreOrderRequest,
    user_id: CurrentUser,
    db: Session = Depends(get_db)
):
    if not order_req.items:
        raise HTTPException(status_code=400, detail="Order must have at least one item")

    order_id = f"ORD-{uuid.uuid4().hex[:8].upper()}"

    # Deduct or reserve stock from the partner store if product exists
    for item in order_req.items:
        product = db.query(models.Product).filter(
            models.Product.id == item.product_id
        ).first()
        if product and product.stock >= item.quantity:
            product.stock -= item.quantity
    
    db.commit()

    return InterStoreOrderResponse(
        order_id=order_id,
        status="CONFIRMED",
        seller_store_name=order_req.seller_store_name,
        total_amount=order_req.total_amount,
        message=f"Order #{order_id} successfully sent to {order_req.seller_store_name}! Delivery / pickup initiated."
    )

@router.get("/seed")
def trigger_seed(db: Session = Depends(get_db)):
    from seed_demo_data import seed
    try:
        seed(db=db)
        return {"success": True, "message": "Demo stores and inventory successfully seeded!"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

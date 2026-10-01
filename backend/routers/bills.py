import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from sqlalchemy import func
from database import get_db
from auth import CurrentUser
import models
import schemas

router = APIRouter(prefix="/bills", tags=["Bills"])


@router.post("", response_model=schemas.BillResponse, status_code=status.HTTP_201_CREATED)
def create_bill(
    body: schemas.BillCreate,
    user_id: CurrentUser,
    db: Session = Depends(get_db)
):
    """
    Create a bill and atomically deduct stock from each existing product.
    Safe for both catalog products and ad-hoc manual items.
    """
    if body.idempotency_key:
        bill_id = str(uuid.uuid5(uuid.NAMESPACE_OID, f"{user_id}_{body.idempotency_key}"))
        existing_bill = db.query(models.Bill).filter(models.Bill.id == bill_id).first()
        if existing_bill:
            return existing_bill
    else:
        bill_id = str(uuid.uuid4())

    # Map item -> valid_product or None, aggregate quantities, and calculate authoritative prices
    processed_items = []
    requested_quantities = {}
    products_by_id = {}
    authoritative_subtotal = 0.0

    for item in body.items:
        product = None
        authoritative_unit_price = item.unit_price
        
        if item.product_id:
            if item.product_id in products_by_id:
                product = products_by_id[item.product_id]
            else:
                product = db.query(models.Product).filter(
                    models.Product.id == item.product_id,
                    models.Product.user_id == user_id
                ).first()
                if product:
                    products_by_id[product.id] = product
            
            if product:
                requested_quantities[product.id] = requested_quantities.get(product.id, 0) + item.quantity
                # Override client price with authoritative server price for inventory items
                authoritative_unit_price = product.price

        # Calculate authoritative line total
        authoritative_line_total = item.quantity * authoritative_unit_price
        authoritative_subtotal += authoritative_line_total

        processed_items.append((item, product, authoritative_unit_price, authoritative_line_total))

    # Validate all stock availability before mutating any data
    for product_id, total_requested in requested_quantities.items():
        product = products_by_id[product_id]
        if total_requested > product.stock:
            raise HTTPException(
                status_code=400,
                detail=f"Insufficient stock for '{product.name}'. Requested: {total_requested}, Available: {product.stock}"
            )

    # Calculate final authoritative total amount
    # (Discount logic is not authoritatively supported in the backend, so we enforce exact summation)
    authoritative_total_amount = authoritative_subtotal + body.tax_amount

    # Create bill record
    bill = models.Bill(
        id=bill_id,
        user_id=user_id,
        total_amount=authoritative_total_amount,
        tax_amount=body.tax_amount,
        payment_mode=body.payment_mode
    )
    db.add(bill)

    # Create bill items and update stock for catalog items
    for item, product, auth_unit_price, auth_line_total in processed_items:
        bill_item = models.BillItem(
            id=str(uuid.uuid4()),
            bill_id=bill_id,
            product_id=product.id if product else None,
            product_name=item.product_name,
            quantity=item.quantity,
            unit_price=auth_unit_price,
            total_price=auth_line_total
        )
        db.add(bill_item)

    # Deduct stock atomically by aggregate amount to prevent concurrent overselling
    # Sort by product_id to prevent deadlocks in row-locking databases
    for product_id, total_requested in sorted(requested_quantities.items()):
        updated_count = db.query(models.Product).filter(
            models.Product.id == product_id,
            models.Product.user_id == user_id,
            models.Product.stock >= total_requested
        ).update({
            models.Product.stock: models.Product.stock - total_requested
        }, synchronize_session=False)

        if updated_count == 0:
            db.rollback()
            raise HTTPException(
                status_code=409,
                detail=f"Checkout failed due to concurrent inventory changes. Insufficient stock for '{products_by_id[product_id].name}'."
            )

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        # If two identical retries completely bypassed the pre-check concurrently, the unique constraint (PK) protects us
        if body.idempotency_key:
            existing_bill = db.query(models.Bill).filter(models.Bill.id == bill_id).first()
            if existing_bill:
                return existing_bill
        raise
    db.refresh(bill)
    return bill


@router.get("", response_model=List[schemas.BillResponse])
def list_bills(
    user_id: CurrentUser,
    limit: int = 50,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    bills = db.query(models.Bill).filter(
        models.Bill.user_id == user_id
    ).order_by(models.Bill.created_at.desc()).offset(offset).limit(limit).all()
    return bills


@router.get("/{bill_id}", response_model=schemas.BillResponse)
def get_bill(
    bill_id: str,
    user_id: CurrentUser,
    db: Session = Depends(get_db)
):
    bill = db.query(models.Bill).filter(
        models.Bill.id == bill_id,
        models.Bill.user_id == user_id
    ).first()
    if not bill:
        raise HTTPException(status_code=404, detail="Bill not found")
    return bill

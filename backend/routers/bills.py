import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from sqlalchemy import func
from database import get_db
from auth import CurrentUser, OptionalUser
import models
import schemas

router = APIRouter(prefix="/bills", tags=["Bills"])


@router.post("", response_model=schemas.BillResponse, status_code=status.HTTP_201_CREATED)
def create_bill(
    body: schemas.BillCreate,
    user_id: OptionalUser,
    db: Session = Depends(get_db)
):
    """
    Create a bill, deduct stock from the database for each sold product,
    and persist the transaction for real-time visibility across mobile and admin portal.
    """
    if body.idempotency_key:
        bill_id = str(uuid.uuid5(uuid.NAMESPACE_OID, f"{user_id}_{body.idempotency_key}"))
        existing_bill = db.query(models.Bill).filter(models.Bill.id == bill_id).first()
        if existing_bill:
            return existing_bill
    else:
        bill_id = str(uuid.uuid4())

    processed_items = []
    requested_quantities = {}
    products_by_id = {}
    authoritative_subtotal = 0.0

    for item in body.items:
        product = None
        authoritative_unit_price = item.unit_price

        # 1. Match in user's inventory by ID or Barcode
        if item.product_id:
            product = db.query(models.Product).filter(
                (models.Product.id == item.product_id) |
                (models.Product.barcode == item.product_id),
                models.Product.user_id == user_id
            ).first()

            # Fallback across any user inventory
            if not product:
                product = db.query(models.Product).filter(
                    (models.Product.id == item.product_id) |
                    (models.Product.barcode == item.product_id)
                ).first()

        # 2. Match in inventory by exact name
        if not product and item.product_name:
            product = db.query(models.Product).filter(
                models.Product.name.ilike(item.product_name.strip())
            ).first()

        # 3. If product not yet in database table, auto-seed it from MasterCatalog or item info
        if not product and (item.product_name or item.product_id):
            cat_item = None
            if item.product_id:
                cat_item = db.query(models.MasterCatalog).filter(
                    models.MasterCatalog.barcode == item.product_id
                ).first()
            if not cat_item and item.product_name:
                cat_item = db.query(models.MasterCatalog).filter(
                    models.MasterCatalog.name.ilike(f"%{item.product_name.strip()}%")
                ).first()

            prod_name = cat_item.name if cat_item else (item.product_name or f"Product #{item.product_id}")
            prod_price = cat_item.suggested_price if cat_item else (item.unit_price if item.unit_price > 0 else 25.0)
            prod_category = cat_item.category if cat_item else "Retail FMCG"
            prod_barcode = cat_item.barcode if cat_item else (item.product_id or "")

            product = models.Product(
                id=str(uuid.uuid4()),
                user_id=user_id,
                name=prod_name,
                category=prod_category,
                price=prod_price,
                stock=50,
                low_stock_threshold=5,
                barcode=prod_barcode
            )
            db.add(product)
            db.flush()

        if product:
            products_by_id[product.id] = product
            requested_quantities[product.id] = requested_quantities.get(product.id, 0) + item.quantity
            authoritative_unit_price = product.price

        line_total = item.quantity * authoritative_unit_price
        authoritative_subtotal += line_total
        processed_items.append((item, product, authoritative_unit_price, line_total))

    # Calculate final authoritative bill total
    authoritative_total_amount = authoritative_subtotal + body.tax_amount

    # Create bill record
    bill = models.Bill(
        id=bill_id,
        user_id=user_id,
        total_amount=authoritative_total_amount,
        tax_amount=body.tax_amount,
        payment_mode=body.payment_mode or "cash"
    )
    db.add(bill)

    # Create bill items
    for item, product, auth_unit_price, auth_line_total in processed_items:
        bill_item = models.BillItem(
            id=str(uuid.uuid4()),
            bill_id=bill_id,
            product_id=product.id if product else None,
            product_name=product.name if product else item.product_name,
            quantity=item.quantity,
            unit_price=auth_unit_price,
            total_price=auth_line_total
        )
        db.add(bill_item)

    # Atomically reduce stock for all sold products
    for product_id, total_requested in sorted(requested_quantities.items()):
        db.query(models.Product).filter(
            models.Product.id == product_id
        ).update({
            models.Product.stock: func.greatest(0, models.Product.stock - total_requested)
        }, synchronize_session=False)

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        if body.idempotency_key:
            existing_bill = db.query(models.Bill).filter(models.Bill.id == bill_id).first()
            if existing_bill:
                return existing_bill
        raise

    db.refresh(bill)
    return bill


@router.get("", response_model=List[schemas.BillResponse])
def list_bills(
    user_id: OptionalUser,
    limit: int = 100,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """
    List all completed bills across store transactions for POS, Mobile History, and Admin Portal.
    """
    bills = db.query(models.Bill).order_by(models.Bill.created_at.desc()).offset(offset).limit(limit).all()
    return bills


@router.get("/{bill_id}", response_model=schemas.BillResponse)
def get_bill(
    bill_id: str,
    user_id: OptionalUser,
    db: Session = Depends(get_db)
):
    bill = db.query(models.Bill).filter(
        models.Bill.id == bill_id
    ).first()
    if not bill:
        raise HTTPException(status_code=404, detail="Bill not found")
    return bill

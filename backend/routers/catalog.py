from typing import List, Optional
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from database import get_db
import models
import schemas

router = APIRouter(prefix="/catalog", tags=["Master Catalog"])


@router.get("", response_model=List[schemas.MasterCatalogResponse])
@router.get("/search", response_model=List[schemas.MasterCatalogResponse])
def search_catalog(
    search: Optional[str] = None,
    q: Optional[str] = None,
    category: Optional[str] = None,
    limit: int = 50,
    db: Session = Depends(get_db)
):
    query_str = search or q
    query = db.query(models.MasterCatalog)
    if query_str:
        query = query.filter(models.MasterCatalog.name.ilike(f"%{query_str}%"))
    if category:
        query = query.filter(models.MasterCatalog.category == category)
    return query.order_by(models.MasterCatalog.name).limit(limit).all()



@router.get("/all", response_model=List[schemas.MasterCatalogResponse])
def get_all_catalog(
    db: Session = Depends(get_db)
):
    """
    Returns full master catalog (6,000 items) for local OCR reference context.
    """
    return db.query(models.MasterCatalog).all()

from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, computed_field


# ─── Store Profile Schemas ─────────────────────────────────────────────────────

class StoreProfileCreate(BaseModel):
    name: str = ""
    address: str = ""
    phone: str = ""
    gst: str = ""
    upi: str = ""
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class StoreProfileResponse(BaseModel):
    user_id: str
    name: str
    address: str
    phone: str
    gst: str
    upi: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    updated_at: datetime

    model_config = {"from_attributes": True}


# ─── Product Schemas ───────────────────────────────────────────────────────────

class ProductCreate(BaseModel):
    name: str
    barcode: Optional[str] = None
    category: str = "General"
    brand: Optional[str] = None
    size: Optional[str] = None
    available_sizes: Optional[str] = None
    floor: Optional[str] = None
    section: Optional[str] = None
    aisle: Optional[str] = None
    rack_number: Optional[str] = None
    price: float
    stock: int = 0
    low_stock_threshold: int = 5
    image_url: Optional[str] = None


class ProductUpdate(BaseModel):
    name: Optional[str] = None
    barcode: Optional[str] = None
    category: Optional[str] = None
    brand: Optional[str] = None
    size: Optional[str] = None
    available_sizes: Optional[str] = None
    floor: Optional[str] = None
    section: Optional[str] = None
    aisle: Optional[str] = None
    rack_number: Optional[str] = None
    price: Optional[float] = None
    stock: Optional[int] = None
    low_stock_threshold: Optional[int] = None
    image_url: Optional[str] = None


class StockUpdateRequest(BaseModel):
    stock: int


class ProductResponse(BaseModel):
    id: str
    user_id: str
    name: str
    barcode: Optional[str] = None
    category: str
    brand: Optional[str] = None
    size: Optional[str] = None
    available_sizes: Optional[str] = None
    floor: Optional[str] = None
    section: Optional[str] = None
    aisle: Optional[str] = None
    rack_number: Optional[str] = None
    price: float
    stock: int
    low_stock_threshold: int
    image_url: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    model_config = {"from_attributes": True}


class NearbyStoreResponse(BaseModel):
    store_id: str
    store_name: str
    address: str
    available: bool
    price: float
    distance_km: Optional[float] = None
    last_updated: str
    floor: Optional[str] = None
    section: Optional[str] = None
    aisle: Optional[str] = None
    rack_number: Optional[str] = None


class ProductNearbyResponse(BaseModel):
    product: ProductResponse
    alternatives: List[NearbyStoreResponse]


class MasterCatalogResponse(BaseModel):
    id: str
    name: str
    category: str
    brand: Optional[str] = None
    size: Optional[str] = None
    available_sizes: Optional[str] = None
    floor: Optional[str] = None
    section: Optional[str] = None
    aisle: Optional[str] = None
    rack_number: Optional[str] = None
    suggested_price: float
    barcode: Optional[str] = None

    model_config = {"from_attributes": True}


class LocationMetadata(BaseModel):
    store_id: str
    store_name: str
    address: Optional[str] = None
    floor: Optional[str] = None
    section: Optional[str] = None
    aisle: Optional[str] = None
    rack_number: Optional[str] = None
    last_updated: str = "Today"


class SizeRecommendation(BaseModel):
    recommended_size: Optional[str] = None
    available_sizes: List[str] = []
    unavailable_sizes: List[str] = []
    size_matched_user_profile: bool = False
    size_chart: Optional[dict] = None


class CheckoutPreview(BaseModel):
    status: str = "COMING_NEXT"
    label: str = "Checkout & Payment — Coming Next"
    description: str = "Production payment gateway and checkout workflow ready for activation."


class InstantFindResponse(BaseModel):
    status: str
    match_type: str = "NONE"
    match_label: str = "No match"
    confidence: float = 0.0
    product: Optional[ProductResponse] = None
    location: Optional[LocationMetadata] = None
    size_recommendation: Optional[SizeRecommendation] = None
    alternatives: List[NearbyStoreResponse] = []
    checkout_preview: CheckoutPreview = Field(default_factory=CheckoutPreview)
    loss_prevention_alert: Optional[dict] = None




# ─── Bill Schemas ──────────────────────────────────────────────────────────────

class BillItemCreate(BaseModel):
    product_id: Optional[str] = None
    product_name: str
    quantity: int = Field(..., ge=1)
    unit_price: float
    total_price: float


class BillCreate(BaseModel):
    idempotency_key: Optional[str] = None
    customer_name: Optional[str] = None
    customer_phone: Optional[str] = None
    items: List[BillItemCreate]
    total_amount: float
    tax_amount: float = Field(0.0, ge=0.0)
    payment_mode: str = "cash"


class BillItemResponse(BaseModel):
    id: str
    product_id: Optional[str]
    product_name: str
    quantity: int
    unit_price: float
    total_price: float

    model_config = {"from_attributes": True}


class BillResponse(BaseModel):
    id: str
    user_id: str
    customer_name: Optional[str] = "Abhradeep Das"
    customer_phone: Optional[str] = "+91 98301 24510"
    total_amount: float
    tax_amount: float
    payment_mode: str
    created_at: datetime
    items: List[BillItemResponse] = []

    @computed_field
    def bill_number(self) -> str:
        if self.id and self.id.startswith("BILL_"):
            return self.id
        return f"INV-{self.id[:8].upper()}" if self.id else "INV-0001"

    model_config = {"from_attributes": True}


# ─── Analytics Schemas ─────────────────────────────────────────────────────────

class DailyRevenue(BaseModel):
    date: str
    revenue: float
    bill_count: int


class TopProduct(BaseModel):
    product_name: str = ""
    quantity_sold: int = 0
    revenue: float = 0.0
    name: Optional[str] = None
    sales_count: Optional[int] = None

    def model_post_init(self, __context):
        if not self.name:
            self.name = self.product_name
        if self.sales_count is None:
            self.sales_count = self.quantity_sold


class StockRecommendationItem(BaseModel):
    product_id: str
    product_name: str
    current_stock: int
    recommended_reorder: int
    category: str
    peak_window: str = "General Demand"
    sales_velocity: str = "Moderate"
    reasoning: str = "Stock level requires attention based on sales velocity."
    urgency_level: str = "MEDIUM"


class MarketTrendInsight(BaseModel):
    title: str
    description: str
    recommended_product: str
    action_type: str = "RESTOCK"
    badge_label: str = "🌐 Market Trend"


class AnalyticsSummary(BaseModel):
    total_revenue: float
    total_bills: int
    total_products: int
    low_stock_count: int
    average_bill_value: float = 0.0
    payment_breakdown: dict = {"cash": 0.0, "upi": 0.0, "card": 0.0}
    recent_bills: List[BillResponse] = []
    top_products: List[TopProduct] = []
    daily_revenue: List[DailyRevenue] = []
    stock_recommendations: List[StockRecommendationItem] = []
    market_trends: List[MarketTrendInsight] = []


# ─── AI Schemas ────────────────────────────────────────────────────────────────

class NearbyStoreProduct(BaseModel):
    product_id: str
    product_name: str
    store_id: str
    store_name: str
    store_address: Optional[str] = ""
    store_phone: Optional[str] = ""
    price: float
    stock: int
    distance_km: Optional[float] = None
    time_ago: str = ""

class AIChatRequest(BaseModel):
    message: str

class AIChatResponse(BaseModel):
    success: bool
    response: Optional[str] = None
    nearby_options: Optional[List[NearbyStoreProduct]] = None
    error: Optional[str] = None

class InterStoreOrderItem(BaseModel):
    product_id: str
    product_name: str
    quantity: int
    unit_price: float

class InterStoreOrderRequest(BaseModel):
    seller_store_id: str
    seller_store_name: str
    items: List[InterStoreOrderItem]
    total_amount: float
    delivery_note: Optional[str] = "Inter-store transfer request"

class InterStoreOrderResponse(BaseModel):
    order_id: str
    status: str
    seller_store_name: str
    total_amount: float
    message: str


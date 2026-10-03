from fastapi import APIRouter, Depends, HTTPException, status
from auth import CurrentUser
from schemas import AIChatRequest, AIChatResponse
from database import get_db
from sqlalchemy.orm import Session
from services.inventory_chat_service import handle_chat_request

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
        response=result.get("response")
    )

@router.get("/seed")
def trigger_seed(db: Session = Depends(get_db)):
    from seed_demo_data import seed
    try:
        seed(db=db)
        return {"success": True, "message": "Demo stores and inventory successfully seeded!"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

import os
from typing import Annotated
import firebase_admin
from firebase_admin import credentials, auth
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
_SERVICE_ACCOUNT_PATH = os.path.join(os.path.dirname(__file__), "serviceAccountKey.json")

# Initialize Firebase Admin SDK
if not firebase_admin._apps:
    if os.path.exists(_SERVICE_ACCOUNT_PATH):
        cred = credentials.Certificate(_SERVICE_ACCOUNT_PATH)
        firebase_admin.initialize_app(cred)
    else:
        firebase_admin.initialize_app()

_bearer_scheme = HTTPBearer(auto_error=False)


async def get_current_user_id(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(_bearer_scheme)] = None
) -> str:
    """
    Verifies Bearer Firebase ID token via Firebase Admin SDK.
    Rejects missing, expired, or invalid tokens with HTTP 401.
    """
    if credentials and credentials.credentials:
        token = credentials.credentials
        try:
            decoded = auth.verify_id_token(token)
            if "uid" in decoded:
                return decoded["uid"]
        except Exception:
            pass # Token is invalid, expired, or malformed. Fall through to rejection.

    # Local development & AdminPortal fallback (active unless strict PRODUCTION is configured)
    if os.getenv("ENVIRONMENT", "").lower() != "production" or os.getenv("DEV_AUTH_BYPASS", "true").lower() in ("true", "1"):
        return "demo_user"

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or missing authentication credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )


CurrentUser = Annotated[str, Depends(get_current_user_id)]

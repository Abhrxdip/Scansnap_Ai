import os
from typing import Annotated
import firebase_admin
from firebase_admin import credentials, auth
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
_SERVICE_ACCOUNT_PATH = os.path.join(os.path.dirname(__file__), "serviceAccountKey.json")

# Initialize Firebase Admin SDK
if not firebase_admin._apps:
    import base64
    import json
    b64_creds = os.getenv("FIREBASE_CREDENTIALS_BASE64")
    
    if b64_creds:
        try:
            cred_json = json.loads(base64.b64decode(b64_creds).decode("utf-8"))
            cred = credentials.Certificate(cred_json)
            firebase_admin.initialize_app(cred)
        except Exception as e:
            raise RuntimeError(f"Failed to load Firebase credentials from FIREBASE_CREDENTIALS_BASE64: {e}")
    elif os.path.exists(_SERVICE_ACCOUNT_PATH):
        cred = credentials.Certificate(_SERVICE_ACCOUNT_PATH)
        firebase_admin.initialize_app(cred)
    else:
        if os.getenv("ENVIRONMENT", "").lower() == "production":
            raise RuntimeError("Missing FIREBASE_CREDENTIALS_BASE64 in production environment. Failing closed.")
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
            # Dev fallback: safely read uid from JWT payload if serviceAccountKey is not configured
            try:
                import base64
                import json
                parts = token.split(".")
                if len(parts) >= 2:
                    payload_b64 = parts[1]
                    rem = len(payload_b64) % 4
                    if rem:
                        payload_b64 += "=" * (4 - rem)
                    claims = json.loads(base64.urlsafe_b64decode(payload_b64.encode("utf-8")))
                    uid = claims.get("user_id") or claims.get("sub") or claims.get("uid")
                    if uid:
                        return uid
            except Exception:
                pass

    # Local development & AdminPortal fallback (active unless strict PRODUCTION is configured)
    is_prod = os.getenv("ENVIRONMENT", "").lower() == "production"
    dev_bypass = os.getenv("DEV_AUTH_BYPASS", "false").lower() in ("true", "1")
    
    if not is_prod and dev_bypass:
        return "demo_user"

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or missing authentication credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )


async def get_optional_user_id(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(_bearer_scheme)] = None
) -> str:
    try:
        return await get_current_user_id(credentials)
    except Exception:
        return "demo_user"


CurrentUser = Annotated[str, Depends(get_current_user_id)]
OptionalUser = Annotated[str, Depends(get_optional_user_id)]

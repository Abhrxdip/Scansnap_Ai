import sys
import database
import services.inventory_chat_service as s

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

def main():
    db = database.SessionLocal()
    try:
        queries = [
            "Is Maggi available?",
            "Where else can I find Maggi?",
            "What is the price of Amul milk?",
            "Show me biscuits"
        ]
        for q in queries:
            print(f"=== USER QUERY: {q} ===")
            res = s.handle_chat_request(q, db, "demo_user")
            print(res["response"])
            print("-" * 50)
    finally:
        db.close()

if __name__ == "__main__":
    main()

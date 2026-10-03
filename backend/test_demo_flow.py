import database
import services.inventory_chat_service as s

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

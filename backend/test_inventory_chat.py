import unittest
from unittest.mock import MagicMock
from services.inventory_chat_service import handle_chat_request
import models
from datetime import datetime, timedelta

class TestInventoryChatService(unittest.TestCase):
    
    def test_product_availability_in_stock(self):
        mock_db = MagicMock()
        mock_product = models.Product(
            id="123", user_id="store1", name="Maggi Noodles", price=12.0, stock=50, updated_at=datetime.utcnow()
        )
        mock_db.query().filter().limit().all.return_value = [mock_product]
        
        result = handle_chat_request("Is Maggi available?", mock_db, "store1")
        
        self.assertTrue(result["success"])
        self.assertIn("Maggi Noodles is in stock", result["response"])
        self.assertIn("50 available", result["response"])
        self.assertIn("12.0", result["response"])
        
    def test_product_out_of_stock_stale(self):
        mock_db = MagicMock()
        mock_product = models.Product(
            id="123", user_id="store1", name="Amul Milk", price=32.0, stock=0, updated_at=datetime.utcnow() - timedelta(days=3)
        )
        mock_db.query().filter().limit().all.return_value = [mock_product]
        
        result = handle_chat_request("Is Amul milk available?", mock_db, "store1")
        
        self.assertTrue(result["success"])
        self.assertIn("Amul Milk is out of stock", result["response"])
        self.assertIn("may be stale", result["response"])
        
    def test_product_not_found(self):
        mock_db = MagicMock()
        mock_db.query().filter().limit().all.return_value = []
        
        result = handle_chat_request("Do you have UnknownItem?", mock_db, "store1")
        
        self.assertTrue(result["success"])
        self.assertIn("couldn't find any product matching", result["response"])
        
    def test_ambiguous_query(self):
        mock_db = MagicMock()
        
        result = handle_chat_request("", mock_db, "store1")
        
        self.assertTrue(result["success"])
        self.assertIn("not quite sure", result["response"])

    def test_find_elsewhere_found(self):
        mock_db = MagicMock()
        curr_store = models.StoreProfile(user_id="store1", name="Store 1", latitude=12.9716, longitude=77.5946)
        mock_db.query().filter().first.return_value = curr_store

        other_store = models.StoreProfile(user_id="store2", name="Super Bazaar", latitude=12.9780, longitude=77.5990)
        other_product = models.Product(id="p2", user_id="store2", name="Maggi Noodles 2-Minute", price=14.0, stock=20, updated_at=datetime.utcnow())

        mock_db.query().join().filter().all.return_value = [(other_product, other_store)]

        result = handle_chat_request("Where else can I find Maggi?", mock_db, "store1")

        self.assertTrue(result["success"])
        self.assertIn("Found 'maggi' at nearby stores:", result["response"])
        self.assertIn("Super Bazaar", result["response"])
        self.assertIn("14.0", result["response"])

if __name__ == '__main__':
    unittest.main()

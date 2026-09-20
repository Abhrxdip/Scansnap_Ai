const API_BASE = '/api';

export async function fetchAnalytics() {
  const res = await fetch(`${API_BASE}/analytics/summary`);
  if (!res.ok) throw new Error('Failed to fetch analytics summary');
  return res.json();
}

export async function fetchProducts(search = '', category = '') {
  const params = new URLSearchParams();
  if (search) params.append('search', search);
  if (category) params.append('category', category);
  
  const res = await fetch(`${API_BASE}/products?${params.toString()}`);
  if (!res.ok) throw new Error('Failed to fetch products');
  return res.json();
}

export async function createProduct(productData) {
  const res = await fetch(`${API_BASE}/products`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(productData),
  });
  if (!res.ok) throw new Error('Failed to create product');
  return res.json();
}

export async function updateProduct(id, productData) {
  const res = await fetch(`${API_BASE}/products/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(productData),
  });
  if (!res.ok) throw new Error('Failed to update product');
  return res.json();
}

export async function deleteProduct(id) {
  const res = await fetch(`${API_BASE}/products/${id}`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error('Failed to delete product');
  return res.json();
}

export async function fetchBills() {
  const res = await fetch(`${API_BASE}/bills`);
  if (!res.ok) throw new Error('Failed to fetch bills');
  return res.json();
}

export async function searchMasterCatalog(query) {
  if (!query || query.trim().length < 2) return [];
  const res = await fetch(`${API_BASE}/catalog/search?q=${encodeURIComponent(query)}&limit=15`);
  if (!res.ok) throw new Error('Failed to search master catalog');
  return res.json();
}

export async function detectObjectsInImage(file) {
  const formData = new FormData();
  formData.append('file', file);
  const res = await fetch(`${API_BASE}/detect/image`, {
    method: 'POST',
    body: formData,
  });
  if (!res.ok) throw new Error('Detection failed');
  return res.json();
}

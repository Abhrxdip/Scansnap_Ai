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

export async function detectObjectsInImage(file, conf = 0.25) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('conf', conf);
  const res = await fetch(`${API_BASE}/detect/image`, {
    method: 'POST',
    body: formData,
  });
  if (!res.ok) {
    const errData = await res.json().catch(() => ({}));
    throw new Error(errData.detail || 'Detection failed');
  }
  return res.json();
}

// ─── Offers & Smart Combos API ──────────────────────────────────────────────

export async function fetchFestivals() {
  const res = await fetch(`${API_BASE}/offers/festivals`);
  if (!res.ok) throw new Error('Failed to fetch festival discounts');
  return res.json();
}

export async function overrideFestival(festivalName) {
  const res = await fetch(`${API_BASE}/offers/override-festival`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ festival_name: festivalName }),
  });
  if (!res.ok) throw new Error('Failed to override festival');
  return res.json();
}

export async function fetchCombos() {
  const res = await fetch(`${API_BASE}/offers/combos`);
  if (!res.ok) throw new Error('Failed to fetch smart combos');
  return res.json();
}

export async function calculateCartOffers(items, applyFestival = true, applyCombos = true) {
  const res = await fetch(`${API_BASE}/offers/calculate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      items,
      apply_festival: applyFestival,
      apply_combos: applyCombos,
    }),
  });
  if (!res.ok) throw new Error('Failed to calculate offers');
  return res.json();
}

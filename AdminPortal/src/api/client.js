// ScanSnap AI — Unified API Client
// Automatically tries /api proxy first, with transparent fallback to http://127.0.0.1:8000
const DIRECT_BACKEND = 'https://scansnapai-production.up.railway.app';

export async function apiFetch(endpoint, options = {}) {
  try {
    const res = await fetch(`${DIRECT_BACKEND}${endpoint}`, options);
    return res;
  } catch (err) {
    // Transparent local fallback
    try {
      return await fetch(`/api${endpoint}`, options);
    } catch {
      return fetch(`http://127.0.0.1:8000${endpoint}`, options);
    }
  }
}

export async function fetchAnalytics() {
  const res = await apiFetch('/analytics/summary');
  if (!res.ok) throw new Error('Failed to fetch analytics summary');
  return res.json();
}

export async function fetchProducts(search = '', category = '') {
  const params = new URLSearchParams();
  if (search) params.append('search', search);
  if (category) params.append('category', category);
  
  const res = await apiFetch(`/products?${params.toString()}`);
  if (!res.ok) throw new Error('Failed to fetch products');
  return res.json();
}

export async function syncRealtimeInventory() {
  const res = await apiFetch('/products/sync-catalog', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!res.ok) throw new Error('Failed to sync catalog inventory');
  return res.json();
}

export async function createProduct(productData) {
  const res = await apiFetch('/products', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(productData),
  });
  if (!res.ok) throw new Error('Failed to create product');
  return res.json();
}

export async function updateProduct(id, productData) {
  const res = await apiFetch(`/products/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(productData),
  });
  if (!res.ok) throw new Error('Failed to update product');
  return res.json();
}

export async function deleteProduct(id) {
  const res = await apiFetch(`/products/${id}`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error('Failed to delete product');
  return res.json();
}

export async function fetchBills() {
  const res = await apiFetch('/bills');
  if (!res.ok) throw new Error('Failed to fetch bills');
  return res.json();
}

export async function searchMasterCatalog(query) {
  if (!query || query.trim().length < 2) return [];
  const res = await apiFetch(`/catalog/search?q=${encodeURIComponent(query)}&limit=15`);
  if (!res.ok) throw new Error('Failed to search master catalog');
  return res.json();
}

export async function detectObjectsInImage(file, conf = 0.25) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('conf', conf);
  try {
    const res = await apiFetch('/detect/image', {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw new Error(errData.detail || `Server error (${res.status}): ${res.statusText}`);
    }
    return res.json();
  } catch (err) {
    if (err.message && (err.message.includes('fetch') || err.message.includes('connect'))) {
      throw new Error('Unable to connect to backend server. Please verify backend is running on port 8000.');
    }
    throw err;
  }
}

// ─── Offers & Smart Combos API ──────────────────────────────────────────────

export async function fetchFestivals() {
  const res = await apiFetch('/offers/festivals');
  if (!res.ok) throw new Error('Failed to fetch festival discounts');
  return res.json();
}

export async function overrideFestival(festivalName) {
  const res = await apiFetch('/offers/override-festival', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ festival_name: festivalName }),
  });
  if (!res.ok) throw new Error('Failed to override festival');
  return res.json();
}

export async function fetchCombos() {
  const res = await apiFetch('/offers/combos');
  if (!res.ok) throw new Error('Failed to fetch smart combos');
  return res.json();
}

export async function calculateCartOffers(items, applyFestival = true, applyCombos = true) {
  const res = await apiFetch('/offers/calculate', {
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

export async function seedDemoBills() {
  const res = await apiFetch('/bills/seed-demo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!res.ok) throw new Error('Failed to seed demo bills');
  return res.json();
}

// ─── Loss Prevention & Shrink Shield Client ─────────────────────────────────

export async function fetchLossPreventionStats() {
  const res = await apiFetch('/loss-prevention/stats');
  if (!res.ok) throw new Error('Failed to fetch loss prevention stats');
  return res.json();
}

export async function fetchLossPreventionIncidents(limit = 20) {
  const res = await apiFetch(`/loss-prevention/incidents?limit=${limit}`);
  if (!res.ok) throw new Error('Failed to fetch loss prevention incidents');
  return res.json();
}

export async function simulateLossPreventionScenario(scenarioId) {
  const res = await apiFetch(`/loss-prevention/simulate/${scenarioId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  });
  if (!res.ok) throw new Error(`Simulation failed: ${scenarioId}`);
  return res.json();
}

export async function resolveLossPreventionIncident(incidentId, action = 'confirm', note = '') {
  const res = await apiFetch(`/loss-prevention/resolve/${incidentId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action, note })
  });
  if (!res.ok) throw new Error('Failed to resolve incident');
  return res.json();
}


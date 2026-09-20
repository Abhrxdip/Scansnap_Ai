import React, { useState, useEffect, useMemo } from 'react';
import { 
  Package, 
  Search, 
  Plus, 
  FileSpreadsheet, 
  Download, 
  Upload, 
  Edit2, 
  Trash2, 
  Check, 
  X, 
  AlertTriangle, 
  Database, 
  Sparkles,
  RefreshCw,
  PlusCircle,
  MinusCircle
} from 'lucide-react';
import { 
  fetchProducts, 
  createProduct, 
  updateProduct, 
  deleteProduct, 
  searchMasterCatalog 
} from '../api/client';

export default function InventoryPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  
  // Modals
  const [isAddEditOpen, setIsAddEditOpen] = useState(false);
  const [isCsvModalOpen, setIsCsvModalOpen] = useState(false);
  const [isCatalogModalOpen, setIsCatalogModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    barcode: '',
    brand: '',
    category: 'Groceries',
    price: '',
    cost_price: '',
    stock_quantity: '',
    unit: 'pcs',
    reorder_level: 5,
  });

  // Master catalog search
  const [catalogQuery, setCatalogQuery] = useState('');
  const [catalogResults, setCatalogResults] = useState([]);
  const [isSearchingCatalog, setIsSearchingCatalog] = useState(false);

  // CSV Import State
  const [csvFile, setCsvFile] = useState(null);
  const [csvPreview, setCsvPreview] = useState([]);
  const [csvUploading, setCsvUploading] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (msg, type = 'success') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  };

  const loadProducts = async () => {
    try {
      setLoading(true);
      const data = await fetchProducts();
      setProducts(data);
    } catch (err) {
      console.error(err);
      showToast('Error loading inventory products', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const categories = useMemo(() => {
    const set = new Set(products.map(p => p.category).filter(Boolean));
    return ['ALL', ...Array.from(set)];
  }, [products]);

  const filteredProducts = useMemo(() => {
    return products.filter(p => {
      const matchSearch = 
        p.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        p.barcode?.includes(searchQuery) ||
        p.brand?.toLowerCase().includes(searchQuery.toLowerCase());
      const matchCategory = selectedCategory === 'ALL' || p.category === selectedCategory;
      return matchSearch && matchCategory;
    });
  }, [products, searchQuery, selectedCategory]);

  // Inventory valuation & metrics
  const totalValuation = useMemo(() => {
    return products.reduce((sum, p) => sum + (Number(p.price || 0) * Number(p.stock_quantity || 0)), 0);
  }, [products]);

  const lowStockCount = useMemo(() => {
    return products.filter(p => Number(p.stock_quantity || 0) <= Number(p.reorder_level || 5)).length;
  }, [products]);

  // Handle Add/Edit
  const openAddModal = () => {
    setEditingProduct(null);
    setFormData({
      name: '',
      barcode: `890${Math.floor(1000000000 + Math.random() * 9000000000)}`,
      brand: '',
      category: 'Groceries',
      price: '',
      cost_price: '',
      stock_quantity: 25,
      unit: 'pcs',
      reorder_level: 5,
    });
    setIsAddEditOpen(true);
  };

  const openEditModal = (prod) => {
    setEditingProduct(prod);
    setFormData({
      name: prod.name || '',
      barcode: prod.barcode || '',
      brand: prod.brand || '',
      category: prod.category || 'Groceries',
      price: prod.price || '',
      cost_price: prod.cost_price || '',
      stock_quantity: prod.stock_quantity || 0,
      unit: prod.unit || 'pcs',
      reorder_level: prod.reorder_level || 5,
    });
    setIsAddEditOpen(true);
  };

  const handleSaveProduct = async (e) => {
    e.preventDefault();
    if (!formData.name || !formData.price) {
      showToast('Name and Price are required', 'error');
      return;
    }

    try {
      const payload = {
        ...formData,
        price: parseFloat(formData.price),
        cost_price: formData.cost_price ? parseFloat(formData.cost_price) : 0,
        stock_quantity: parseInt(formData.stock_quantity, 10) || 0,
        reorder_level: parseInt(formData.reorder_level, 10) || 5,
      };

      if (editingProduct) {
        await updateProduct(editingProduct.id, payload);
        showToast(`Updated "${formData.name}" successfully!`);
      } else {
        await createProduct(payload);
        showToast(`Added "${formData.name}" to inventory!`);
      }

      setIsAddEditOpen(false);
      loadProducts();
    } catch (err) {
      console.error(err);
      showToast('Failed to save product.', 'error');
    }
  };

  const handleDeleteProduct = async (id, name) => {
    if (!window.confirm(`Are you sure you want to delete "${name}" from store inventory?`)) {
      return;
    }
    try {
      await deleteProduct(id);
      showToast(`Deleted "${name}"`);
      loadProducts();
    } catch (err) {
      console.error(err);
      showToast('Could not delete product.', 'error');
    }
  };

  // Quick Stock adjustment directly from row
  const adjustStock = async (prod, delta) => {
    const newQty = Math.max(0, (prod.stock_quantity || 0) + delta);
    try {
      // Optimistic update
      setProducts(prev => prev.map(p => p.id === prod.id ? { ...p, stock_quantity: newQty } : p));
      await updateProduct(prod.id, { ...prod, stock_quantity: newQty });
    } catch (err) {
      console.error(err);
      loadProducts();
    }
  };

  // Catalog search
  const handleSearchCatalog = async (q) => {
    setCatalogQuery(q);
    if (q.trim().length < 2) {
      setCatalogResults([]);
      return;
    }
    try {
      setIsSearchingCatalog(true);
      const results = await searchMasterCatalog(q);
      setCatalogResults(results || []);
    } catch (err) {
      console.error(err);
    } finally {
      setIsSearchingCatalog(false);
    }
  };

  const handleImportFromCatalog = async (catItem) => {
    try {
      const payload = {
        name: catItem.name,
        barcode: catItem.barcode || `890${Math.floor(1000000000 + Math.random() * 9000000000)}`,
        brand: catItem.brand || '',
        category: catItem.category || 'Groceries',
        price: parseFloat(catItem.mrp || catItem.price || 40),
        cost_price: parseFloat(catItem.cost_price || (catItem.mrp ? catItem.mrp * 0.8 : 32)),
        stock_quantity: 50,
        unit: catItem.unit || 'pcs',
        reorder_level: 5,
      };

      await createProduct(payload);
      showToast(`Imported "${catItem.name}" to inventory!`);
      loadProducts();
    } catch (err) {
      console.error(err);
      showToast('Failed to import catalog product', 'error');
    }
  };

  // CSV Export
  const handleExportCsv = () => {
    if (products.length === 0) {
      showToast('No products to export', 'error');
      return;
    }

    const headers = ['barcode', 'name', 'brand', 'category', 'price', 'cost_price', 'stock_quantity', 'unit', 'reorder_level'];
    const rows = products.map(p => [
      `"${p.barcode || ''}"`,
      `"${(p.name || '').replace(/"/g, '""')}"`,
      `"${(p.brand || '').replace(/"/g, '""')}"`,
      `"${p.category || ''}"`,
      p.price || 0,
      p.cost_price || 0,
      p.stock_quantity || 0,
      `"${p.unit || 'pcs'}"`,
      p.reorder_level || 5
    ]);

    const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `scansnap_inventory_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('Inventory exported as CSV successfully!');
  };

  // CSV Import parsing
  const handleCsvFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setCsvFile(file);

    const reader = new FileReader();
    reader.onload = (evt) => {
      const text = evt.target.result;
      const lines = text.split(/\r\n|\n/).filter(line => line.trim() !== '');
      if (lines.length < 2) {
        showToast('CSV must have a header row and at least 1 data row', 'error');
        return;
      }
      
      const headers = lines[0].split(',').map(h => h.trim().toLowerCase().replace(/"/g, ''));
      const parsed = [];

      for (let i = 1; i < lines.length; i++) {
        // Simple regex to parse CSV with quoted fields
        const values = lines[i].match(/(".*?"|[^",\s]+)(?=\s*,|\s*$)/g) || lines[i].split(',');
        const row = {};
        headers.forEach((h, idx) => {
          row[h] = values[idx] ? values[idx].replace(/^"|"$/g, '').trim() : '';
        });

        if (row.name) {
          parsed.push({
            name: row.name,
            barcode: row.barcode || `890${Math.floor(1000000000 + Math.random() * 9000000000)}`,
            brand: row.brand || '',
            category: row.category || 'Groceries',
            price: parseFloat(row.price) || 50,
            cost_price: parseFloat(row.cost_price) || 40,
            stock_quantity: parseInt(row.stock_quantity, 10) || 20,
            unit: row.unit || 'pcs',
            reorder_level: parseInt(row.reorder_level, 10) || 5,
          });
        }
      }

      setCsvPreview(parsed);
    };
    reader.readAsText(file);
  };

  const handleBulkUploadCsv = async () => {
    if (csvPreview.length === 0) return;
    setCsvUploading(true);
    let successCount = 0;

    for (const item of csvPreview) {
      try {
        await createProduct(item);
        successCount++;
      } catch (err) {
        console.warn('Skipped duplicate or error:', item.name);
      }
    }

    setCsvUploading(false);
    setIsCsvModalOpen(false);
    setCsvFile(null);
    setCsvPreview([]);
    showToast(`Successfully imported ${successCount} products from CSV!`);
    loadProducts();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Toast */}
      {toast && (
        <div className="toast-banner" style={{ background: toast.type === 'error' ? '#FFE4E6' : '#DCFCE7', color: toast.type === 'error' ? '#9F1239' : '#14532D' }}>
          {toast.type === 'error' ? <AlertTriangle size={18} /> : <Check size={18} />}
          <span>{toast.msg}</span>
        </div>
      )}

      {/* Top summary cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFF' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#666' }}>Active Catalog SKUs</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px' }}>
            {products.length} Items
          </div>
        </div>

        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFF' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#666' }}>Stock Valuation (Retail)</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px', color: '#1D4ED8' }}>
            ₹{totalValuation.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
          </div>
        </div>

        <div className="neu-box" style={{ padding: '16px 20px', background: lowStockCount > 0 ? '#FEF2F2' : '#FFF' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: lowStockCount > 0 ? '#DC2626' : '#666' }}>Low Stock SKUs</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px', color: lowStockCount > 0 ? '#DC2626' : '#059669' }}>
            {lowStockCount} Items
          </div>
        </div>

        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFFDF7' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#666' }}>National Grocery Index</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px', color: '#F59E0B' }}>
            117,000+
          </div>
        </div>
      </div>

      {/* Action Bar */}
      <div className="neu-box" style={{ padding: '18px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: '1 1 320px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#666' }} />
            <input 
              type="text"
              placeholder="Search by product name, barcode (890...), or brand..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="neu-input"
              style={{ paddingLeft: '38px' }}
            />
          </div>

          <select 
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="neu-input"
            style={{ width: '160px' }}
          >
            {categories.map(c => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <button 
            onClick={() => setIsCatalogModalOpen(true)}
            className="neu-btn neu-btn-yellow"
            title="Search Indian Grocery Database"
          >
            <Database size={15} /> 117K Master Catalog
          </button>

          <button 
            onClick={() => setIsCsvModalOpen(true)}
            className="neu-btn"
            title="Import/Export CSV"
          >
            <FileSpreadsheet size={15} color="#059669" /> CSV Studio
          </button>

          <button 
            onClick={openAddModal}
            className="neu-btn neu-btn-primary"
          >
            <Plus size={16} /> Add Product
          </button>
        </div>
      </div>

      {/* Main Table */}
      <div className="neu-table-container">
        <table className="neu-table">
          <thead>
            <tr>
              <th>Barcode</th>
              <th>Product Details</th>
              <th>Category</th>
              <th>Unit Price (MRP)</th>
              <th>Cost Price</th>
              <th>Stock Level</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '40px' }}>
                  <RefreshCw size={28} className="spin-anim" style={{ margin: '0 auto 8px', color: '#1D4ED8' }} />
                  <p style={{ fontWeight: 700 }}>Loading inventory records...</p>
                </td>
              </tr>
            ) : filteredProducts.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '48px', color: '#666' }}>
                  <Package size={40} style={{ margin: '0 auto 12px', color: '#AAA' }} />
                  <h4 style={{ fontSize: '16px', fontWeight: 800 }}>No products matched</h4>
                  <p style={{ fontSize: '13px', marginTop: '4px' }}>
                    Try searching something else or click <strong>"117K Master Catalog"</strong> to import popular Indian products.
                  </p>
                </td>
              </tr>
            ) : (
              filteredProducts.map((p) => {
                const isLow = Number(p.stock_quantity || 0) <= Number(p.reorder_level || 5);
                const margin = p.price && p.cost_price ? Math.round(((p.price - p.cost_price) / p.price) * 100) : 20;

                return (
                  <tr key={p.id}>
                    <td>
                      <code style={{ background: '#F3F4F6', padding: '4px 8px', borderRadius: '4px', border: '1px solid #0A0A0A', fontSize: '12px', fontWeight: 800 }}>
                        {p.barcode || 'N/A'}
                      </code>
                    </td>
                    <td>
                      <div style={{ fontWeight: 800, fontSize: '14px' }}>{p.name}</div>
                      {p.brand && (
                        <span style={{ fontSize: '11px', color: '#555', fontWeight: 600 }}>Brand: {p.brand}</span>
                      )}
                    </td>
                    <td>
                      <span className="neu-badge neu-badge-blue">
                        {p.category || 'General'}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 900, fontSize: '15px' }}>
                        ₹{Number(p.price || 0).toFixed(2)}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontSize: '13px', color: '#555', fontWeight: 600 }}>
                        ₹{Number(p.cost_price || 0).toFixed(2)}
                      </div>
                      <span style={{ fontSize: '10px', fontWeight: 800, color: '#059669' }}>
                        {margin}% margin
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <button 
                          onClick={() => adjustStock(p, -1)}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', display: 'flex' }}
                          title="Decrease 1"
                        >
                          <MinusCircle size={18} color="#E11D48" />
                        </button>
                        <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 900, fontSize: '15px', minWidth: '32px', textAlign: 'center' }}>
                          {p.stock_quantity || 0}
                        </span>
                        <button 
                          onClick={() => adjustStock(p, 1)}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', display: 'flex' }}
                          title="Increase 1"
                        >
                          <PlusCircle size={18} color="#10B981" />
                        </button>
                        <span style={{ fontSize: '11px', color: '#777', fontWeight: 600 }}>{p.unit || 'pcs'}</span>
                      </div>
                    </td>
                    <td>
                      {isLow ? (
                        <span className="neu-badge neu-badge-red">
                          LOW STOCK
                        </span>
                      ) : (
                        <span className="neu-badge neu-badge-green">
                          HEALTHY
                        </span>
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <button 
                          onClick={() => openEditModal(p)}
                          className="neu-btn neu-btn-sm"
                          title="Edit Product"
                        >
                          <Edit2 size={12} />
                        </button>
                        <button 
                          onClick={() => handleDeleteProduct(p.id, p.name)}
                          className="neu-btn neu-btn-sm neu-btn-red"
                          title="Delete Product"
                        >
                          <Trash2 size={12} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Modal 1: Add/Edit Product */}
      {isAddEditOpen && (
        <div className="modal-overlay" onClick={() => setIsAddEditOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontSize: '18px', fontWeight: 800 }}>
                {editingProduct ? 'Edit Inventory SKU' : 'Add New Inventory SKU'}
              </h3>
              <button 
                onClick={() => setIsAddEditOpen(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSaveProduct}>
              <div className="modal-body" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div style={{ gridColumn: '1 / -1' }}>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Product Name *
                  </label>
                  <input 
                    type="text"
                    required
                    placeholder="e.g. Maggi 2-Minute Masala Noodles 70g"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="neu-input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Barcode / EAN
                  </label>
                  <input 
                    type="text"
                    placeholder="8901058852393"
                    value={formData.barcode}
                    onChange={(e) => setFormData({ ...formData, barcode: e.target.value })}
                    className="neu-input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Brand
                  </label>
                  <input 
                    type="text"
                    placeholder="e.g. Nestlé, Amul, Britannia"
                    value={formData.brand}
                    onChange={(e) => setFormData({ ...formData, brand: e.target.value })}
                    className="neu-input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Category
                  </label>
                  <select 
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    className="neu-input"
                  >
                    <option value="Groceries">Groceries & Staples</option>
                    <option value="Snacks">Snacks & Biscuits</option>
                    <option value="Dairy">Dairy & Eggs</option>
                    <option value="Beverages">Beverages & Tea</option>
                    <option value="Personal Care">Personal Care & Hygiene</option>
                    <option value="Household">Household & Cleaning</option>
                    <option value="Instant Foods">Instant & Ready Foods</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Selling Price (₹ MRP) *
                  </label>
                  <input 
                    type="number"
                    step="0.01"
                    required
                    placeholder="14.00"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                    className="neu-input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Cost Price (₹)
                  </label>
                  <input 
                    type="number"
                    step="0.01"
                    placeholder="11.50"
                    value={formData.cost_price}
                    onChange={(e) => setFormData({ ...formData, cost_price: e.target.value })}
                    className="neu-input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Stock Quantity
                  </label>
                  <input 
                    type="number"
                    placeholder="50"
                    value={formData.stock_quantity}
                    onChange={(e) => setFormData({ ...formData, stock_quantity: e.target.value })}
                    className="neu-input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Unit
                  </label>
                  <input 
                    type="text"
                    placeholder="pcs / pack / kg"
                    value={formData.unit}
                    onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                    className="neu-input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                    Low Stock Threshold
                  </label>
                  <input 
                    type="number"
                    placeholder="5"
                    value={formData.reorder_level}
                    onChange={(e) => setFormData({ ...formData, reorder_level: e.target.value })}
                    className="neu-input"
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button 
                  type="button"
                  onClick={() => setIsAddEditOpen(false)}
                  className="neu-btn"
                >
                  Cancel
                </button>
                <button 
                  type="submit"
                  className="neu-btn neu-btn-primary"
                >
                  {editingProduct ? 'Save Changes' : 'Create Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 2: CSV Studio (Import / Export) */}
      {isCsvModalOpen && (
        <div className="modal-overlay" onClick={() => setIsCsvModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '750px' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <FileSpreadsheet size={20} color="#059669" />
                <h3 style={{ fontSize: '18px', fontWeight: 800 }}>CSV Bulk Import & Export Studio</h3>
              </div>
              <button onClick={() => setIsCsvModalOpen(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {/* Export section */}
              <div style={{ padding: '16px', border: 'var(--neu-border-sm)', borderRadius: '10px', background: '#F0FDF4' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                  <div>
                    <h4 style={{ fontWeight: 800, fontSize: '15px', color: '#166534' }}>Export Store Inventory</h4>
                    <p style={{ fontSize: '12px', color: '#15803D' }}>Download all {products.length} products as a formatted spreadsheet</p>
                  </div>
                  <button onClick={handleExportCsv} className="neu-btn neu-btn-green">
                    <Download size={14} /> Download .CSV
                  </button>
                </div>
              </div>

              {/* Import drag & drop */}
              <div style={{ padding: '20px', border: '2px dashed #0A0A0A', borderRadius: '10px', textAlign: 'center', background: '#FFFDF7' }}>
                <Upload size={32} style={{ margin: '0 auto 8px', color: '#1D4ED8' }} />
                <h4 style={{ fontWeight: 800, fontSize: '15px' }}>Upload Products CSV File</h4>
                <p style={{ fontSize: '12px', color: '#666', maxWidth: '400px', margin: '4px auto 14px' }}>
                  Columns supported: <code>barcode, name, brand, category, price, cost_price, stock_quantity</code>
                </p>
                
                <input 
                  type="file" 
                  accept=".csv"
                  id="csv-file-input"
                  style={{ display: 'none' }}
                  onChange={handleCsvFileChange}
                />
                <label htmlFor="csv-file-input" className="neu-btn neu-btn-primary" style={{ cursor: 'pointer' }}>
                  Choose CSV File
                </label>

                {csvFile && (
                  <div style={{ marginTop: '12px', fontSize: '13px', fontWeight: 700, color: '#059669' }}>
                    Selected: {csvFile.name} ({csvPreview.length} valid rows found)
                  </div>
                )}
              </div>

              {/* Preview parsed rows */}
              {csvPreview.length > 0 && (
                <div>
                  <h4 style={{ fontSize: '13px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '8px' }}>
                    Previewing First 5 Rows (of {csvPreview.length})
                  </h4>
                  <div className="neu-table-container" style={{ maxHeight: '180px' }}>
                    <table className="neu-table" style={{ fontSize: '12px' }}>
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Barcode</th>
                          <th>Price</th>
                          <th>Qty</th>
                        </tr>
                      </thead>
                      <tbody>
                        {csvPreview.slice(0, 5).map((r, i) => (
                          <tr key={i}>
                            <td style={{ fontWeight: 700 }}>{r.name}</td>
                            <td>{r.barcode}</td>
                            <td>₹{r.price}</td>
                            <td>{r.stock_quantity}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button onClick={() => setIsCsvModalOpen(false)} className="neu-btn">
                Close
              </button>
              {csvPreview.length > 0 && (
                <button 
                  onClick={handleBulkUploadCsv}
                  disabled={csvUploading}
                  className="neu-btn neu-btn-green"
                >
                  {csvUploading ? 'Importing SKUs...' : `Import All ${csvPreview.length} Products`}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Modal 3: 117K Master Indian Grocery Catalog Search */}
      {isCatalogModalOpen && (
        <div className="modal-overlay" onClick={() => setIsCatalogModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '800px' }}>
            <div className="modal-header" style={{ background: '#FEF3C7' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Database size={20} color="#D97706" />
                <div>
                  <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#92400E' }}>
                    National Master Grocery Catalog
                  </h3>
                  <p style={{ fontSize: '11px', color: '#B45309', fontWeight: 600 }}>
                    Instant 1-Click Import from 117,000+ Pre-Configured Indian SKUs
                  </p>
                </div>
              </div>
              <button onClick={() => setIsCatalogModalOpen(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ position: 'relative' }}>
                <Search size={18} style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: '#666' }} />
                <input 
                  type="text"
                  placeholder="Type product name (e.g. 'Tata Tea', 'Amul', 'Fortune', 'Aashirvaad', 'Colgate', 'Oreo')..."
                  value={catalogQuery}
                  onChange={(e) => handleSearchCatalog(e.target.value)}
                  className="neu-input"
                  style={{ paddingLeft: '42px', fontSize: '15px' }}
                  autoFocus
                />
              </div>

              <div style={{ minHeight: '260px', maxHeight: '360px', overflowY: 'auto' }}>
                {isSearchingCatalog ? (
                  <div style={{ textAlign: 'center', padding: '40px' }}>
                    <RefreshCw size={24} className="spin-anim" style={{ margin: '0 auto 8px', color: '#D97706' }} />
                    <p style={{ fontWeight: 700 }}>Searching 117K master items...</p>
                  </div>
                ) : catalogResults.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '48px', color: '#777' }}>
                    <Sparkles size={36} style={{ margin: '0 auto 10px', color: '#D97706' }} />
                    <h4 style={{ fontWeight: 800, fontSize: '15px' }}>Search the Master Indian Catalog</h4>
                    <p style={{ fontSize: '13px', marginTop: '4px' }}>
                      Type at least 2 letters above to search brands, barcodes, and groceries instantly.
                    </p>
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {catalogResults.map((item, idx) => (
                      <div 
                        key={idx}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '12px 16px',
                          border: '1.5px solid #0A0A0A',
                          borderRadius: '8px',
                          background: '#FFF',
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 800, fontSize: '14px' }}>{item.name}</div>
                          <div style={{ display: 'flex', gap: '10px', marginTop: '3px', fontSize: '11px', color: '#555' }}>
                            <span><strong>Brand:</strong> {item.brand || 'Generic'}</span>
                            <span><strong>Barcode:</strong> {item.barcode}</span>
                            <span><strong>Category:</strong> {item.category || 'Grocery'}</span>
                          </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                          <div style={{ textAlign: 'right' }}>
                            <div style={{ fontSize: '16px', fontWeight: 900, fontFamily: 'var(--font-heading)' }}>
                              ₹{Number(item.mrp || item.price || 50).toFixed(2)}
                            </div>
                            <span style={{ fontSize: '10px', color: '#666' }}>Standard MRP</span>
                          </div>

                          <button 
                            onClick={() => handleImportFromCatalog(item)}
                            className="neu-btn neu-btn-sm neu-btn-green"
                          >
                            <Plus size={14} /> Import to Store
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer">
              <button onClick={() => setIsCatalogModalOpen(false)} className="neu-btn">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

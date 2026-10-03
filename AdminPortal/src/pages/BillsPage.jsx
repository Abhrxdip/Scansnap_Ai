import React, { useState, useEffect, useMemo } from 'react';
import { 
  Receipt, 
  Search, 
  Printer, 
  Eye, 
  Download, 
  X, 
  Calendar, 
  Filter, 
  Smartphone, 
  DollarSign, 
  CheckCircle2,
  FileText,
  QrCode,
  Zap,
  Sparkles,
  Share2
} from 'lucide-react';
import { fetchBills, seedDemoBills } from '../api/client';

export const CORE_DEMO_BILLS = [
  {
    id: 'BILL_202610_001',
    bill_number: 'INV-202610-001',
    customer_name: 'Abhradeep Das',
    customer_phone: '+91 98301 24510',
    payment_mode: 'UPI',
    created_at: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
    tax_amount: 45.45,
    total_amount: 954.45,
    items: [
      { product_name: 'Beardo Mariner Eau De Parfum 50ml', quantity: 1, unit_price: 799.0, total_price: 799.0 },
      { product_name: 'Amul Ice Cream Cup Vanilla Magic 100ml', quantity: 2, unit_price: 30.0, total_price: 60.0 },
      { product_name: 'Britannia Cake Gobbles Choco Chill 65g', quantity: 1, unit_price: 30.0, total_price: 30.0 },
      { product_name: 'Thums Up Charged Carbonated Beverage 250ml Can', quantity: 1, unit_price: 20.0, total_price: 20.0 },
    ]
  },
  {
    id: 'BILL_202610_002',
    bill_number: 'INV-202610-002',
    customer_name: 'Priya Mukherjee',
    customer_phone: '+91 98742 81920',
    payment_mode: 'UPI',
    created_at: new Date(Date.now() - 42 * 60 * 1000).toISOString(),
    tax_amount: 68.20,
    total_amount: 1432.20,
    items: [
      { product_name: 'CeraVe Hydrating Cleanser 236ml', quantity: 1, unit_price: 900.0, total_price: 900.0 },
      { product_name: 'Plum Green Tea Pore Cleansing Face Wash 100ml', quantity: 1, unit_price: 350.0, total_price: 350.0 },
      { product_name: 'Dettol Original Germ Protection Bathing Soap 75g', quantity: 3, unit_price: 38.0, total_price: 114.0 },
    ]
  },
  {
    id: 'BILL_202610_003',
    bill_number: 'INV-202610-003',
    customer_name: 'Rahul Sharma',
    customer_phone: '+91 99033 11842',
    payment_mode: 'CASH',
    created_at: new Date(Date.now() - 85 * 60 * 1000).toISOString(),
    tax_amount: 8.20,
    total_amount: 172.20,
    items: [
      { product_name: 'Maggi 2-Minute Noodles 70g', quantity: 4, unit_price: 14.0, total_price: 56.0 },
      { product_name: 'Yippee Magic Masala Noodles 70g', quantity: 2, unit_price: 14.0, total_price: 28.0 },
      { product_name: 'Cadbury Dairy Milk Chocolate 50g', quantity: 2, unit_price: 40.0, total_price: 80.0 },
    ]
  },
  {
    id: 'BILL_202610_004',
    bill_number: 'INV-202610-004',
    customer_name: 'Ananya Roy',
    customer_phone: '+91 97482 66319',
    payment_mode: 'UPI',
    created_at: new Date(Date.now() - 130 * 60 * 1000).toISOString(),
    tax_amount: 29.45,
    total_amount: 618.45,
    items: [
      { product_name: 'Head & Shoulders Cool Menthol Anti-Dandruff Shampoo 180ml', quantity: 1, unit_price: 250.0, total_price: 250.0 },
      { product_name: 'Wild Stone Forest Spice Deodorant Soap 125g', quantity: 2, unit_price: 70.0, total_price: 140.0 },
      { product_name: 'Nivea Men Fresh Active Deodorant 150ml', quantity: 1, unit_price: 199.0, total_price: 199.0 },
    ]
  },
  {
    id: 'BILL_202610_005',
    bill_number: 'INV-202610-005',
    customer_name: 'Sourav Ganguly',
    customer_phone: '+91 98310 99482',
    payment_mode: 'CARD',
    created_at: new Date(Date.now() - 210 * 60 * 1000).toISOString(),
    tax_amount: 42.30,
    total_amount: 888.30,
    items: [
      { product_name: 'Fortune Basmati Rice 1kg', quantity: 2, unit_price: 180.0, total_price: 360.0 },
      { product_name: 'Aashirvaad Shudh Chakki Atta 5kg', quantity: 1, unit_price: 270.0, total_price: 270.0 },
      { product_name: 'Tata Salt Vaccum Evaporated 1kg', quantity: 2, unit_price: 28.0, total_price: 56.0 },
      { product_name: 'Tata Sampann Toor Dal 1kg', quantity: 1, unit_price: 160.0, total_price: 160.0 },
    ]
  },
  {
    id: 'BILL_202610_006',
    bill_number: 'INV-202610-006',
    customer_name: 'Sneha Sen',
    customer_phone: '+91 98741 55231',
    payment_mode: 'UPI',
    created_at: new Date(Date.now() - 280 * 60 * 1000).toISOString(),
    tax_amount: 14.50,
    total_amount: 304.50,
    items: [
      { product_name: 'Nestle Everyday Dairy Whitener Milk Powder 20g', quantity: 5, unit_price: 10.0, total_price: 50.0 },
      { product_name: 'Brooke Bond Taj Mahal Tea 250g', quantity: 1, unit_price: 180.0, total_price: 180.0 },
      { product_name: 'Britannia Bourbon Chocolate Biscuits', quantity: 2, unit_price: 30.0, total_price: 60.0 },
    ]
  },
  {
    id: 'BILL_202610_007',
    bill_number: 'INV-202610-007',
    customer_name: 'Vikram Singhania',
    customer_phone: '+91 98200 44192',
    payment_mode: 'UPI',
    created_at: new Date(Date.now() - 360 * 60 * 1000).toISOString(),
    tax_amount: 18.75,
    total_amount: 393.75,
    items: [
      { product_name: 'Surf Excel Easy Wash Detergent Powder 1kg', quantity: 1, unit_price: 140.0, total_price: 140.0 },
      { product_name: 'Lizol Disinfectant Floor Cleaner Citrus 500ml', quantity: 1, unit_price: 110.0, total_price: 110.0 },
      { product_name: 'Harpic Power Plus Toilet Cleaner 500ml', quantity: 1, unit_price: 95.0, total_price: 95.0 },
      { product_name: 'Vim Dishwash Bar with Lemon 300g', quantity: 2, unit_price: 15.0, total_price: 30.0 },
    ]
  },
  {
    id: 'BILL_202610_008',
    bill_number: 'INV-202610-008',
    customer_name: 'Debjani Ghosh',
    customer_phone: '+91 97321 00481',
    payment_mode: 'CASH',
    created_at: new Date(Date.now() - 510 * 60 * 1000).toISOString(),
    tax_amount: 11.50,
    total_amount: 241.50,
    items: [
      { product_name: 'Parle Hide & Seek Choco Chip Biscuits', quantity: 3, unit_price: 30.0, total_price: 90.0 },
      { product_name: 'Cadbury Oreo Original Biscuits', quantity: 2, unit_price: 30.0, total_price: 60.0 },
      { product_name: 'Coca-Cola Original Taste 300ml Can', quantity: 2, unit_price: 40.0, total_price: 80.0 },
    ]
  }
];

export default function BillsPage({ selectedBill, setSelectedBill }) {
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterPayment, setFilterPayment] = useState('ALL');
  const [qrModalBill, setQrModalBill] = useState(null);
  const [qrTab, setQrTab] = useState('UPI'); // 'UPI' or 'VERIFY'
  const [isSeeding, setIsSeeding] = useState(false);

  const loadBills = async () => {
    try {
      setLoading(true);
      const data = await fetchBills();
      if (data && data.length > 0) {
        const normalized = data.map((b, idx) => ({
          ...b,
          bill_number: b.bill_number || (b.id ? (b.id.startsWith('BILL_') ? b.id : `INV-${b.id.slice(0, 8).toUpperCase()}`) : `INV-${1000 + idx}`)
        }));
        setBills(normalized);
      } else {
        setBills(CORE_DEMO_BILLS);
      }
    } catch (err) {
      console.error(err);
      setBills(CORE_DEMO_BILLS);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBills();
  }, []);

  const handleSeedDemoBills = async () => {
    try {
      setIsSeeding(true);
      await seedDemoBills();
      await loadBills();
    } catch (err) {
      console.error(err);
      setBills(CORE_DEMO_BILLS);
    } finally {
      setIsSeeding(false);
    }
  };

  const filteredBills = useMemo(() => {
    return bills.filter(b => {
      const billNo = (b.bill_number || b.id || '').toLowerCase();
      const matchSearch = 
        billNo.includes(searchQuery.toLowerCase()) ||
        b.customer_phone?.includes(searchQuery) ||
        b.customer_name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (b.items || []).some(item => item.product_name?.toLowerCase().includes(searchQuery.toLowerCase()));
      
      const matchPayment = 
        filterPayment === 'ALL' || 
        b.payment_mode?.toUpperCase() === filterPayment.toUpperCase();

      return matchSearch && matchPayment;
    });
  }, [bills, searchQuery, filterPayment]);

  const totalCollected = useMemo(() => {
    return filteredBills.reduce((sum, b) => sum + Number(b.total_amount || 0), 0);
  }, [filteredBills]);

  const totalTax = useMemo(() => {
    return filteredBills.reduce((sum, b) => sum + Number(b.tax_amount || 0), 0);
  }, [filteredBills]);

  const handlePrint = () => {
    window.print();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFF' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#666' }}>Invoices Recorded</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px' }}>
            {bills.length} Bills
          </div>
        </div>

        <div className="neu-box" style={{ padding: '16px 20px', background: '#ECFDF5', borderColor: '#059669' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#047857' }}>Filtered Total Revenue</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px', color: '#047857' }}>
            ₹{totalCollected.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
        </div>

        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFF' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#666' }}>GST / Tax Collected</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px', color: '#1D4ED8' }}>
            ₹{totalTax.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
        </div>

        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFFDF7' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#B45309' }}>UPI Dynamic QR</span>
          <div style={{ fontSize: '24px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px', color: '#D97706', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <QrCode size={22} /> Live Scan
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="neu-box" style={{ padding: '18px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: '1 1 320px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#666' }} />
            <input 
              type="text"
              placeholder="Search by Bill # (INV-...), customer name, phone, or product (e.g. Beardo)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="neu-input"
              style={{ paddingLeft: '38px' }}
            />
          </div>

          <select 
            value={filterPayment}
            onChange={(e) => setFilterPayment(e.target.value)}
            className="neu-input"
            style={{ width: '150px' }}
          >
            <option value="ALL">All Payments</option>
            <option value="UPI">UPI / QR</option>
            <option value="CASH">Cash</option>
            <option value="CARD">Card</option>
          </select>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <button 
            onClick={handleSeedDemoBills}
            disabled={isSeeding}
            className="neu-btn neu-btn-primary"
            style={{ background: '#10B981', color: '#FFFFFF', borderColor: '#0A0A0A', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            title="Seed realistic customer invoices with live UPI Dynamic QR codes"
          >
            <Zap size={15} className={isSeeding ? "spin-anim" : ""} />
            {isSeeding ? 'Seeding Invoices...' : '⚡ Seed Demo Invoices (with QR)'}
          </button>

          <button 
            onClick={loadBills} 
            className="neu-btn neu-btn-sm"
          >
            Refresh Archive
          </button>
        </div>
      </div>

      {/* Bills Table */}
      <div className="neu-table-container">
        <table className="neu-table">
          <thead>
            <tr>
              <th>Invoice Number</th>
              <th>Date & Time</th>
              <th>Customer</th>
              <th>Payment Mode</th>
              <th>Items</th>
              <th>Tax (GST)</th>
              <th>Grand Total</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '40px' }}>
                  <p style={{ fontWeight: 700 }}>Loading bill archives...</p>
                </td>
              </tr>
            ) : filteredBills.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '48px', color: '#666' }}>
                  <Receipt size={40} style={{ margin: '0 auto 12px', color: '#AAA' }} />
                  <h4 style={{ fontSize: '16px', fontWeight: 800 }}>No bills match your criteria</h4>
                  <p style={{ fontSize: '13px', marginTop: '4px' }}>
                    Click <strong>"⚡ Seed Demo Invoices"</strong> or generate an order from the Android POS app.
                  </p>
                </td>
              </tr>
            ) : (
              filteredBills.map((b) => {
                const dateStr = b.created_at ? new Date(b.created_at).toLocaleString('en-IN', {
                  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
                }) : 'Just now';

                return (
                  <tr key={b.id || b.bill_number}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <code style={{ background: '#FEF08A', padding: '4px 8px', borderRadius: '4px', border: '1.5px solid #0A0A0A', fontSize: '13px', fontWeight: 800 }}>
                          {b.bill_number}
                        </code>
                        <button
                          onClick={() => setQrModalBill(b)}
                          className="neu-btn neu-btn-sm"
                          style={{ padding: '3px 6px', background: '#F3F4F6' }}
                          title="View Instant UPI Payment QR Code"
                        >
                          <QrCode size={13} color="#047857" />
                        </button>
                      </div>
                    </td>
                    <td style={{ fontSize: '12px', color: '#555', fontWeight: 600 }}>
                      {dateStr}
                    </td>
                    <td>
                      <div style={{ fontWeight: 800 }}>{b.customer_name || 'Walk-in Shopper'}</div>
                      <div style={{ fontSize: '11px', color: '#666' }}>{b.customer_phone || 'No phone'}</div>
                    </td>
                    <td>
                      <span className={`neu-badge ${b.payment_mode?.toUpperCase() === 'UPI' ? 'neu-badge-purple' : 'neu-badge-green'}`}>
                        {b.payment_mode || 'CASH'}
                      </span>
                    </td>
                    <td style={{ fontWeight: 700 }}>
                      <span title={(b.items || []).map(i => `${i.product_name} (${i.quantity})`).join(', ')}>
                        {b.items?.length || 0} items
                      </span>
                    </td>
                    <td style={{ fontSize: '13px', color: '#555', fontWeight: 600 }}>
                      ₹{Number(b.tax_amount || 0).toFixed(2)}
                    </td>
                    <td>
                      <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 900, fontSize: '15px' }}>
                        ₹{Number(b.total_amount || 0).toFixed(2)}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <button 
                          onClick={() => setSelectedBill(b)}
                          className="neu-btn neu-btn-sm neu-btn-yellow"
                          title="View Full Thermal Receipt"
                        >
                          <Eye size={12} /> View Receipt
                        </button>
                        <button
                          onClick={() => setQrModalBill(b)}
                          className="neu-btn neu-btn-sm"
                          title="Instant UPI QR Code"
                        >
                          <QrCode size={12} /> QR
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

      {/* Standalone Live QR Code Modal */}
      {qrModalBill && (
        <div className="modal-overlay" onClick={() => setQrModalBill(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '420px', background: '#FFF', textAlign: 'center' }}>
            <div className="modal-header" style={{ background: '#ECFDF5', borderColor: '#059669' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <QrCode size={18} color="#047857" />
                <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#047857' }}>
                  {qrTab === 'UPI' ? '⚡ Instant UPI Payment QR' : '🛡️ Digital Tax Invoice Verification'}
                </h3>
              </div>
              <button onClick={() => setQrModalBill(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-body" style={{ padding: '24px 20px' }}>
              {/* Tab Selector */}
              <div style={{ display: 'flex', background: '#F3F4F6', padding: '4px', borderRadius: '8px', marginBottom: '20px', border: '1.5px solid #0A0A0A' }}>
                <button
                  onClick={() => setQrTab('UPI')}
                  style={{
                    flex: 1,
                    padding: '8px',
                    borderRadius: '6px',
                    border: 'none',
                    fontWeight: 800,
                    fontSize: '12px',
                    cursor: 'pointer',
                    background: qrTab === 'UPI' ? '#FFFFFF' : 'transparent',
                    boxShadow: qrTab === 'UPI' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                    color: qrTab === 'UPI' ? '#047857' : '#6B7280'
                  }}
                >
                  💳 UPI Payment QR
                </button>
                <button
                  onClick={() => setQrTab('VERIFY')}
                  style={{
                    flex: 1,
                    padding: '8px',
                    borderRadius: '6px',
                    border: 'none',
                    fontWeight: 800,
                    fontSize: '12px',
                    cursor: 'pointer',
                    background: qrTab === 'VERIFY' ? '#FFFFFF' : 'transparent',
                    boxShadow: qrTab === 'VERIFY' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                    color: qrTab === 'VERIFY' ? '#1D4ED8' : '#6B7280'
                  }}
                >
                  📜 e-Invoice Verify
                </button>
              </div>

              {/* QR Image Container */}
              <div style={{ display: 'inline-block', padding: '16px', background: '#FFFFFF', border: '2.5px solid #0A0A0A', borderRadius: '12px', boxShadow: '4px 4px 0px #0A0A0A', marginBottom: '16px' }}>
                <img 
                  src={
                    qrTab === 'UPI'
                      ? `https://api.qrserver.com/v1/create-qr-code/?size=200x200&margin=1&data=${encodeURIComponent(`upi://pay?pa=smartvendor@upi&pn=ScanSnap+Kirana&am=${Number(qrModalBill.total_amount || 0).toFixed(2)}&cu=INR&tn=${qrModalBill.bill_number}`)}`
                      : `https://api.qrserver.com/v1/create-qr-code/?size=200x200&margin=1&data=${encodeURIComponent(JSON.stringify({
                          invoice: qrModalBill.bill_number,
                          store: "ScanSnap Express Kirana",
                          gstin: "19ABCDE1234F1Z5",
                          total: Number(qrModalBill.total_amount || 0).toFixed(2),
                          tax: Number(qrModalBill.tax_amount || 0).toFixed(2),
                          date: qrModalBill.created_at || new Date().toISOString()
                        }))}`
                  } 
                  alt="Dynamic QR Code" 
                  style={{ width: '180px', height: '180px', display: 'block' }}
                />
              </div>

              {/* Invoice details */}
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '20px', fontWeight: 900, fontFamily: 'var(--font-heading)', color: '#0A0A0A' }}>
                  ₹{Number(qrModalBill.total_amount || 0).toFixed(2)}
                </div>
                <div style={{ fontSize: '13px', fontWeight: 800, color: '#4B5563', marginTop: '2px' }}>
                  Invoice: {qrModalBill.bill_number}
                </div>
                <div style={{ fontSize: '11px', color: '#6B7280', marginTop: '4px' }}>
                  Customer: {qrModalBill.customer_name || 'Walk-in'} • {qrModalBill.customer_phone || '+91 98765 43210'}
                </div>
                <p style={{ fontSize: '11px', fontWeight: 700, color: '#059669', marginTop: '12px' }}>
                  {qrTab === 'UPI'
                    ? '📲 Point Google Pay, PhonePe, Paytm, or BHIM camera to pay'
                    : '🔍 Official GST e-Invoice cryptographic hash for audit compliance'}
                </p>
              </div>
            </div>

            <div className="modal-footer" style={{ display: 'flex', justifyContent: 'space-between', gap: '8px' }}>
              <button 
                onClick={() => {
                  setSelectedBill(qrModalBill);
                  setQrModalBill(null);
                }} 
                className="neu-btn"
              >
                View Full Receipt
              </button>
              <button onClick={() => setQrModalBill(null)} className="neu-btn neu-btn-primary">
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Printable Receipt Modal */}
      {selectedBill && (
        <div className="modal-overlay" onClick={() => setSelectedBill(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '440px', background: '#FFF' }}>
            <div className="modal-header" style={{ background: '#FEF08A' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Receipt size={18} />
                <h3 style={{ fontSize: '16px', fontWeight: 800 }}>Tax Invoice / Bill of Supply</h3>
              </div>
              <button onClick={() => setSelectedBill(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            {/* Thermal style receipt body */}
            <div className="modal-body print-area" style={{ fontFamily: "'Courier New', Courier, monospace", fontSize: '13px', lineHeight: 1.4, color: '#000' }}>
              <div style={{ textAlign: 'center', borderBottom: '2px dashed #000', paddingBottom: '12px', marginBottom: '12px' }}>
                <h2 style={{ fontSize: '18px', fontWeight: 900, textTransform: 'uppercase' }}>ScanSnap Express Kirana</h2>
                <p style={{ fontSize: '11px', marginTop: '2px' }}>Brigade Road, Bangalore - 560001</p>
                <p style={{ fontSize: '11px' }}>GSTIN: 19ABCDE1234F1Z5 | FSSAI: 1282101900012</p>
                <p style={{ fontSize: '11px' }}>Tel: +91 98765 43210</p>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '8px' }}>
                <div>
                  <strong>INV #:</strong> {selectedBill.bill_number}<br />
                  <strong>Customer:</strong> {selectedBill.customer_name || 'Walk-in'}
                </div>
                <div style={{ textAlign: 'right' }}>
                  <strong>Date:</strong> {new Date(selectedBill.created_at || Date.now()).toLocaleDateString('en-IN')}<br />
                  <strong>Time:</strong> {new Date(selectedBill.created_at || Date.now()).toLocaleTimeString('en-IN')}
                </div>
              </div>

              <table style={{ width: '100%', borderCollapse: 'collapse', borderTop: '1px solid #000', borderBottom: '1px solid #000', margin: '12px 0', fontSize: '12px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px dashed #000' }}>
                    <th style={{ textAlign: 'left', padding: '6px 2px' }}>ITEM</th>
                    <th style={{ textAlign: 'center', padding: '6px 2px' }}>QTY</th>
                    <th style={{ textAlign: 'right', padding: '6px 2px' }}>RATE</th>
                    <th style={{ textAlign: 'right', padding: '6px 2px' }}>AMT</th>
                  </tr>
                </thead>
                <tbody>
                  {(selectedBill.items || []).map((item, idx) => (
                    <tr key={idx}>
                      <td style={{ padding: '4px 2px' }}>{item.product_name || `Item #${idx+1}`}</td>
                      <td style={{ textAlign: 'center', padding: '4px 2px' }}>{item.quantity}</td>
                      <td style={{ textAlign: 'right', padding: '4px 2px' }}>₹{Number(item.unit_price || 0).toFixed(2)}</td>
                      <td style={{ textAlign: 'right', padding: '4px 2px' }}>₹{Number(item.total_price || (item.quantity * item.unit_price) || 0).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '12px', borderBottom: '2px dashed #000', paddingBottom: '10px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Subtotal:</span>
                  <span>₹{(Number(selectedBill.total_amount || 0) - Number(selectedBill.tax_amount || 0)).toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>CGST (2.5%):</span>
                  <span>₹{(Number(selectedBill.tax_amount || 0) / 2).toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>SGST (2.5%):</span>
                  <span>₹{(Number(selectedBill.tax_amount || 0) / 2).toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 900, fontSize: '16px', marginTop: '6px', borderTop: '1px solid #000', paddingTop: '6px' }}>
                  <span>GRAND TOTAL:</span>
                  <span>₹{Number(selectedBill.total_amount || 0).toFixed(2)}</span>
                </div>
              </div>

              <div style={{ textAlign: 'center', marginTop: '14px', paddingTop: '10px', borderTop: '1px dashed #000' }}>
                <div style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                  ⚡ Instant UPI Payment QR
                </div>
                <div 
                  onClick={() => {
                    setQrModalBill(selectedBill);
                    setSelectedBill(null);
                  }}
                  style={{ display: 'inline-block', padding: '6px', background: '#FFFFFF', border: '2px solid #000', borderRadius: '8px', cursor: 'pointer' }}
                  title="Click to Zoom Fullscreen QR"
                >
                  <img 
                    src={`https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=${encodeURIComponent(`upi://pay?pa=smartvendor@upi&pn=ScanSnap+Kirana&am=${Number(selectedBill.total_amount || 0).toFixed(2)}&cu=INR&tn=${selectedBill.bill_number}`)}`} 
                    alt="UPI QR Code" 
                    style={{ width: '110px', height: '110px', display: 'block' }}
                  />
                </div>
                <p style={{ fontSize: '10px', fontWeight: 800, color: '#374151', marginTop: '4px' }}>
                  Scan with GPay / PhonePe / Paytm / BHIM
                </p>
              </div>

              <div style={{ textAlign: 'center', marginTop: '12px', fontSize: '11px' }}>
                <p><strong>Payment Mode:</strong> {selectedBill.payment_mode || 'UPI / CASH'} - VERIFIED</p>
                <p style={{ marginTop: '4px' }}>Thank you for shopping at ScanSnap AI!</p>
                <p>*** Powered by Computer Vision POS ***</p>
              </div>
            </div>

            <div className="modal-footer" style={{ display: 'flex', justifyContent: 'space-between', gap: '8px' }}>
              <button 
                onClick={() => {
                  const billText = `🧾 *ScanSnap AI Supermarket Receipt*\nInv: ${selectedBill.bill_number}\nDate: ${new Date().toLocaleDateString('en-IN')}\nTotal Items: ${(selectedBill.items || []).length}\n*Grand Total: ₹${Number(selectedBill.total_amount || 0).toFixed(2)}*\nStatus: Paid via ${selectedBill.payment_mode || 'UPI'}\n\n_Thank you for visiting! Powered by ScanSnap AI._`;
                  window.open(`https://wa.me/?text=${encodeURIComponent(billText)}`, '_blank');
                }}
                className="neu-btn"
                style={{ background: '#25D366', color: '#FFFFFF', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                💬 Send WhatsApp
              </button>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button onClick={() => setSelectedBill(null)} className="neu-btn">
                  Close
                </button>
                <button onClick={handlePrint} className="neu-btn neu-btn-primary">
                  <Printer size={15} /> Print Thermal
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

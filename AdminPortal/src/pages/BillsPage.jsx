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
  FileText
} from 'lucide-react';
import { fetchBills } from '../api/client';

export default function BillsPage({ selectedBill, setSelectedBill }) {
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterPayment, setFilterPayment] = useState('ALL');

  const loadBills = async () => {
    try {
      setLoading(true);
      const data = await fetchBills();
      setBills(data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBills();
  }, []);

  const filteredBills = useMemo(() => {
    return bills.filter(b => {
      const matchSearch = 
        b.bill_number?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        b.customer_phone?.includes(searchQuery) ||
        b.customer_name?.toLowerCase().includes(searchQuery.toLowerCase());
      
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
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFF' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#666' }}>Invoices Recorded</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px' }}>
            {bills.length} Bills
          </div>
        </div>

        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFF' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#666' }}>Filtered Total Revenue</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px', color: '#10B981' }}>
            ₹{totalCollected.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
        </div>

        <div className="neu-box" style={{ padding: '16px 20px', background: '#FFF' }}>
          <span style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#666' }}>GST / Tax Collected</span>
          <div style={{ fontSize: '26px', fontWeight: 900, fontFamily: 'var(--font-heading)', marginTop: '4px', color: '#1D4ED8' }}>
            ₹{totalTax.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
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
              placeholder="Search by Bill # (BILL-...), customer phone, or name..."
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

        <button 
          onClick={loadBills} 
          className="neu-btn neu-btn-sm"
        >
          Refresh Archive
        </button>
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
                    Generate a checkout order from the Android POS app or adjust your search filter.
                  </p>
                </td>
              </tr>
            ) : (
              filteredBills.map((b) => {
                const dateStr = b.created_at ? new Date(b.created_at).toLocaleString('en-IN', {
                  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
                }) : 'Just now';

                return (
                  <tr key={b.id}>
                    <td>
                      <code style={{ background: '#FEF08A', padding: '4px 8px', borderRadius: '4px', border: '1.5px solid #0A0A0A', fontSize: '13px', fontWeight: 800 }}>
                        {b.bill_number}
                      </code>
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
                      {b.items?.length || 0} items
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
                      <button 
                        onClick={() => setSelectedBill(b)}
                        className="neu-btn neu-btn-sm neu-btn-yellow"
                      >
                        <Eye size={12} /> View Receipt
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

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
                <h2 style={{ fontSize: '18px', fontWeight: 900, textTransform: 'uppercase' }}>ScanSnap AI Supermarket</h2>
                <p style={{ fontSize: '11px', marginTop: '2px' }}>Main Street, Sector 4, Tech City</p>
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
                  <span>₹{Number(selectedBill.subtotal || selectedBill.total_amount).toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>CGST (9%):</span>
                  <span>₹{(Number(selectedBill.tax_amount || 0) / 2).toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>SGST (9%):</span>
                  <span>₹{(Number(selectedBill.tax_amount || 0) / 2).toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 900, fontSize: '16px', marginTop: '6px', borderTop: '1px solid #000', paddingTop: '6px' }}>
                  <span>GRAND TOTAL:</span>
                  <span>₹{Number(selectedBill.total_amount || 0).toFixed(2)}</span>
                </div>
              </div>

              <div style={{ textAlign: 'center', marginTop: '12px', fontSize: '11px' }}>
                <p><strong>Payment Mode:</strong> {selectedBill.payment_mode || 'CASH'} - PAID</p>
                <p style={{ marginTop: '4px' }}>Thank you for shopping at ScanSnap AI!</p>
                <p>*** Powered by Computer Vision POS ***</p>
              </div>
            </div>

            <div className="modal-footer">
              <button onClick={() => setSelectedBill(null)} className="neu-btn">
                Close
              </button>
              <button onClick={handlePrint} className="neu-btn neu-btn-primary">
                <Printer size={15} /> Print Invoice
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

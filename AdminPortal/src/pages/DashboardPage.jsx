import React, { useState, useEffect } from 'react';
import { 
  DollarSign, 
  Receipt, 
  AlertTriangle, 
  TrendingUp, 
  Package, 
  CreditCard, 
  Smartphone, 
  ArrowUpRight,
  Eye,
  Sparkles,
  ShoppingBag
} from 'lucide-react';
import { fetchAnalytics } from '../api/client';

export default function DashboardPage({ setActiveTab, onSelectBill }) {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchAnalytics();
      setAnalytics(data);
    } catch (err) {
      console.error(err);
      setError('Could not connect to backend analytics. Ensure backend is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 15000); // 15s live polling
    return () => clearInterval(interval);
  }, []);

  const totalRev = analytics?.total_revenue ?? 0;
  const totalBills = analytics?.total_bills ?? 0;
  const avgBill = analytics?.average_bill_value ?? 0;
  const lowStock = analytics?.low_stock_count ?? 0;
  const topProducts = analytics?.top_products || [];
  const recentBills = analytics?.recent_bills || [];
  const paymentBreakdown = analytics?.payment_breakdown || { cash: 0, upi: 0, card: 0 };

  const totalPaymentSum = (paymentBreakdown.cash || 0) + (paymentBreakdown.upi || 0) + (paymentBreakdown.card || 0);
  const upiPercent = totalPaymentSum > 0 ? Math.round(((paymentBreakdown.upi || 0) / totalPaymentSum) * 100) : 60;
  const cashPercent = totalPaymentSum > 0 ? Math.round(((paymentBreakdown.cash || 0) / totalPaymentSum) * 100) : 35;
  const cardPercent = totalPaymentSum > 0 ? Math.max(0, 100 - upiPercent - cashPercent) : 5;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
      {/* Welcome Banner */}
      <div 
        className="neu-box"
        style={{
          background: 'linear-gradient(135deg, #FEF08A 0%, #FBBF24 100%)',
          padding: '24px 28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '16px'
        }}
      >
        <div>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', background: '#0A0A0A', color: '#FFF', padding: '4px 10px', borderRadius: '6px', fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '8px' }}>
            <Sparkles size={12} color="#FBBF24" /> AI Store Command v2.4
          </div>
          <h2 style={{ fontSize: '26px', fontWeight: 900, color: '#0A0A0A', lineHeight: 1.2 }}>
            Welcome back, Store Admin!
          </h2>
          <p style={{ fontSize: '14px', color: '#333', fontWeight: 500, marginTop: '4px' }}>
            ScanSnap vision engine is monitoring POS terminals in real time. 117K Indian grocery items indexed.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button 
            onClick={() => setActiveTab('inventory')}
            className="neu-btn neu-btn-primary"
          >
            <Package size={16} /> Manage Inventory
          </button>
          <button 
            onClick={() => setActiveTab('ai-studio')}
            className="neu-btn"
            style={{ background: '#FFF' }}
          >
            <Sparkles size={16} color="#8B5CF6" /> Test Vision AI
          </button>
        </div>
      </div>

      {error && (
        <div style={{ padding: '14px 20px', background: '#FFE4E6', border: 'var(--neu-border-sm)', borderRadius: '10px', color: '#9F1239', fontWeight: 700, fontSize: '13px' }}>
          ⚠️ {error}
        </div>
      )}

      {/* KPI Cards Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px' }}>
        {/* Card 1: Revenue */}
        <div className="neu-box neu-box-interactive" style={{ padding: '20px', background: '#FFFDF7' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', color: '#666', letterSpacing: '0.04em' }}>
              Total Revenue
            </span>
            <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: '#D1FAE5', border: '2px solid #0A0A0A', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <TrendingUp size={18} color="#065F46" />
            </div>
          </div>
          <div style={{ fontSize: '30px', fontWeight: 900, fontFamily: 'var(--font-heading)', color: '#0A0A0A' }}>
            ₹{totalRev.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div style={{ marginTop: '8px', fontSize: '12px', fontWeight: 600, color: '#059669', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span style={{ background: '#D1FAE5', padding: '2px 6px', borderRadius: '4px', border: '1px solid #059669' }}>+12.4%</span>
            <span>vs previous period</span>
          </div>
        </div>

        {/* Card 2: Total Bills */}
        <div className="neu-box neu-box-interactive" style={{ padding: '20px', background: '#FFFDF7' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', color: '#666', letterSpacing: '0.04em' }}>
              Total Orders
            </span>
            <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: '#EFF6FF', border: '2px solid #0A0A0A', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Receipt size={18} color="#1D4ED8" />
            </div>
          </div>
          <div style={{ fontSize: '30px', fontWeight: 900, fontFamily: 'var(--font-heading)', color: '#0A0A0A' }}>
            {totalBills}
          </div>
          <div style={{ marginTop: '8px', fontSize: '12px', fontWeight: 600, color: '#1D4ED8', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span>Completed POS checkouts</span>
          </div>
        </div>

        {/* Card 3: Avg Order Value */}
        <div className="neu-box neu-box-interactive" style={{ padding: '20px', background: '#FFFDF7' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', color: '#666', letterSpacing: '0.04em' }}>
              Average Ticket
            </span>
            <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: '#FEF3C7', border: '2px solid #0A0A0A', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <ShoppingBag size={18} color="#D97706" />
            </div>
          </div>
          <div style={{ fontSize: '30px', fontWeight: 900, fontFamily: 'var(--font-heading)', color: '#0A0A0A' }}>
            ₹{avgBill.toFixed(2)}
          </div>
          <div style={{ marginTop: '8px', fontSize: '12px', fontWeight: 600, color: '#78350F' }}>
            Avg value per customer
          </div>
        </div>

        {/* Card 4: Low Stock Alert */}
        <div 
          className="neu-box neu-box-interactive" 
          onClick={() => setActiveTab('inventory')}
          style={{ padding: '20px', background: lowStock > 0 ? '#FFE4E6' : '#FFFDF7', cursor: 'pointer' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', color: lowStock > 0 ? '#9F1239' : '#666', letterSpacing: '0.04em' }}>
              Low Stock Alerts
            </span>
            <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: lowStock > 0 ? '#F43F5E' : '#E5E7EB', border: '2px solid #0A0A0A', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <AlertTriangle size={18} color={lowStock > 0 ? '#FFF' : '#374151'} />
            </div>
          </div>
          <div style={{ fontSize: '30px', fontWeight: 900, fontFamily: 'var(--font-heading)', color: lowStock > 0 ? '#9F1239' : '#0A0A0A' }}>
            {lowStock} SKUs
          </div>
          <div style={{ marginTop: '8px', fontSize: '12px', fontWeight: 700, color: lowStock > 0 ? '#E11D48' : '#059669', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span>{lowStock > 0 ? 'Action required: Restock now' : 'All stock levels healthy'}</span>
            <ArrowUpRight size={14} />
          </div>
        </div>
      </div>

      {/* Analytics Second Row: Payment Breakdown & Top Selling */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '24px' }}>
        {/* Payment Methods Split */}
        <div className="neu-box" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
            <div>
              <h3 style={{ fontSize: '18px', fontWeight: 800 }}>Payment Method Split</h3>
              <p style={{ fontSize: '12px', color: '#666' }}>Distribution across live checkout registers</p>
            </div>
            <span className="neu-badge neu-badge-purple">Real-time</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {/* Multi-segmented Neo progress bar */}
            <div style={{ height: '24px', border: 'var(--neu-border-sm)', borderRadius: '6px', overflow: 'hidden', display: 'flex', background: '#EEE' }}>
              <div style={{ width: `${upiPercent}%`, background: '#8B5CF6', title: `UPI ${upiPercent}%` }} />
              <div style={{ width: `${cashPercent}%`, background: '#10B981', title: `Cash ${cashPercent}%` }} />
              <div style={{ width: `${cardPercent}%`, background: '#1D4ED8', title: `Card ${cardPercent}%` }} />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px', marginTop: '6px' }}>
              <div style={{ padding: '12px', background: '#F3E8FF', border: '2px solid #0A0A0A', borderRadius: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#6B21A8' }}>
                  <Smartphone size={14} /> UPI / QR
                </div>
                <div style={{ fontSize: '20px', fontWeight: 900, marginTop: '4px' }}>{upiPercent}%</div>
                <div style={{ fontSize: '11px', color: '#7E22CE', fontWeight: 600 }}>
                  ₹{(paymentBreakdown.upi || 0).toFixed(2)}
                </div>
              </div>

              <div style={{ padding: '12px', background: '#D1FAE5', border: '2px solid #0A0A0A', borderRadius: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#065F46' }}>
                  <DollarSign size={14} /> Cash
                </div>
                <div style={{ fontSize: '20px', fontWeight: 900, marginTop: '4px' }}>{cashPercent}%</div>
                <div style={{ fontSize: '11px', color: '#047857', fontWeight: 600 }}>
                  ₹{(paymentBreakdown.cash || 0).toFixed(2)}
                </div>
              </div>

              <div style={{ padding: '12px', background: '#DBEAFE', border: '2px solid #0A0A0A', borderRadius: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#1E40AF' }}>
                  <CreditCard size={14} /> Card
                </div>
                <div style={{ fontSize: '20px', fontWeight: 900, marginTop: '4px' }}>{cardPercent}%</div>
                <div style={{ fontSize: '11px', color: '#1D4ED8', fontWeight: 600 }}>
                  ₹{(paymentBreakdown.card || 0).toFixed(2)}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Top Selling Products */}
        <div className="neu-box" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
            <div>
              <h3 style={{ fontSize: '18px', fontWeight: 800 }}>Top Velocity SKUs</h3>
              <p style={{ fontSize: '12px', color: '#666' }}>Highest volume items scanned at counters</p>
            </div>
            <button 
              onClick={() => setActiveTab('inventory')}
              className="neu-btn neu-btn-sm"
            >
              Full Inventory
            </button>
          </div>

          {topProducts.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '32px', color: '#666' }}>
              <Package size={32} style={{ margin: '0 auto 8px', color: '#AAA' }} />
              <p style={{ fontSize: '13px', fontWeight: 600 }}>No sales recorded yet today.</p>
              <p style={{ fontSize: '12px', color: '#888' }}>Complete a checkout on the Android App or POS terminal.</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {topProducts.slice(0, 5).map((p, idx) => (
                <div 
                  key={idx}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    border: '1.5px solid #0A0A0A',
                    borderRadius: '8px',
                    background: idx === 0 ? '#FEF9C3' : '#FFF',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span style={{ 
                      width: '24px', 
                      height: '24px', 
                      borderRadius: '6px', 
                      background: idx === 0 ? '#F59E0B' : '#0A0A0A', 
                      color: '#FFF', 
                      fontSize: '11px', 
                      fontWeight: 900, 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'center' 
                    }}>
                      #{idx + 1}
                    </span>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: '13px' }}>{p.name}</div>
                      <div style={{ fontSize: '11px', color: '#666' }}>{p.sales_count} units sold</div>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontWeight: 900, fontSize: '14px', color: '#0A0A0A' }}>
                      ₹{Number(p.revenue || 0).toFixed(2)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Recent Transactions Feed */}
      <div className="neu-box" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
          <div>
            <h3 style={{ fontSize: '18px', fontWeight: 800 }}>Recent Customer Invoices</h3>
            <p style={{ fontSize: '12px', color: '#666' }}>Live bills emitted from the AI POS counters</p>
          </div>
          <button 
            onClick={() => setActiveTab('bills')}
            className="neu-btn neu-btn-sm neu-btn-yellow"
          >
            <Receipt size={14} /> View All Bills
          </button>
        </div>

        {recentBills.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '36px', color: '#777' }}>
            <Receipt size={36} style={{ margin: '0 auto 8px', color: '#BBB' }} />
            <p style={{ fontSize: '14px', fontWeight: 700 }}>No bills generated yet.</p>
            <p style={{ fontSize: '12px' }}>Bills generated from the Android POS app will appear here automatically.</p>
          </div>
        ) : (
          <div className="neu-table-container">
            <table className="neu-table">
              <thead>
                <tr>
                  <th>Invoice #</th>
                  <th>Customer</th>
                  <th>Payment</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {recentBills.slice(0, 6).map((b) => (
                  <tr key={b.id}>
                    <td style={{ fontFamily: 'var(--font-heading)', fontWeight: 800 }}>
                      {b.bill_number}
                    </td>
                    <td>
                      <div style={{ fontWeight: 700 }}>{b.customer_name || 'Walk-in Customer'}</div>
                      {b.customer_phone && (
                        <div style={{ fontSize: '11px', color: '#666' }}>{b.customer_phone}</div>
                      )}
                    </td>
                    <td>
                      <span className={`neu-badge ${b.payment_mode?.toUpperCase() === 'UPI' ? 'neu-badge-purple' : 'neu-badge-green'}`}>
                        {b.payment_mode || 'CASH'}
                      </span>
                    </td>
                    <td style={{ fontWeight: 600 }}>
                      {b.items?.length || 1} items
                    </td>
                    <td style={{ fontFamily: 'var(--font-heading)', fontWeight: 900, fontSize: '15px' }}>
                      ₹{Number(b.total_amount || 0).toFixed(2)}
                    </td>
                    <td>
                      <span className="neu-badge neu-badge-green">
                        PAID
                      </span>
                    </td>
                    <td>
                      <button
                        onClick={() => onSelectBill(b)}
                        className="neu-btn neu-btn-sm"
                        title="View Receipt"
                      >
                        <Eye size={12} /> Receipt
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

import React, { useState } from 'react';
import { 
  Store, 
  Smartphone, 
  Server, 
  ShieldCheck, 
  Save, 
  Check, 
  Radio, 
  RefreshCw, 
  HardDrive,
  Cpu
} from 'lucide-react';

export default function SettingsPage() {
  const [storeData, setStoreData] = useState(() => {
    const saved = localStorage.getItem('scansnap_store_settings');
    if (saved) {
      try { return JSON.parse(saved); } catch (e) {}
    }
    return {
      storeName: 'ScanSnap AI Supermarket',
      gstin: '19ABCDE1234F1Z5',
      fssai: '1282101900012',
      phone: '+91 98765 43210',
      email: 'store.admin@scansnap.ai',
      address: 'Shop 14-16, Commercial Complex, Sector 4, Tech City',
      defaultGst: '18',
      currency: 'INR (₹)',
      upiId: 'scansnap.retail@okhdfcbank',
    };
  });

  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSave = (e) => {
    e.preventDefault();
    localStorage.setItem('scansnap_store_settings', JSON.stringify(storeData));
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 3000);
  };

  const terminals = [
    {
      id: 'TERM-01',
      name: 'Android AI Vision Handheld',
      device: 'Samsung Galaxy A54 5G (Android 14)',
      ip: '192.168.1.42',
      status: 'ONLINE',
      mode: 'Live Camera Vision Scanner',
      lastSeen: '4 seconds ago',
    },
    {
      id: 'TERM-02',
      name: 'Counter #1 Checkout Register',
      device: 'Web POS Station (Port 3000)',
      ip: '127.0.0.1',
      status: 'ONLINE',
      mode: 'Admin Command & Billing',
      lastSeen: 'Active Now',
    },
    {
      id: 'TERM-03',
      name: 'Aisle #2 Smart Cart Kiosk',
      device: 'Raspberry Pi 4 / Touch Display',
      ip: '192.168.1.88',
      status: 'STANDBY',
      mode: 'Self-Service Price Checker',
      lastSeen: '12 minutes ago',
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {savedSuccess && (
        <div className="toast-banner" style={{ background: '#DCFCE7', color: '#14532D' }}>
          <Check size={18} />
          <span>Store settings saved successfully!</span>
        </div>
      )}

      {/* Grid: Store details & Connected terminals */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '24px' }}>
        {/* Left: Store Profile Form */}
        <div className="neu-box" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '18px' }}>
            <Store size={22} color="#1D4ED8" />
            <h3 style={{ fontSize: '18px', fontWeight: 800 }}>Store Profile & Tax Settings</h3>
          </div>

          <form onSubmit={handleSave} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                Store Business Name
              </label>
              <input 
                type="text"
                required
                value={storeData.storeName}
                onChange={(e) => setStoreData({ ...storeData, storeName: e.target.value })}
                className="neu-input"
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                GSTIN / Tax ID
              </label>
              <input 
                type="text"
                value={storeData.gstin}
                onChange={(e) => setStoreData({ ...storeData, gstin: e.target.value })}
                className="neu-input"
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                FSSAI License No.
              </label>
              <input 
                type="text"
                value={storeData.fssai}
                onChange={(e) => setStoreData({ ...storeData, fssai: e.target.value })}
                className="neu-input"
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                Store Contact Phone
              </label>
              <input 
                type="text"
                value={storeData.phone}
                onChange={(e) => setStoreData({ ...storeData, phone: e.target.value })}
                className="neu-input"
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                UPI ID for QR Payments
              </label>
              <input 
                type="text"
                value={storeData.upiId}
                onChange={(e) => setStoreData({ ...storeData, upiId: e.target.value })}
                className="neu-input"
              />
            </div>

            <div style={{ gridColumn: '1 / -1' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '6px' }}>
                Store Address (Printed on Receipts)
              </label>
              <textarea 
                rows={2}
                value={storeData.address}
                onChange={(e) => setStoreData({ ...storeData, address: e.target.value })}
                className="neu-input"
                style={{ resize: 'none' }}
              />
            </div>

            <div style={{ gridColumn: '1 / -1', marginTop: '10px' }}>
              <button type="submit" className="neu-btn neu-btn-primary">
                <Save size={16} /> Save Store Information
              </button>
            </div>
          </form>
        </div>

        {/* Right: Server & AI Engine Status */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div className="neu-box" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <Server size={20} color="#059669" />
              <h3 style={{ fontSize: '16px', fontWeight: 800 }}>Core System Health</h3>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: '#F0FDF4', border: '1.5px solid #0A0A0A', borderRadius: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10B981' }} />
                  <div>
                    <div style={{ fontWeight: 800, fontSize: '13px' }}>FastAPI Backend</div>
                    <div style={{ fontSize: '11px', color: '#666' }}>http://127.0.0.1:8000</div>
                  </div>
                </div>
                <span className="neu-badge neu-badge-green">200 OK</span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: '#F0FDF4', border: '1.5px solid #0A0A0A', borderRadius: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <HardDrive size={16} color="#065F46" />
                  <div>
                    <div style={{ fontWeight: 800, fontSize: '13px' }}>National Catalog DB</div>
                    <div style={{ fontSize: '11px', color: '#666' }}>117,000+ Indian SKUs Loaded</div>
                  </div>
                </div>
                <span className="neu-badge neu-badge-green">SYNCED</span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: '#F3E8FF', border: '1.5px solid #0A0A0A', borderRadius: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Cpu size={16} color="#7C3AED" />
                  <div>
                    <div style={{ fontWeight: 800, fontSize: '13px' }}>YOLOv8 Engine</div>
                    <div style={{ fontSize: '11px', color: '#666' }}>Weights: best.pt</div>
                  </div>
                </div>
                <span className="neu-badge neu-badge-purple">READY</span>
              </div>
            </div>
          </div>

          {/* Quick info note */}
          <div style={{ padding: '16px', background: '#FEF3C7', border: 'var(--neu-border-sm)', borderRadius: '10px' }}>
            <h4 style={{ fontWeight: 800, fontSize: '13px', color: '#92400E', marginBottom: '4px' }}>
              💡 Seamless Local Network Pairing
            </h4>
            <p style={{ fontSize: '12px', color: '#78350F', lineHeight: 1.4 }}>
              The Android app connects to your machine IP at port 8000. Changes made in this Admin Portal (price updates, new SKUs) are immediately reflected in the mobile scanner.
            </p>
          </div>
        </div>
      </div>

      {/* Connected Terminals Section */}
      <div className="neu-box" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
          <div>
            <h3 style={{ fontSize: '18px', fontWeight: 800 }}>Connected POS Terminals & Devices</h3>
            <p style={{ fontSize: '12px', color: '#666' }}>Hardware active across store checkout lanes and aisles</p>
          </div>
          <span className="neu-badge neu-badge-green">2 Live Devices</span>
        </div>

        <div className="neu-table-container">
          <table className="neu-table">
            <thead>
              <tr>
                <th>Terminal ID</th>
                <th>Device Name</th>
                <th>Hardware / OS</th>
                <th>IP Address</th>
                <th>Operating Mode</th>
                <th>Heartbeat</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {terminals.map((t) => (
                <tr key={t.id}>
                  <td>
                    <code style={{ background: '#F3F4F6', padding: '3px 8px', borderRadius: '4px', border: '1px solid #0A0A0A', fontWeight: 800 }}>
                      {t.id}
                    </code>
                  </td>
                  <td style={{ fontWeight: 800 }}>{t.name}</td>
                  <td style={{ fontSize: '12px', color: '#555' }}>{t.device}</td>
                  <td style={{ fontSize: '12px', fontFamily: 'monospace' }}>{t.ip}</td>
                  <td>
                    <span className="neu-badge neu-badge-blue">{t.mode}</span>
                  </td>
                  <td style={{ fontSize: '12px', fontWeight: 600 }}>{t.lastSeen}</td>
                  <td>
                    <span className={`neu-badge ${t.status === 'ONLINE' ? 'neu-badge-green' : 'neu-badge-yellow'}`}>
                      {t.status === 'ONLINE' && <Radio size={10} color="#059669" />}
                      {t.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

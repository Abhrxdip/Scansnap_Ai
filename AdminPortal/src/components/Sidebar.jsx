import React from 'react';
import { 
  LayoutDashboard, 
  Package, 
  Receipt, 
  Sparkles, 
  Sliders, 
  Store, 
  Radio, 
  ChevronRight,
  TrendingUp
} from 'lucide-react';

export default function Sidebar({ activeTab, setActiveTab }) {
  const menuItems = [
    { id: 'dashboard', label: 'Command Center', icon: LayoutDashboard, badge: 'Live' },
    { id: 'inventory', label: 'Master Inventory', icon: Package },
    { id: 'bills', label: 'Invoice Archive', icon: Receipt },
    { id: 'ai-studio', label: 'Visual AI Studio', icon: Sparkles, badge: 'YOLOv8' },
    { id: 'settings', label: 'Store & Terminals', icon: Sliders },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-logo-icon">⚡</div>
        <div>
          <h2 style={{ fontSize: '18px', fontWeight: 800, lineHeight: 1.1 }}>ScanSnap AI</h2>
          <span style={{ fontSize: '11px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#1A1A1A' }}>
            Store Admin OS
          </span>
        </div>
      </div>

      <div className="sidebar-nav">
        <div style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#666', padding: '6px 12px' }}>
          Navigation
        </div>

        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`nav-item ${isActive ? 'active' : ''}`}
              style={{ width: '100%', textAlign: 'left', background: isActive ? undefined : 'transparent' }}
            >
              <Icon size={18} strokeWidth={2.4} />
              <span style={{ flex: 1 }}>{item.label}</span>
              {item.badge && (
                <span
                  style={{
                    fontSize: '10px',
                    fontWeight: 800,
                    padding: '2px 6px',
                    borderRadius: '4px',
                    background: isActive ? '#FFFFFF' : '#FEF08A',
                    color: '#0A0A0A',
                    border: '1.5px solid #0A0A0A',
                  }}
                >
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}

        <div style={{ marginTop: '24px', padding: '16px', background: '#FEF3C7', border: 'var(--neu-border-sm)', borderRadius: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
            <TrendingUp size={18} color="#B45309" />
            <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 800, fontSize: '13px', color: '#92400E' }}>
              Quick Tip
            </span>
          </div>
          <p style={{ fontSize: '12px', color: '#78350F', lineHeight: 1.4 }}>
            ScanSnap detects items in real-time. Use <strong>Master Inventory</strong> to import products from the 117K Indian grocery database!
          </p>
        </div>
      </div>

      <div className="sidebar-footer">
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{ position: 'relative' }}>
            <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: '#34D399', border: '2px solid #0A0A0A', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800 }}>
              POS
            </div>
            <span style={{ position: 'absolute', top: -3, right: -3, width: '10px', height: '10px', borderRadius: '50%', background: '#10B981', border: '1.5px solid #0A0A0A' }} />
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: '13px', fontWeight: 800, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              Terminal #01 Online
            </div>
            <div style={{ fontSize: '11px', color: '#555', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <Radio size={10} color="#059669" /> Port 8000 Sync
            </div>
          </div>
        </div>
      </div>
    </aside>
  );
}

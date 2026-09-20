import React from 'react';
import { RefreshCw, Store, Bell, CheckCircle2 } from 'lucide-react';

export default function TopBar({ title, subtitle, onRefresh, isRefreshing }) {
  return (
    <header className="topbar">
      <div>
        <h1 style={{ fontSize: '20px', fontWeight: 800, color: 'var(--neu-black)' }}>
          {title}
        </h1>
        {subtitle && (
          <p style={{ fontSize: '13px', color: '#666', fontWeight: 500 }}>
            {subtitle}
          </p>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '6px 12px', background: '#D1FAE5', border: '2px solid #0A0A0A', borderRadius: '8px', fontSize: '12px', fontWeight: 700 }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10B981', display: 'inline-block' }} />
          <span>LIVE STORE #1</span>
        </div>

        {onRefresh && (
          <button
            onClick={onRefresh}
            className="neu-btn neu-btn-sm"
            disabled={isRefreshing}
            title="Refresh Store Data"
          >
            <RefreshCw size={14} className={isRefreshing ? 'spin-anim' : ''} />
            <span>{isRefreshing ? 'Syncing...' : 'Sync Data'}</span>
          </button>
        )}

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '4px 10px 4px 6px', background: 'var(--neu-surface-secondary)', border: '2px solid #0A0A0A', borderRadius: '10px' }}>
          <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'var(--neu-yellow)', border: '2px solid #0A0A0A', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: '14px' }}>
            AP
          </div>
          <div>
            <div style={{ fontSize: '12px', fontWeight: 800 }}>Abhradeep Admin</div>
            <div style={{ fontSize: '10px', color: '#666', fontWeight: 600 }}>Super Admin</div>
          </div>
        </div>
      </div>
    </header>
  );
}

import React, { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import DashboardPage from './pages/DashboardPage';
import InventoryPage from './pages/InventoryPage';
import BillsPage from './pages/BillsPage';
import AiStudioPage from './pages/AiStudioPage';
import SettingsPage from './pages/SettingsPage';
import OffersPage from './pages/OffersPage';
import LossPreventionShield from './components/LossPreventionShield';

export default function App() {
  const getInitialTab = () => {
    if (typeof window !== 'undefined') {
      const hash = window.location.hash.replace('#', '').toLowerCase();
      if (['loss-prevention', 'prevention', 'shrink'].includes(hash)) return 'loss-prevention';
      if (['ai-studio', 'vision'].includes(hash)) return 'ai-studio';
      if (['inventory', 'bills', 'offers', 'settings'].includes(hash)) return hash;
    }
    return 'dashboard';
  };

  const [activeTab, setActiveTabState] = useState(getInitialTab);
  const [selectedBill, setSelectedBill] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const setActiveTab = (tab) => {
    setActiveTabState(tab);
    if (typeof window !== 'undefined') {
      window.location.hash = tab;
    }
  };

  useEffect(() => {
    const handleHashChange = () => {
      const hash = window.location.hash.replace('#', '').toLowerCase();
      if (['loss-prevention', 'prevention', 'shrink'].includes(hash)) setActiveTabState('loss-prevention');
      else if (['dashboard', 'inventory', 'bills', 'offers', 'ai-studio', 'settings'].includes(hash)) {
        setActiveTabState(hash);
      }
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  // Tab configurations
  const tabTitles = {
    dashboard: {
      title: 'Store Command Center',
      subtitle: 'Real-time sales velocity, revenue metrics, and inventory alerts',
    },
    'loss-prevention': {
      title: 'Retail Loss Prevention & Shrink Shield',
      subtitle: 'Real-time CCTV lane audits, ticket-switching defense & automated POS fraud detection',
    },
    inventory: {
      title: 'Master Inventory Studio',
      subtitle: 'Manage catalog SKUs, batch CSV import/export & 117K grocery lookup',
    },
    bills: {
      title: 'Invoice & Billing Archive',
      subtitle: 'Search past customer orders, generate GST tax receipts & audits',
    },
    offers: {
      title: 'Festivals & Dynamic Combos',
      subtitle: '9 Indian festival discounts, smart FMCG upsell packages & margin simulator',
    },
    'ai-studio': {
      title: 'AI Vision Studio & Multi-Modal Lab',
      subtitle: 'YOLOv11 object detection, ML Kit OCR packaging intelligence & hackathon barcode test lab',
    },
    settings: {
      title: 'Store & POS Terminal Settings',
      subtitle: 'Business identity, GST credentials, and connected hardware registers',
    },
  };

  const handleRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      setIsRefreshing(false);
      window.location.reload();
    }, 600);
  };

  const handleSelectBill = (bill) => {
    setSelectedBill(bill);
    setActiveTab('bills');
  };

  const currentTab = tabTitles[activeTab] || tabTitles.dashboard;

  return (
    <div className="app-container">
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="main-content">
        <TopBar 
          title={currentTab.title}
          subtitle={currentTab.subtitle}
          onRefresh={handleRefresh}
          isRefreshing={isRefreshing}
        />

        <div className="page-body">
          {activeTab === 'dashboard' && (
            <DashboardPage 
              setActiveTab={setActiveTab} 
              onSelectBill={handleSelectBill} 
            />
          )}

          {activeTab === 'loss-prevention' && (
            <LossPreventionShield />
          )}

          {activeTab === 'inventory' && (
            <InventoryPage />
          )}

          {activeTab === 'bills' && (
            <BillsPage 
              selectedBill={selectedBill} 
              setSelectedBill={setSelectedBill} 
            />
          )}

          {activeTab === 'offers' && (
            <OffersPage />
          )}

          {activeTab === 'ai-studio' && (
            <AiStudioPage />
          )}

          {activeTab === 'settings' && (
            <SettingsPage />
          )}
        </div>
      </main>
    </div>
  );
}

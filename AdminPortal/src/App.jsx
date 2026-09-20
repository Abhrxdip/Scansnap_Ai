import React, { useState } from 'react';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import DashboardPage from './pages/DashboardPage';
import InventoryPage from './pages/InventoryPage';
import BillsPage from './pages/BillsPage';
import AiStudioPage from './pages/AiStudioPage';
import SettingsPage from './pages/SettingsPage';

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [selectedBill, setSelectedBill] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Tab configurations
  const tabTitles = {
    dashboard: {
      title: 'Store Command Center',
      subtitle: 'Real-time sales velocity, revenue metrics, and inventory alerts',
    },
    inventory: {
      title: 'Master Inventory Studio',
      subtitle: 'Manage catalog SKUs, batch CSV import/export & 117K grocery lookup',
    },
    bills: {
      title: 'Invoice & Billing Archive',
      subtitle: 'Search past customer orders, generate GST tax receipts & audits',
    },
    'ai-studio': {
      title: 'Visual AI Studio & Test Bench',
      subtitle: 'YOLOv8 retail detection pipeline, test image bench & class manager',
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

          {activeTab === 'inventory' && (
            <InventoryPage />
          )}

          {activeTab === 'bills' && (
            <BillsPage 
              selectedBill={selectedBill} 
              setSelectedBill={setSelectedBill} 
            />
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

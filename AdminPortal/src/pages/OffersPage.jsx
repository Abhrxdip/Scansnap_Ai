import React, { useState, useEffect } from 'react';
import { 
  Sparkles, 
  Flame, 
  Tag, 
  Percent, 
  ShoppingBag, 
  CheckCircle, 
  ArrowRight, 
  RefreshCw, 
  Zap, 
  Gift, 
  Calendar,
  AlertCircle
} from 'lucide-react';
import { 
  fetchFestivals, 
  overrideFestival, 
  fetchCombos, 
  calculateCartOffers 
} from '../api/client';

export default function OffersPage() {
  const [festivalsData, setFestivalsData] = useState({ active_festival: null, all_festivals: [], is_manual_override: false });
  const [combos, setCombos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [notification, setNotification] = useState(null);

  // Simulator state
  const SAMPLE_ITEMS = [
    { name: 'Amul Ice Cream Vanilla Magic', price: 40.0, icon: '🍦' },
    { name: 'Thums Up Charged Carbonated Drink', price: 40.0, icon: '🥤' },
    { name: 'Britannia Treat Chocolate Cake', price: 30.0, icon: '🍰' },
    { name: 'Nestle Everyday Dairy Whitener', price: 120.0, icon: '🥛' },
    { name: 'Head & Shoulders Cool Menthol Shampoo', price: 180.0, icon: '🧴' },
    { name: 'Plum Green Tea Face Wash', price: 95.0, icon: '✨' },
    { name: 'CeraVe Daily Moisturizing Lotion', price: 350.0, icon: '🧴' },
    { name: 'Wild Stone Code Platinum Deodorant', price: 220.0, icon: '🕶️' },
  ];

  const [simCart, setSimCart] = useState([
    { product_name: 'Amul Ice Cream Vanilla Magic', unit_price: 40.0, quantity: 1 },
    { product_name: 'Thums Up Charged Carbonated Drink', unit_price: 40.0, quantity: 1 }
  ]);
  const [calcResult, setCalcResult] = useState(null);
  const [calcLoading, setCalcLoading] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    runSimulator();
  }, [simCart, festivalsData.active_festival]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [fData, cData] = await Promise.all([
        fetchFestivals(),
        fetchCombos(),
      ]);
      setFestivalsData(fData);
      setCombos(cData.combos || []);
    } catch (err) {
      console.error(err);
      showNotice('Failed to load offers data', 'error');
    } finally {
      setLoading(false);
    }
  };

  const showNotice = (msg, type = 'success') => {
    setNotification({ msg, type });
    setTimeout(() => setNotification(null), 3500);
  };

  const handleActivateFestival = async (festivalName) => {
    setActionLoading(true);
    try {
      const res = await overrideFestival(festivalName);
      setFestivalsData(prev => ({
        ...prev,
        active_festival: res.active_festival,
        is_manual_override: res.is_manual_override
      }));
      showNotice(`🎉 ${festivalName} is now LIVE across store & POS terminals!`, 'success');
    } catch (err) {
      showNotice('Failed to activate festival season', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleResetFestival = async () => {
    setActionLoading(true);
    try {
      const res = await overrideFestival(null);
      setFestivalsData(prev => ({
        ...prev,
        active_festival: res.active_festival,
        is_manual_override: false
      }));
      showNotice('🔄 Reverted to calendar-based automated festival detection.', 'success');
    } catch (err) {
      showNotice('Failed to reset festival', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const toggleCartItem = (sample) => {
    const existing = simCart.find(i => i.product_name === sample.name);
    if (existing) {
      setSimCart(simCart.filter(i => i.product_name !== sample.name));
    } else {
      setSimCart([...simCart, { product_name: sample.name, unit_price: sample.price, quantity: 1 }]);
    }
  };

  const runSimulator = async () => {
    if (simCart.length === 0) {
      setCalcResult(null);
      return;
    }
    setCalcLoading(true);
    try {
      const res = await calculateCartOffers(simCart, true, true);
      setCalcResult(res);
    } catch (err) {
      console.error(err);
    } finally {
      setCalcLoading(false);
    }
  };

  const activeFest = festivalsData.active_festival;

  return (
    <div className="offers-page" style={{ paddingBottom: '60px' }}>
      {notification && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '24px',
          zIndex: 9999,
          background: notification.type === 'error' ? '#FEE2E2' : '#D1FAE5',
          border: '2px solid #0A0A0A',
          boxShadow: '4px 4px 0px #0A0A0A',
          padding: '12px 20px',
          borderRadius: '8px',
          fontWeight: 800,
          color: notification.type === 'error' ? '#991B1B' : '#065F46',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          animation: 'slideIn 0.2s ease-out'
        }}>
          {notification.type === 'error' ? <AlertCircle size={20} /> : <CheckCircle size={20} />}
          {notification.msg}
        </div>
      )}

      {/* ── Active Festival Hero Banner ─────────────────────────────────── */}
      <div style={{
        background: 'linear-gradient(135deg, #FEF08A 0%, #FDE047 100%)',
        border: '3px solid #0A0A0A',
        borderRadius: '16px',
        boxShadow: '6px 6px 0px #0A0A0A',
        padding: '24px 28px',
        marginBottom: '28px',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
          <div style={{ flex: 1, minWidth: '280px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <span style={{
                background: '#0A0A0A',
                color: '#FEF08A',
                fontWeight: 900,
                fontSize: '11px',
                padding: '4px 10px',
                borderRadius: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.06em',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px'
              }}>
                <Flame size={14} color="#F59E0B" /> ACTIVE PRICING SEASON
              </span>
              {festivalsData.is_manual_override && (
                <span style={{
                  background: '#EC4899',
                  color: '#FFFFFF',
                  fontWeight: 800,
                  fontSize: '11px',
                  padding: '4px 8px',
                  borderRadius: '6px',
                  border: '1.5px solid #0A0A0A'
                }}>
                  DEMO OVERRIDE ACTIVE
                </span>
              )}
            </div>

            <h1 style={{ fontSize: '26px', fontWeight: 900, color: '#0A0A0A', margin: '4px 0 8px 0' }}>
              {activeFest ? activeFest.banner : '🛍️ Standard Retail Pricing Active'}
            </h1>
            <p style={{ fontSize: '14px', color: '#4B5563', margin: 0, fontWeight: 600 }}>
              {activeFest ? (
                <>All checkout carts automatically receive <strong>{activeFest.discount_percent}% Flat Discount</strong> across FMCG & Kirana products.</>
              ) : (
                'Automated seasonal calendar is monitoring upcoming Indian holidays.'
              )}
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {festivalsData.is_manual_override && (
              <button
                onClick={handleResetFestival}
                disabled={actionLoading}
                className="btn btn-secondary"
                style={{
                  background: '#FFFFFF',
                  fontWeight: 800,
                  fontSize: '13px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}
              >
                <RefreshCw size={14} /> Reset to Auto Date
              </button>
            )}
            <div style={{
              background: '#0A0A0A',
              color: '#FFFFFF',
              padding: '12px 18px',
              borderRadius: '10px',
              border: '2px solid #0A0A0A',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '11px', textTransform: 'uppercase', fontWeight: 700, color: '#9CA3AF' }}>Active Rate</div>
              <div style={{ fontSize: '24px', fontWeight: 900, color: '#FEF08A' }}>
                {activeFest ? `${activeFest.discount_percent}%` : '0%'}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: '28px' }}>
        {/* ── LEFT COLUMN: Festivals & Combos Catalog ──────────────────── */}
        <div>
          {/* 1. Indian Festivals Season Selector */}
          <div style={{ marginBottom: '32px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
              <div>
                <h2 style={{ fontSize: '18px', fontWeight: 900, margin: 0 }}>
                  🇮🇳 9 Indian Festival Seasons
                </h2>
                <p style={{ fontSize: '12px', color: '#6B7280', margin: '2px 0 0 0', fontWeight: 600 }}>
                  Test seasonal demand surges and dynamic festival discounts in 1 click
                </p>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '12px' }}>
              {festivalsData.all_festivals.map((fest) => {
                const isSelected = activeFest && activeFest.name === fest.name;
                return (
                  <div
                    key={fest.name}
                    style={{
                      background: isSelected ? '#FEF9C3' : '#FFFFFF',
                      border: isSelected ? '3px solid #0A0A0A' : '2px solid #0A0A0A',
                      borderRadius: '12px',
                      boxShadow: isSelected ? '4px 4px 0px #0A0A0A' : '2px 2px 0px #0A0A0A',
                      padding: '14px',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                        <span style={{ fontSize: '12px', fontWeight: 800, color: '#0A0A0A' }}>
                          {fest.name}
                        </span>
                        <span style={{
                          background: '#ECFDF5',
                          color: '#059669',
                          fontWeight: 900,
                          fontSize: '11px',
                          padding: '2px 6px',
                          borderRadius: '4px',
                          border: '1.5px solid #0A0A0A'
                        }}>
                          {fest.discount_percent}% OFF
                        </span>
                      </div>
                      <p style={{ fontSize: '11px', color: '#6B7280', margin: '0 0 10px 0', lineHeight: 1.3 }}>
                        {fest.banner}
                      </p>
                    </div>

                    <button
                      onClick={() => handleActivateFestival(fest.name)}
                      disabled={actionLoading || isSelected}
                      style={{
                        width: '100%',
                        padding: '6px 10px',
                        background: isSelected ? '#10B981' : '#0A0A0A',
                        color: '#FFFFFF',
                        border: '1.5px solid #0A0A0A',
                        borderRadius: '6px',
                        fontWeight: 800,
                        fontSize: '11px',
                        cursor: isSelected ? 'default' : 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px'
                      }}
                    >
                      {isSelected ? (
                        <><CheckCircle size={12} /> Currently Live</>
                      ) : (
                        <><Zap size={12} /> Activate for Demo</>
                      )}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* 2. Smart Combo Packages */}
          <div>
            <div style={{ marginBottom: '14px' }}>
              <h2 style={{ fontSize: '18px', fontWeight: 900, margin: 0 }}>
                ⚡ Smart FMCG Combo Bundles (Basket Upsell Engine)
              </h2>
              <p style={{ fontSize: '12px', color: '#6B7280', margin: '2px 0 0 0', fontWeight: 600 }}>
                Rule-based recommendation engine triggers dynamic bundle discounts when matched
              </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '14px' }}>
              {combos.map((combo) => (
                <div
                  key={combo.id}
                  style={{
                    background: '#FFFFFF',
                    border: '2.5px solid #0A0A0A',
                    borderRadius: '12px',
                    boxShadow: '3px 3px 0px #0A0A0A',
                    padding: '16px',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between'
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                      <span style={{
                        background: '#EEF2FF',
                        color: '#4F46E5',
                        border: '1.5px solid #0A0A0A',
                        borderRadius: '6px',
                        fontSize: '10px',
                        fontWeight: 900,
                        padding: '2px 6px',
                        textTransform: 'uppercase'
                      }}>
                        {combo.badge}
                      </span>
                      <span style={{
                        background: '#FEF08A',
                        color: '#0A0A0A',
                        fontWeight: 900,
                        fontSize: '12px',
                        padding: '2px 8px',
                        borderRadius: '6px',
                        border: '1.5px solid #0A0A0A'
                      }}>
                        {combo.discount_percent}% COMBO
                      </span>
                    </div>

                    <h3 style={{ fontSize: '14px', fontWeight: 900, color: '#0A0A0A', margin: '0 0 6px 0' }}>
                      {combo.name}
                    </h3>
                    <p style={{ fontSize: '11px', color: '#6B7280', margin: '0 0 10px 0', lineHeight: 1.4 }}>
                      {combo.description}
                    </p>

                    <div style={{
                      background: '#F9FAFB',
                      border: '1px solid #E5E7EB',
                      borderRadius: '6px',
                      padding: '8px',
                      marginBottom: '10px'
                    }}>
                      <div style={{ fontSize: '10px', fontWeight: 800, textTransform: 'uppercase', color: '#6B7280', marginBottom: '4px' }}>
                        Required Products:
                      </div>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                        {combo.display_items.map((item, idx) => (
                          <span
                            key={idx}
                            style={{
                              background: '#FFFFFF',
                              border: '1px solid #0A0A0A',
                              borderRadius: '4px',
                              padding: '2px 6px',
                              fontSize: '10px',
                              fontWeight: 700
                            }}
                          >
                            {item}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div style={{ fontSize: '10px', color: '#059669', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <CheckCircle size={12} /> Auto-applied in mobile checkout
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── RIGHT COLUMN: Interactive Cart Simulator ─────────────────── */}
        <div>
          <div style={{
            background: '#FFFFFF',
            border: '3px solid #0A0A0A',
            borderRadius: '16px',
            boxShadow: '6px 6px 0px #0A0A0A',
            padding: '20px',
            position: 'sticky',
            top: '20px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
              <div style={{
                background: '#A78BFA',
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                border: '2px solid #0A0A0A',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 900
              }}>
                🛒
              </div>
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 900, margin: 0 }}>
                  Interactive Cart & Margin Simulator
                </h3>
                <span style={{ fontSize: '11px', color: '#6B7280', fontWeight: 700 }}>
                  Click products to simulate real-time AI combo detection
                </span>
              </div>
            </div>

            {/* Product Pickers */}
            <div style={{ marginBottom: '16px' }}>
              <div style={{ fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', color: '#4B5563', marginBottom: '6px' }}>
                Select Sample Products:
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                {SAMPLE_ITEMS.map((item) => {
                  const inCart = simCart.some(i => i.product_name === item.name);
                  return (
                    <button
                      key={item.name}
                      onClick={() => toggleCartItem(item)}
                      style={{
                        padding: '6px 10px',
                        background: inCart ? '#0A0A0A' : '#F3F4F6',
                        color: inCart ? '#FFFFFF' : '#0A0A0A',
                        border: '1.5px solid #0A0A0A',
                        borderRadius: '6px',
                        fontSize: '11px',
                        fontWeight: 800,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '5px'
                      }}
                    >
                      <span>{item.icon}</span>
                      <span>{item.name.split(' ')[0]}</span>
                      <span style={{ opacity: 0.8 }}>₹{item.price}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Cart Items List */}
            <div style={{
              background: '#F9FAFB',
              border: '2px solid #0A0A0A',
              borderRadius: '10px',
              padding: '12px',
              marginBottom: '16px',
              maxHeight: '180px',
              overflowY: 'auto'
            }}>
              <div style={{ fontSize: '11px', fontWeight: 800, color: '#6B7280', marginBottom: '6px' }}>
                Current Cart Items ({simCart.length}):
              </div>
              {simCart.length === 0 ? (
                <div style={{ fontSize: '12px', color: '#9CA3AF', textAlign: 'center', padding: '16px 0' }}>
                  Click buttons above to add items to cart
                </div>
              ) : (
                simCart.map((item, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      fontSize: '12px',
                      fontWeight: 700,
                      padding: '4px 0',
                      borderBottom: idx < simCart.length - 1 ? '1px dashed #E5E7EB' : 'none'
                    }}
                  >
                    <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '200px' }}>
                      {item.product_name}
                    </span>
                    <span style={{ fontWeight: 800 }}>₹{item.unit_price.toFixed(2)}</span>
                  </div>
                ))
              )}
            </div>

            {/* AI Combo Upsell Prompt */}
            {calcResult && calcResult.combo_suggestions && calcResult.combo_suggestions.length > 0 && (
              <div style={{
                background: '#FEF3C7',
                border: '2px solid #0A0A0A',
                borderRadius: '8px',
                padding: '10px 12px',
                marginBottom: '16px'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', fontWeight: 900, color: '#92400E', marginBottom: '4px' }}>
                  <Sparkles size={14} color="#D97706" /> AI SMART UPSELL PROMPT:
                </div>
                <div style={{ fontSize: '11px', color: '#78350F', fontWeight: 700, lineHeight: 1.3 }}>
                  {calcResult.combo_suggestions[0].prompt}
                </div>
              </div>
            )}

            {/* Calculations Breakdown */}
            {calcResult && (
              <div style={{
                background: '#FFFFFF',
                border: '2px solid #0A0A0A',
                borderRadius: '10px',
                padding: '14px',
                marginBottom: '16px'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '6px', color: '#4B5563' }}>
                  <span>Gross Subtotal</span>
                  <span style={{ fontWeight: 800 }}>₹{calcResult.original_total.toFixed(2)}</span>
                </div>

                {calcResult.applied_offers.map((offer, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      fontSize: '11px',
                      color: '#059669',
                      fontWeight: 800,
                      marginBottom: '4px'
                    }}
                  >
                    <span>{offer.name} ({offer.discount_percent}%)</span>
                    <span>-₹{offer.saved_amount.toFixed(2)}</span>
                  </div>
                ))}

                <div style={{
                  borderTop: '2px solid #0A0A0A',
                  paddingTop: '8px',
                  marginTop: '8px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'baseline'
                }}>
                  <span style={{ fontSize: '14px', fontWeight: 900 }}>Final Customer Bill:</span>
                  <span style={{ fontSize: '20px', fontWeight: 900, color: '#0A0A0A' }}>
                    ₹{calcResult.discounted_total.toFixed(2)}
                  </span>
                </div>

                {calcResult.total_savings > 0 && (
                  <div style={{
                    marginTop: '8px',
                    textAlign: 'center',
                    background: '#D1FAE5',
                    color: '#065F46',
                    fontSize: '11px',
                    fontWeight: 900,
                    padding: '4px',
                    borderRadius: '4px',
                    border: '1px solid #10B981'
                  }}>
                    Customer Saves: ₹{calcResult.total_savings.toFixed(2)}!
                  </div>
                )}
              </div>
            )}

            <div style={{ fontSize: '10px', color: '#6B7280', textAlign: 'center', fontWeight: 600 }}>
              ⚡ Connected in real-time to Android POS via <code>/api/offers/calculate</code>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

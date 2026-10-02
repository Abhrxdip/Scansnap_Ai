import React, { useState } from 'react';
import { Copy, Check, QrCode, Tag, ArrowRight, Eye, CheckCircle2 } from 'lucide-react';

// Standard Code 128B patterns (Index 0-106)
const CODE128_PATTERNS = [
  "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
  "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
  "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
  "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
  "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
  "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
  "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
  "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
  "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
  "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
  "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
];

export function generateCode128SvgBars(text, height = 75, moduleWidth = 2) {
  if (!text) return null;

  // Code 128B start index is 104
  const startCode = 104;
  let checksum = startCode;
  const patternList = [CODE128_PATTERNS[startCode]];

  for (let i = 0; i < text.length; i++) {
    const charCode = text.charCodeAt(i);
    const value = charCode >= 32 && charCode <= 126 ? charCode - 32 : 0;
    checksum += value * (i + 1);
    patternList.push(CODE128_PATTERNS[value] || CODE128_PATTERNS[0]);
  }

  const checkVal = checksum % 103;
  patternList.push(CODE128_PATTERNS[checkVal]);
  // Stop pattern index 106
  patternList.push(CODE128_PATTERNS[106]);

  const quietZone = 20; // 10 modules on each side
  let currentX = quietZone;
  const rects = [];

  patternList.forEach((pattern, patIdx) => {
    let isBar = true;
    for (let charIdx = 0; charIdx < pattern.length; charIdx++) {
      const width = parseInt(pattern[charIdx], 10) * moduleWidth;
      if (isBar) {
        rects.push(
          <rect
            key={`bar-${patIdx}-${charIdx}-${currentX}`}
            x={currentX}
            y={0}
            width={width}
            height={height}
            fill="#0A0A0A"
          />
        );
      }
      currentX += width;
      isBar = !isBar;
    }
  });

  const totalWidth = currentX + quietZone;

  return {
    svg: (
      <svg
        viewBox={`0 0 ${totalWidth} ${height + 24}`}
        width="100%"
        height="100%"
        style={{ display: 'block', background: '#FFFFFF', borderRadius: '4px' }}
      >
        <rect x={0} y={0} width={totalWidth} height={height + 24} fill="#FFFFFF" />
        {rects}
        <text
          x={totalWidth / 2}
          y={height + 17}
          textAnchor="middle"
          fontFamily="monospace, Consolas, sans-serif"
          fontSize="14"
          fontWeight="bold"
          fill="#0A0A0A"
          letterSpacing="2.5"
        >
          {text}
        </text>
      </svg>
    ),
    width: totalWidth,
    height: height + 24
  };
}

export default function BarcodeCard({ product, onSimulateScan }) {
  const [copied, setCopied] = useState(false);
  const barcodeStr = product.barcode || '8901000000000';
  const barcodeObj = generateCode128SvgBars(barcodeStr, 65, 2);

  const handleCopy = () => {
    navigator.clipboard.writeText(barcodeStr);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const colorBadgeMap = {
    YELLOW: { bg: '#FEF08A', border: '#CA8A04', text: '#854D0E', label: 'Yellow/Gold' },
    BLUE: { bg: '#DBEAFE', border: '#2563EB', text: '#1E40AF', label: 'Blue Packaging' },
    RED: { bg: '#FEE2E2', border: '#DC2626', text: '#991B1B', label: 'Red Packaging' },
    GREEN: { bg: '#DCFCE7', border: '#16A34A', text: '#166534', label: 'Green Packaging' },
    PURPLE: { bg: '#F3E8FF', border: '#9333EA', text: '#6B21A8', label: 'Purple Packaging' },
    ORANGE: { bg: '#FFEDD5', border: '#EA580C', text: '#9A3412', label: 'Orange Packaging' }
  };

  const colorInfo = colorBadgeMap[product.color] || { bg: '#F1F5F9', border: '#64748B', text: '#334155', label: 'Neutral' };

  return (
    <div
      className="neu-box"
      style={{
        background: '#FFFFFF',
        padding: '18px',
        display: 'flex',
        flexDirection: 'column',
        gap: '14px',
        transition: 'transform 0.15s ease, box-shadow 0.15s ease',
        position: 'relative'
      }}
    >
      {/* Top Header Row */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '8px' }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px', flexWrap: 'wrap' }}>
            <span
              style={{
                fontSize: '10px',
                fontWeight: 800,
                textTransform: 'uppercase',
                padding: '2px 6px',
                borderRadius: '4px',
                background: '#F1F5F9',
                color: '#334155',
                border: '1px solid #CBD5E1'
              }}
            >
              {product.category || 'Retail FMCG'}
            </span>
            <span
              style={{
                fontSize: '10px',
                fontWeight: 800,
                padding: '2px 6px',
                borderRadius: '4px',
                background: colorInfo.bg,
                color: colorInfo.text,
                border: `1px solid ${colorInfo.border}`
              }}
            >
              🎨 {colorInfo.label}
            </span>
          </div>
          <h4
            style={{
              fontSize: '15px',
              fontWeight: 800,
              color: '#0A0A0A',
              lineHeight: 1.25,
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}
            title={product.name}
          >
            {product.name}
          </h4>
        </div>

        {/* Price & Quantity Badge */}
        <div style={{ textAlign: 'right' }}>
          <span
            style={{
              fontSize: '15px',
              fontWeight: 900,
              color: '#059669',
              background: '#D1FAE5',
              padding: '4px 8px',
              borderRadius: '6px',
              border: '1.5px solid #059669',
              display: 'inline-block'
            }}
          >
            ₹{product.price || product.suggested_price || 0}
          </span>
          {product.unit && (
            <div style={{ fontSize: '11px', fontWeight: 700, color: '#666', marginTop: '2px' }}>
              {product.unit}
            </div>
          )}
        </div>
      </div>

      {/* SVG Barcode Display Container */}
      <div
        style={{
          background: '#FFFFFF',
          border: '2px solid #0A0A0A',
          borderRadius: '8px',
          padding: '10px',
          boxShadow: 'inset 0 1px 4px rgba(0,0,0,0.06)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        <div style={{ width: '100%', maxWidth: '280px', height: '85px' }}>
          {barcodeObj?.svg}
        </div>
      </div>

      {/* OCR Packaging Ground Truth Text */}
      <div
        style={{
          background: '#F8FAFC',
          borderRadius: '6px',
          padding: '8px 10px',
          border: '1px dashed #CBD5E1',
          fontSize: '11px',
          color: '#475569'
        }}
      >
        <div style={{ fontWeight: 800, color: '#1E293B', marginBottom: '2px', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <span>📝 OCR Target Text:</span>
        </div>
        <div style={{ fontFamily: 'monospace', fontSize: '10.5px', color: '#0F172A', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {product.ocr_text || `${product.name} Net Wt ${product.unit || '100g'} MRP Rs ${product.price || 30.0}`}
        </div>
      </div>

      {/* Action Buttons Row */}
      <div style={{ display: 'flex', gap: '8px', marginTop: 'auto' }}>
        <button
          onClick={handleCopy}
          className="neu-btn neu-btn-sm"
          style={{ flex: 1, padding: '7px 10px', fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
        >
          {copied ? <Check size={14} color="#059669" /> : <Copy size={14} />}
          <span>{copied ? 'Copied!' : 'Copy Code'}</span>
        </button>

        {onSimulateScan && (
          <button
            onClick={() => onSimulateScan(product)}
            className="neu-btn neu-btn-sm neu-btn-yellow"
            style={{ padding: '7px 12px', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '5px' }}
            title="Simulate scanning this product in POS"
          >
            <QrCode size={14} />
            <span>Test POS</span>
          </button>
        )}
      </div>
    </div>
  );
}

import React, { useState, useRef, useEffect } from 'react';
import { 
  Sparkles, 
  Upload, 
  Eye, 
  Layers, 
  Sliders, 
  CheckCircle2, 
  AlertCircle, 
  RefreshCw, 
  Box, 
  Image as ImageIcon, 
  Check,
  ZoomIn,
  ZoomOut,
  RotateCw,
  Download,
  Tag,
  Maximize2
} from 'lucide-react';
import { detectObjectsInImage } from '../api/client';

const BOX_COLORS = [
  '#10B981', // Emerald
  '#3B82F6', // Blue
  '#F59E0B', // Amber
  '#EC4899', // Pink
  '#8B5CF6', // Purple
  '#06B6D4', // Cyan
  '#EF4444', // Red
  '#14B8A6'  // Teal
];

function hexToRgba(hex, alpha) {
  const clean = hex.replace('#', '');
  const r = parseInt(clean.substring(0, 2), 16);
  const g = parseInt(clean.substring(2, 4), 16);
  const b = parseInt(clean.substring(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function drawRoundedRect(ctx, x, y, width, height, radius) {
  if (ctx.roundRect) {
    ctx.beginPath();
    ctx.roundRect(x, y, width, height, radius);
  } else {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    ctx.lineTo(x + radius, y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
  }
}

export default function AiStudioPage() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [isDetecting, setIsDetecting] = useState(false);
  const [detectionResult, setDetectionResult] = useState(null);
  const [confidenceThreshold, setConfidenceThreshold] = useState(0.35);
  const [activeTab, setActiveTab] = useState('bench'); // 'bench' | 'classes'
  
  // Interactive canvas states
  const [zoom, setZoom] = useState(1.0);
  const [showLabels, setShowLabels] = useState(true);
  const [hoveredIdx, setHoveredIdx] = useState(null);

  const canvasRef = useRef(null);

  // Model classes trained in best.pt and registered in ScanSnap AI
  const trainedClasses = [
    { id: 0, name: 'Amul Ice Cream Cup 100ml', category: 'Dairy & Frozen', count: 'Dataset/Amul_Ice_Cream', accuracy: '98.5%' },
    { id: 1, name: 'Britannia Treat Chocolate Cake 65g', category: 'Dairy & Bakery', count: 'Dataset/Cake', accuracy: '98.0%' },
    { id: 2, name: 'CeraVe Daily Moisturizing Lotion 236ml', category: 'Personal Care', count: 'Dataset/CeraVe', accuracy: '99.0%' },
    { id: 3, name: 'Head & Shoulders Cool Menthol Shampoo 180ml', category: 'Personal Care', count: 'Dataset/HnS_Shampoo', accuracy: '98.5%' },
    { id: 4, name: 'Nestle Everyday Dairy Whitener Milk Powder 20g', category: 'Dairy & Beverages', count: 'Dataset/Nestle_Milk_Powder', accuracy: '98.0%' },
    { id: 5, name: 'Plum Green Tea Pore Cleansing Face Wash 100ml', category: 'Personal Care', count: 'Dataset/Plum', accuracy: '98.2%' },
    { id: 6, name: 'Thums Up Charged Carbonated Beverage 250ml Can', category: 'Beverages', count: 'Dataset/Thums_Up', accuracy: '99.2%' },
    { id: 7, name: 'Wild Stone Code Platinum Deodorant Spray 120ml', category: 'Personal Care', count: 'Dataset/Wild_Stone', accuracy: '98.8%' },
    { id: 8, name: 'Nivea Men Fresh Active Deodorant 150ml', category: 'Personal Care', count: 'Kirana Core Batch', accuracy: '97.6%' },
    { id: 9, name: 'Britannia Bourbon Chocolate Biscuits 150g', category: 'Snacks & Biscuits', count: 'Kirana Core Batch', accuracy: '98.1%' },
    { id: 10, name: 'Britannia Milk Bikis Biscuits 100g', category: 'Snacks & Biscuits', count: 'Kirana Core Batch', accuracy: '96.9%' },
    { id: 11, name: 'Maggi 2-Minute Masala Noodles 70g', category: 'Instant Foods', count: 'Kirana Core Batch', accuracy: '98.4%' },
    { id: 12, name: 'Surf Excel Easy Wash Detergent 500g', category: 'Household', count: 'Kirana Core Batch', accuracy: '97.5%' },
    { id: 13, name: 'Parle Hide & Seek Choco Chip Biscuits', category: 'Snacks & Biscuits', count: 'Kirana Core Batch', accuracy: '97.8%' },
  ];

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setSelectedFile(file);
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    setDetectionResult(null);
    setZoom(1.0);
    setHoveredIdx(null);
  };

  const drawDetections = (res, threshold, hIdx = hoveredIdx, labelsOn = showLabels) => {
    if (!previewUrl) return;
    const img = new Image();
    img.src = previewUrl;
    img.onload = () => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      
      const imgW = img.naturalWidth || img.width;
      const imgH = img.naturalHeight || img.height;
      canvas.width = imgW;
      canvas.height = imgH;

      // Draw clean source image without distortion
      ctx.drawImage(img, 0, 0, imgW, imgH);

      if (!res || !res.detections) return;

      const detections = res.detections;
      const strokeW = Math.max(3, Math.round(imgW / 260));
      const fontSize = Math.min(20, Math.max(13, Math.round(imgW / 65)));
      const cornerLen = Math.min(30, Math.max(14, Math.round(imgW / 45)));

      detections.forEach((det, idx) => {
        if (det.confidence < threshold) return;

        const isHovered = hIdx === idx;
        const hasHover = hIdx !== null;
        
        let [x1, y1, x2, y2] = det.box || (det.bbox ? [
          det.bbox[0] * imgW, 
          det.bbox[1] * imgH, 
          det.bbox[2] * imgW, 
          det.bbox[3] * imgH
        ] : [0, 0, 0, 0]);

        // Keep inside bounds
        x1 = Math.max(0, Math.min(imgW - 1, x1));
        y1 = Math.max(0, Math.min(imgH - 1, y1));
        x2 = Math.max(x1 + 4, Math.min(imgW, x2));
        y2 = Math.max(y1 + 4, Math.min(imgH, y2));
        const w = x2 - x1;
        const h = y2 - y1;
        const color = BOX_COLORS[idx % BOX_COLORS.length];

        ctx.save();

        if (hasHover && !isHovered) {
          ctx.globalAlpha = 0.35;
        }

        // Bounding Box Glow on hover
        if (isHovered) {
          ctx.shadowColor = color;
          ctx.shadowBlur = 16;
        }

        // Box border
        ctx.lineWidth = isHovered ? strokeW + 2 : strokeW;
        ctx.strokeStyle = color;
        ctx.strokeRect(x1, y1, w, h);

        // Highlight box interior
        ctx.fillStyle = isHovered ? hexToRgba(color, 0.22) : hexToRgba(color, 0.10);
        ctx.fillRect(x1, y1, w, h);

        // Corner bracket accents (HUD style)
        ctx.lineWidth = strokeW + 1.5;
        ctx.strokeStyle = '#FFFFFF';
        ctx.beginPath();
        // Top-left
        ctx.moveTo(x1, y1 + cornerLen);
        ctx.lineTo(x1, y1);
        ctx.lineTo(x1 + cornerLen, y1);
        // Top-right
        ctx.moveTo(x2 - cornerLen, y1);
        ctx.lineTo(x2, y1);
        ctx.lineTo(x2, y1 + cornerLen);
        // Bottom-left
        ctx.moveTo(x1, y2 - cornerLen);
        ctx.lineTo(x1, y2);
        ctx.lineTo(x1 + cornerLen, y2);
        // Bottom-right
        ctx.moveTo(x2 - cornerLen, y2);
        ctx.lineTo(x2, y2);
        ctx.lineTo(x2, y2 - cornerLen);
        ctx.stroke();

        // High-clarity badge
        if (labelsOn) {
          const rawName = det.class_name || det.label;
          const displayName = rawName.length > 26 ? rawName.substring(0, 24) + '…' : rawName;
          const confText = `${(det.confidence * 100).toFixed(0)}%`;

          ctx.font = `600 ${fontSize}px Inter, system-ui, -apple-system, sans-serif`;
          const nameMetrics = ctx.measureText(displayName);
          ctx.font = `bold ${Math.max(11, fontSize - 2)}px Inter, system-ui, -apple-system, sans-serif`;
          const confMetrics = ctx.measureText(confText);

          const padX = Math.round(fontSize * 0.55);
          const padY = Math.round(fontSize * 0.35);
          const badgeH = fontSize + (padY * 2);
          const badgeW = nameMetrics.width + confMetrics.width + (padX * 3.5) + (fontSize * 0.7);

          // Smart placement: never cut off top or right
          let badgeY = (y1 - badgeH - 6 < 0) ? (y1 + 6) : (y1 - badgeH - 6);
          let badgeX = Math.max(4, Math.min(x1, imgW - badgeW - 4));

          // Draw Badge Container
          ctx.shadowColor = 'rgba(0, 0, 0, 0.6)';
          ctx.shadowBlur = 10;
          ctx.fillStyle = '#0F172A';
          drawRoundedRect(ctx, badgeX, badgeY, badgeW, badgeH, 6);
          ctx.fill();

          ctx.shadowBlur = 0;
          ctx.strokeStyle = color;
          ctx.lineWidth = 1.5;
          ctx.stroke();

          // Left indicator dot
          const dotRadius = Math.round(fontSize * 0.28);
          const dotX = badgeX + padX + dotRadius;
          const dotY = badgeY + (badgeH / 2);
          ctx.fillStyle = color;
          ctx.beginPath();
          ctx.arc(dotX, dotY, dotRadius, 0, Math.PI * 2);
          ctx.fill();

          // Text label
          const textX = dotX + dotRadius + Math.round(fontSize * 0.4);
          const textY = badgeY + padY + fontSize - 2;
          ctx.font = `600 ${fontSize}px Inter, system-ui, -apple-system, sans-serif`;
          ctx.fillStyle = '#FFFFFF';
          ctx.fillText(displayName, textX, textY);

          // Confidence pill
          const confW = confMetrics.width + (padX * 1.2);
          const confH = badgeH - 6;
          const confX = badgeX + badgeW - confW - 4;
          const confY = badgeY + 3;

          ctx.fillStyle = color;
          drawRoundedRect(ctx, confX, confY, confW, confH, 4);
          ctx.fill();

          ctx.font = `bold ${Math.max(11, fontSize - 2)}px Inter, system-ui, -apple-system, sans-serif`;
          ctx.fillStyle = '#0A0A0A';
          ctx.fillText(confText, confX + (padX * 0.6), confY + confH - 3);
        }

        ctx.restore();
      });
    };
  };

  // Re-draw when threshold or hovered detection changes
  useEffect(() => {
    if (detectionResult) {
      drawDetections(detectionResult, confidenceThreshold, hoveredIdx, showLabels);
    }
  }, [confidenceThreshold, hoveredIdx, showLabels, previewUrl]);

  const handleThresholdChange = (val) => {
    setConfidenceThreshold(val);
  };

  const handleRotateClockwise = () => {
    if (!previewUrl) return;
    const img = new Image();
    img.src = previewUrl;
    img.onload = () => {
      const rotCanvas = document.createElement('canvas');
      rotCanvas.width = img.height;
      rotCanvas.height = img.width;
      const rCtx = rotCanvas.getContext('2d');
      rCtx.translate(rotCanvas.width / 2, rotCanvas.height / 2);
      rCtx.rotate((90 * Math.PI) / 180);
      rCtx.drawImage(img, -img.width / 2, -img.height / 2);

      rotCanvas.toBlob((blob) => {
        if (!blob) return;
        const newFile = new File([blob], selectedFile?.name || 'rotated.jpg', { type: 'image/jpeg' });
        setSelectedFile(newFile);
        const newUrl = URL.createObjectURL(blob);
        setPreviewUrl(newUrl);
        setDetectionResult(null);
        setZoom(1.0);
        setHoveredIdx(null);
      }, 'image/jpeg', 0.95);
    };
  };

  const handleDownloadAnnotated = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const link = document.createElement('a');
    link.download = `scansnap_detection_${Date.now()}.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  };

  const runDetection = async () => {
    if (!selectedFile) return;

    try {
      setIsDetecting(true);
      const res = await detectObjectsInImage(selectedFile, 0.20);
      setDetectionResult(res);
      drawDetections(res, confidenceThreshold, null, showLabels);
    } catch (err) {
      console.error(err);
      alert(err.message || 'Detection failed. Please check backend server status.');
    } finally {
      setIsDetecting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Studio Header Card */}
      <div 
        className="neu-box"
        style={{
          background: 'linear-gradient(135deg, #F3E8FF 0%, #EDE9FE 100%)',
          padding: '24px 28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <div>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', background: '#8B5CF6', color: '#FFF', padding: '4px 10px', borderRadius: '6px', fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '8px' }}>
            <Sparkles size={12} color="#FFF" /> YOLOv11 Computer Vision Engine
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: 900, color: '#0A0A0A' }}>
            Visual AI Studio & Test Bench
          </h2>
          <p style={{ fontSize: '13px', color: '#4C1D95', fontWeight: 600, marginTop: '4px' }}>
            Active Model: <code>best.pt</code> | Multi-Product Detection & Packshot Descriptor Verification
          </p>
        </div>

        {/* Tab switcher */}
        <div style={{ display: 'flex', gap: '8px', background: '#FFF', padding: '6px', border: 'var(--neu-border-sm)', borderRadius: '10px' }}>
          <button 
            onClick={() => setActiveTab('bench')}
            className={`neu-btn neu-btn-sm ${activeTab === 'bench' ? 'neu-btn-purple' : ''}`}
          >
            <Eye size={13} /> Test Bench
          </button>
          <button 
            onClick={() => setActiveTab('classes')}
            className={`neu-btn neu-btn-sm ${activeTab === 'classes' ? 'neu-btn-purple' : ''}`}
          >
            <Layers size={13} /> Model Classes
          </button>
        </div>
      </div>

      {activeTab === 'bench' && (
        <div style={{ display: 'grid', gridTemplateColumns: '380px 1fr', gap: '24px', alignItems: 'start' }}>
          {/* Left panel: Upload & Controls */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div className="neu-box" style={{ padding: '24px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 800, marginBottom: '14px' }}>
                1. Upload Image for Inference
              </h3>

              <div style={{ border: '2px dashed #0A0A0A', borderRadius: '10px', padding: '24px', textAlign: 'center', background: '#FFFDF7' }}>
                <ImageIcon size={36} style={{ margin: '0 auto 8px', color: '#8B5CF6' }} />
                <p style={{ fontSize: '13px', fontWeight: 700 }}>Upload Shelf or Product Image</p>
                <p style={{ fontSize: '11px', color: '#666', margin: '4px 0 14px' }}>Supports JPG, PNG (phones, webcam, counter frames)</p>

                <input 
                  type="file" 
                  accept="image/*"
                  id="test-image-input"
                  style={{ display: 'none' }}
                  onChange={handleFileSelect}
                />
                <label htmlFor="test-image-input" className="neu-btn neu-btn-sm neu-btn-purple" style={{ cursor: 'pointer' }}>
                  <Upload size={14} /> Select Image
                </label>
              </div>

              {selectedFile && (
                <div style={{ marginTop: '16px', padding: '12px', background: '#F3E8FF', border: '1.5px solid #0A0A0A', borderRadius: '8px', fontSize: '12px', fontWeight: 700 }}>
                  Selected: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
                </div>
              )}

              {/* Confidence Threshold Slider */}
              <div style={{ marginTop: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', fontWeight: 800, marginBottom: '6px' }}>
                  <span>Confidence Threshold:</span>
                  <span style={{ color: '#8B5CF6' }}>{(confidenceThreshold * 100).toFixed(0)}%</span>
                </div>
                <input 
                  type="range"
                  min="0.10"
                  max="0.95"
                  step="0.05"
                  value={confidenceThreshold}
                  onChange={(e) => handleThresholdChange(parseFloat(e.target.value))}
                  style={{ width: '100%', accentColor: '#8B5CF6', cursor: 'pointer' }}
                />
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '10px', color: '#666', fontWeight: 700, marginTop: '2px' }}>
                  <span>Sensitive (10%)</span>
                  <span>Strict (95%)</span>
                </div>
              </div>

              <button
                onClick={runDetection}
                disabled={!selectedFile || isDetecting}
                className="neu-btn neu-btn-primary"
                style={{ width: '100%', marginTop: '20px', padding: '12px' }}
              >
                {isDetecting ? (
                  <>
                    <RefreshCw size={16} className="spin-anim" />
                    <span>Running Vision Inference...</span>
                  </>
                ) : (
                  <>
                    <Sparkles size={16} />
                    <span>Detect Objects & Match SKUs</span>
                  </>
                )}
              </button>
            </div>

            {/* Inference stats card */}
            {detectionResult && (
              <div className="neu-box" style={{ padding: '20px', background: '#FFF' }}>
                <h4 style={{ fontSize: '14px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '12px' }}>
                  Detection Summary
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                    <span style={{ color: '#666' }}>Detected Items:</span>
                    <strong>
                      {(detectionResult.detections || []).filter(d => d.confidence >= confidenceThreshold).length} objects
                    </strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                    <span style={{ color: '#666' }}>Matched Products:</span>
                    <strong style={{ color: '#10B981' }}>
                      {(detectionResult.detections || []).filter(d => d.confidence >= confidenceThreshold && d.product_match).length} SKUs
                    </strong>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Right panel: Visual Canvas Display */}
          <div className="neu-box" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 800 }}>
                2. Visual Detection Canvas
              </h3>
              {detectionResult && (
                <span style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>
                  Showing objects with &ge; {(confidenceThreshold * 100).toFixed(0)}% confidence
                </span>
              )}
            </div>

            {/* Main Canvas Container */}
            <div style={{ 
              width: '100%',
              minHeight: '440px', 
              background: '#0B0F19', 
              border: '2px solid #0A0A0A', 
              borderRadius: '10px', 
              display: 'flex', 
              flexDirection: 'column',
              position: 'relative',
              overflow: 'hidden'
            }}>
              {/* Canvas Interactive Toolbar */}
              {previewUrl && (
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '8px 14px',
                  background: 'rgba(15, 23, 42, 0.95)',
                  borderBottom: '1px solid #1E293B',
                  flexWrap: 'wrap',
                  gap: '8px',
                  zIndex: 10
                }}>
                  {/* Zoom Controls */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span style={{ fontSize: '11px', fontWeight: 800, color: '#94A3B8', textTransform: 'uppercase', marginRight: '4px' }}>
                      Zoom:
                    </span>
                    <button 
                      onClick={() => setZoom(z => Math.max(0.4, Number((z - 0.2).toFixed(1))))}
                      className="neu-btn neu-btn-sm"
                      style={{ padding: '3px 8px', fontSize: '11px', background: '#1E293B', color: '#F8FAFC', border: '1px solid #334155' }}
                      title="Zoom Out"
                    >
                      <ZoomOut size={12} />
                    </button>
                    <span style={{ fontSize: '11px', fontWeight: 800, color: '#38BDF8', minWidth: '42px', textAlign: 'center' }}>
                      {Math.round(zoom * 100)}%
                    </span>
                    <button 
                      onClick={() => setZoom(z => Math.min(2.5, Number((z + 0.2).toFixed(1))))}
                      className="neu-btn neu-btn-sm"
                      style={{ padding: '3px 8px', fontSize: '11px', background: '#1E293B', color: '#F8FAFC', border: '1px solid #334155' }}
                      title="Zoom In"
                    >
                      <ZoomIn size={12} />
                    </button>
                    <button 
                      onClick={() => setZoom(1.0)}
                      className="neu-btn neu-btn-sm"
                      style={{ padding: '3px 8px', fontSize: '11px', background: '#1E293B', color: '#F8FAFC', border: '1px solid #334155' }}
                      title="Fit to Container"
                    >
                      Fit
                    </button>
                  </div>

                  {/* Actions */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <button 
                      onClick={handleRotateClockwise}
                      className="neu-btn neu-btn-sm"
                      style={{ padding: '4px 10px', fontSize: '11px', background: '#1E293B', color: '#F8FAFC', border: '1px solid #334155' }}
                      title="Rotate Image 90° Clockwise"
                    >
                      <RotateCw size={12} /> Rotate 90°
                    </button>
                    <button 
                      onClick={() => setShowLabels(v => !v)}
                      className="neu-btn neu-btn-sm"
                      style={{ 
                        padding: '4px 10px', 
                        fontSize: '11px', 
                        background: showLabels ? '#8B5CF6' : '#1E293B', 
                        color: '#FFF', 
                        border: '1px solid #334155' 
                      }}
                      title="Toggle Product Label Badges"
                    >
                      <Tag size={12} /> {showLabels ? 'Labels ON' : 'Labels OFF'}
                    </button>
                    <button 
                      onClick={handleDownloadAnnotated}
                      className="neu-btn neu-btn-sm"
                      style={{ padding: '4px 10px', fontSize: '11px', background: '#10B981', color: '#0A0A0A', border: '1px solid #059669', fontWeight: 800 }}
                      title="Save Annotated Image as PNG"
                    >
                      <Download size={12} /> Save PNG
                    </button>
                  </div>
                </div>
              )}

              {/* Viewport Area */}
              <div style={{ 
                flex: 1, 
                minHeight: '380px',
                maxHeight: '600px',
                overflow: 'auto',
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center',
                padding: '16px',
                background: 'radial-gradient(circle at center, #1E293B 0%, #0B0F19 100%)'
              }}>
                {!previewUrl ? (
                  <div style={{ textAlign: 'center', color: '#777', padding: '30px' }}>
                    <Box size={44} style={{ margin: '0 auto 12px', color: '#555' }} />
                    <p style={{ fontWeight: 700, color: '#AAA' }}>No image selected</p>
                    <p style={{ fontSize: '12px', color: '#666' }}>Select an image from the left panel to test vision detection</p>
                  </div>
                ) : (
                  <div style={{
                    transform: `scale(${zoom})`,
                    transformOrigin: 'center center',
                    transition: 'transform 0.15s ease-out',
                    maxWidth: '100%',
                    display: 'flex',
                    justifyContent: 'center'
                  }}>
                    <canvas 
                      ref={canvasRef} 
                      style={{ 
                        maxWidth: '100%', 
                        height: 'auto', 
                        display: 'block',
                        borderRadius: '6px',
                        boxShadow: '0 8px 30px rgba(0, 0, 0, 0.5)'
                      }} 
                    />
                  </div>
                )}
              </div>
            </div>

            {/* List of detected objects */}
            {detectionResult?.detections && (
              <div style={{ marginTop: '12px' }}>
                <h4 style={{ fontSize: '13px', fontWeight: 800, textTransform: 'uppercase', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span>Detected Objects Breakdown:</span>
                  <span style={{ fontSize: '11px', fontWeight: 600, color: '#666' }}>
                    (Hover any item to highlight on canvas)
                  </span>
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {detectionResult.detections
                    .filter(det => det.confidence >= confidenceThreshold)
                    .map((det, idx) => {
                      const itemColor = BOX_COLORS[idx % BOX_COLORS.length];
                      const isHovered = hoveredIdx === idx;
                      return (
                        <div 
                          key={idx}
                          onMouseEnter={() => setHoveredIdx(idx)}
                          onMouseLeave={() => setHoveredIdx(null)}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            padding: '12px 16px',
                            border: isHovered ? `2px solid ${itemColor}` : '1.5px solid #0A0A0A',
                            borderRadius: '8px',
                            background: isHovered ? '#F0FDF4' : '#FFFDF7',
                            boxShadow: isHovered ? `0 4px 12px ${hexToRgba(itemColor, 0.2)}` : 'none',
                            cursor: 'pointer',
                            transition: 'all 0.15s ease'
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            {/* Color Dot matching canvas box */}
                            <span 
                              style={{ 
                                width: '14px', 
                                height: '14px', 
                                borderRadius: '4px', 
                                background: itemColor,
                                border: '1px solid #0A0A0A',
                                flexShrink: 0
                              }} 
                            />
                            <div>
                              <div style={{ fontWeight: 800, fontSize: '13px', color: '#0A0A0A' }}>
                                {det.class_name || det.label}
                              </div>
                              {det.product_match?.category && (
                                <div style={{ fontSize: '11px', color: '#666', fontWeight: 600, marginTop: '2px' }}>
                                  {det.product_match.category} • Barcode: {det.product_match.barcode || 'N/A'}
                                </div>
                              )}
                            </div>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                            {/* Confidence Pill with mini bar */}
                            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '3px' }}>
                              <span 
                                style={{ 
                                  padding: '2px 8px', 
                                  borderRadius: '4px', 
                                  fontSize: '11px', 
                                  fontWeight: 800, 
                                  background: itemColor, 
                                  color: '#0A0A0A',
                                  border: '1px solid #0A0A0A'
                                }}
                              >
                                {(det.confidence * 100).toFixed(1)}% Confidence
                              </span>
                              <div style={{ width: '80px', height: '4px', background: '#E2E8F0', borderRadius: '2px', overflow: 'hidden' }}>
                                <div style={{ width: `${Math.min(100, Math.round(det.confidence * 100))}%`, height: '100%', background: itemColor }} />
                              </div>
                            </div>

                            {/* Price */}
                            {det.product_match && (
                              <span style={{ fontWeight: 900, fontFamily: 'var(--font-heading)', fontSize: '16px', minWidth: '60px', textAlign: 'right' }}>
                                ₹{det.product_match.price}
                              </span>
                            )}
                          </div>
                        </div>
                      );
                    })}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === 'classes' && (
        <div className="neu-box" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
            <div>
              <h3 style={{ fontSize: '18px', fontWeight: 800 }}>Trained Vision Classes in best.pt</h3>
              <p style={{ fontSize: '13px', color: '#666' }}>Active classes recognized by the retail object detection pipeline</p>
            </div>
            <span className="neu-badge neu-badge-purple">{trainedClasses.length} Multi-Product Classes</span>
          </div>

          <div className="neu-table-container">
            <table className="neu-table">
              <thead>
                <tr>
                  <th>Class ID</th>
                  <th>Product Class Name</th>
                  <th>Category</th>
                  <th>Training Samples</th>
                  <th>Validation mAP50</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {trainedClasses.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <code style={{ background: '#F3F4F6', padding: '3px 8px', borderRadius: '4px', border: '1px solid #0A0A0A', fontWeight: 800 }}>
                        {c.id}
                      </code>
                    </td>
                    <td style={{ fontWeight: 800 }}>{c.name}</td>
                    <td>
                      <span className="neu-badge neu-badge-blue">{c.category}</span>
                    </td>
                    <td style={{ fontWeight: 600 }}>{c.count}</td>
                    <td style={{ fontWeight: 800, color: '#059669' }}>{c.accuracy}</td>
                    <td>
                      <span className="neu-badge neu-badge-green">
                        <Check size={10} /> Active in best.pt
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

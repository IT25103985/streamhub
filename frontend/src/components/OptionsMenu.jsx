import React, { useState, useEffect, useRef } from 'react';
import './OptionsMenu.css';

//dropdown menu component for action items and popup triggers
const OptionsMenu = ({ items = [], onToggle }) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        if (isOpen) {
          setIsOpen(false);
          if (onToggle) onToggle(false);
        }
      }
    };
    
    const handleEscape = (e) => {
      if (e.key === 'Escape' && isOpen) {
        setIsOpen(false);
        if (onToggle) onToggle(false);
      }
    };
    
    document.addEventListener('mousedown', handleClickOutside);
    window.addEventListener('keydown', handleEscape);
    
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen, onToggle]);

  if (!items || items.length === 0) return null;

  return (
    <div className="options-menu-container" ref={dropdownRef}>
      <button 
        className="options-btn"
        onClick={(e) => {
          e.stopPropagation();
          e.preventDefault();
          const newIsOpen = !isOpen;
          setIsOpen(newIsOpen);
          if (onToggle) onToggle(newIsOpen);
        }}
        aria-label="Options"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="5" r="1"></circle>
          <circle cx="12" cy="12" r="1"></circle>
          <circle cx="12" cy="19" r="1"></circle>
        </svg>
      </button>

      {isOpen && (
        <div className="options-menu">
          {items.map((item, index) => (
            <button 
              key={index} 
              className={`options-menu-item ${item.className || ''}`}
              style={{ display: 'flex', alignItems: 'center', gap: '10px', ...item.style }}
              onClick={(e) => {
                e.stopPropagation();
                e.preventDefault();
                setIsOpen(false);
                if (onToggle) onToggle(false);
                if (item.onClick) item.onClick(e);
              }}
            >
              {item.icon && <span className="options-menu-icon" style={{ display: 'flex', alignItems: 'center', opacity: 0.9 }}>{item.icon}</span>}
              <span>{item.label}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

export default OptionsMenu;

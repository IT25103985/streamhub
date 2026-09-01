import React from 'react';
import './Loader.css';

//compact spinner loader for inline button states and busy actions
const Loader = ({ size = 20, color = 'currentColor', className = '' }) => {
  return (
    <div 
      className={`inline-spinner ${className}`} 
      style={{ 
        width: `${size}px`, 
        height: `${size}px`, 
        borderColor: 'rgba(255, 255, 255, 0.2)',
        borderTopColor: color 
      }} 
      role="status"
      aria-label="Loading"
    />
  );
};

export default Loader;

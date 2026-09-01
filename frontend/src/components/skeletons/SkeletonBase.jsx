import React from 'react';
import './Skeleton.css';

//shared shimmer building block with customizable dimensions
const SkeletonBase = ({ 
  width = '100%', 
  height = '16px', 
  borderRadius, 
  className = '', 
  style = {} 
}) => {
  const inlineStyle = {
    width: typeof width === 'number' ? `${width}px` : width,
    height: typeof height === 'number' ? `${height}px` : height,
    ...(borderRadius ? { borderRadius: typeof borderRadius === 'number' ? `${borderRadius}px` : borderRadius } : {}),
    ...style
  };

  return (
    <div 
      className={`skeleton-base ${className}`} 
      style={inlineStyle} 
      aria-hidden="true" 
    />
  );
};

export default SkeletonBase;

import React from 'react';
import SkeletonBase from './SkeletonBase';
import './Skeleton.css';

//skeleton placeholder for notification list entries
const NotificationItemSkeleton = () => {
  return (
    <div className="notification-item-skeleton" aria-hidden="true">
      <SkeletonBase width="8px" height="8px" borderRadius="50%" style={{ flexShrink: 0 }} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '6px' }}>
        <SkeletonBase width="65%" height="13px" />
        <SkeletonBase width="90%" height="12px" />
        <SkeletonBase width="40px" height="10px" style={{ marginTop: '2px' }} />
      </div>
    </div>
  );
};

export default NotificationItemSkeleton;

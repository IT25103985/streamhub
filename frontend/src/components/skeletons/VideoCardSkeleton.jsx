import React from 'react';
import SkeletonBase from './SkeletonBase';
import './Skeleton.css';

//skeleton placeholder matching the layout and dimensions of videocard
const VideoCardSkeleton = () => {
  return (
    <div className="video-card-skeleton" aria-hidden="true">
      <div className="skeleton-thumb">
        <SkeletonBase width="100%" height="100%" borderRadius={0} />
      </div>
      <div className="skeleton-body">
        <div style={{ marginBottom: '6px' }}>
          <SkeletonBase width="90%" height="14px" style={{ marginBottom: '5px' }} />
          <SkeletonBase width="60%" height="14px" />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px', height: '20px', marginBottom: '6px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <SkeletonBase width="24px" height="24px" borderRadius="50%" />
            <SkeletonBase width="75px" height="12px" />
          </div>
          <SkeletonBase width="45px" height="12px" />
        </div>
        <div style={{ display: 'flex', gap: '6px' }}>
          <SkeletonBase width="55px" height="20px" borderRadius="999px" />
        </div>
      </div>
    </div>
  );
};

export default VideoCardSkeleton;

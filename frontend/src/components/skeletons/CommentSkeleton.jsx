import React from 'react';
import SkeletonBase from './SkeletonBase';
import './Skeleton.css';

//skeleton placeholder for comment rows and reply threads
const CommentSkeleton = () => {
  return (
    <div className="comment-skeleton" aria-hidden="true">
      <SkeletonBase width="36px" height="36px" borderRadius="50%" style={{ flexShrink: 0 }} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <SkeletonBase width="110px" height="13px" />
          <SkeletonBase width="60px" height="11px" />
        </div>
        <SkeletonBase width="95%" height="13px" />
        <SkeletonBase width="70%" height="13px" />
        <div style={{ display: 'flex', gap: '12px', marginTop: '4px' }}>
          <SkeletonBase width="45px" height="16px" borderRadius="4px" />
          <SkeletonBase width="55px" height="16px" borderRadius="4px" />
        </div>
      </div>
    </div>
  );
};

export default CommentSkeleton;

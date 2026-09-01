import React from 'react';
import SkeletonBase from './SkeletonBase';
import '../../features/profile/Profile.css';

//skeleton placeholder matching profileheader cover avatar and bio details
const ProfileHeaderSkeleton = () => {
  return (
    <div className="profile-header-container" aria-hidden="true">
      <div className="profile-cover" style={{ background: 'none', padding: 0 }}>
        <SkeletonBase width="100%" height="100%" borderRadius={0} />
      </div>
      
      <div className="profile-info-section">
        <div className="profile-avatar-wrapper" style={{ border: 'none', background: 'transparent' }}>
          <SkeletonBase width="100%" height="100%" borderRadius="50%" />
        </div>
        
        <div className="profile-details">
          <div className="profile-details-top">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <SkeletonBase width="180px" height="28px" />
              <SkeletonBase width="130px" height="14px" />
            </div>
            <SkeletonBase width="100px" height="36px" borderRadius="8px" />
          </div>
          
          <div style={{ marginTop: '12px' }}>
            <SkeletonBase width="85%" height="14px" style={{ marginBottom: '6px' }} />
            <SkeletonBase width="55%" height="14px" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfileHeaderSkeleton;

import React from 'react';
import { getFullImageUrl } from '../../utils/imageUtils';
import './Profile.css';

//profile hero section with cover image avatar and stats
const ProfileHeader = ({ profile, isOwner = true, onEditClick, onReportClick }) => {
      const coverImage = getFullImageUrl(profile?.coverImageUrl) || 'https://via.placeholder.com/1200x300/333333/666666?text=Cover+Image';
    const avatar = getFullImageUrl(profile?.avatarUrl) || 'https://via.placeholder.com/150/444444/888888?text=Avatar';

    return (
        <div className="profile-header-container">
            <div 
                className="profile-cover" 
                style={{ backgroundImage: `url(${coverImage})` }}
            >
                
            </div>
            
            <div className="profile-info-section">
                <div className="profile-avatar-wrapper">
                    <img src={avatar} alt="Avatar" className="profile-avatar-img" />
                </div>
                
                <div className="profile-details">
                    <div className="profile-details-top">
                        <div>
                            <h1 className="profile-name">{profile?.firstName} {profile?.lastName}</h1>
                            {profile?.email && <p className="profile-email">{profile.email}</p>}
                        </div>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            {isOwner ? (
                                <button className="btn btn-primary edit-profile-btn" onClick={onEditClick}>
                                    Edit Profile
                                </button>
                            ) : (
                                <button className="btn btn-secondary report-profile-btn" onClick={onReportClick}>
                                    Report
                                </button>
                            )}
                        </div>
                    </div>
                    
                    {profile?.bio && (
                        <p className="profile-bio">{profile.bio}</p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ProfileHeader;

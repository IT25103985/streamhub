import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import { getFullImageUrl } from '../../utils/imageUtils';
import { getFollowCounts } from '../../api/followApi';
import websocketClient from '../../api/websocketClient';
import FollowButton from '../../components/FollowButton';
import FollowListModal from '../follow/FollowListModal';
import './Profile.css';

//profile hero section with cover image avatar stats and follow integration
const ProfileHeader = ({ profile, userId, isOwner = true, onEditClick, onReportClick }) => {
    const { user } = useContext(AuthContext);
    const [counts, setCounts] = useState({
        followerCount: 0,
        followingCount: 0,
        isFollowing: false
    });
    const [showFollowersModal, setShowFollowersModal] = useState(false);
    const [showFollowingModal, setShowFollowingModal] = useState(false);

    // Fetch initial follow counts
    useEffect(() => {
        if (!userId) return;
        let isMounted = true;

        getFollowCounts(userId)
            .then(res => {
                if (isMounted && res) {
                    setCounts({
                        followerCount: Number(res.followerCount) || 0,
                        followingCount: Number(res.followingCount) || 0,
                        isFollowing: Boolean(res.isFollowing)
                    });
                }
            })
            .catch(err => {
                console.error("Failed to fetch follow counts:", err);
            });

        return () => {
            isMounted = false;
        };
    }, [userId]);

    // STOMP WebSocket live count subscription
    useEffect(() => {
        if (!userId) return;

        const sub = websocketClient.subscribe('/user/queue/follow-counts', (payload) => {
            if (payload && payload.userId === userId) {
                setCounts(prev => ({
                    ...prev,
                    followerCount: typeof payload.followerCount === 'number' ? payload.followerCount : prev.followerCount,
                    followingCount: typeof payload.followingCount === 'number' ? payload.followingCount : prev.followingCount,
                    isFollowing: typeof payload.isFollowing === 'boolean' ? payload.isFollowing : prev.isFollowing
                }));
            }
        });

        return () => {
            if (sub && typeof sub.unsubscribe === 'function') {
                sub.unsubscribe();
            }
        };
    }, [userId]);

    const coverImage = getFullImageUrl(profile?.coverImageUrl) || 'https://via.placeholder.com/1200x300/333333/666666?text=Cover+Image';
    const avatar = getFullImageUrl(profile?.avatarUrl) || 'https://via.placeholder.com/150/444444/888888?text=Avatar';

    return (
        <div className="profile-header-container">
            <div 
                className="profile-cover" 
                style={{ backgroundImage: `url(${coverImage})` }}
            />
            
            <div className="profile-info-section">
                <div className="profile-avatar-wrapper">
                    <img src={avatar} alt="Avatar" className="profile-avatar-img" />
                </div>
                
                <div className="profile-details">
                    <div className="profile-details-top">
                        <div>
                            <h1 className="profile-name">{profile?.firstName} {profile?.lastName}</h1>
                            {profile?.email && <p className="profile-email">{profile.email}</p>}
                            
                            <div className="profile-stats-row">
                                {isOwner ? (
                                    <>
                                        <button 
                                            type="button" 
                                            className="profile-stat-btn" 
                                            onClick={() => setShowFollowersModal(true)}
                                            title="View followers"
                                        >
                                            <strong>{counts.followerCount}</strong> followers
                                        </button>
                                        <button 
                                            type="button" 
                                            className="profile-stat-btn" 
                                            onClick={() => setShowFollowingModal(true)}
                                            title="View following"
                                        >
                                            <strong>{counts.followingCount}</strong> following
                                        </button>
                                    </>
                                ) : (
                                    <>
                                        <span className="profile-stat-text">
                                            <strong>{counts.followerCount}</strong> followers
                                        </span>
                                        <span className="profile-stat-text">
                                            <strong>{counts.followingCount}</strong> following
                                        </span>
                                    </>
                                )}
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            {!isOwner && user && userId && (
                                <FollowButton 
                                    targetUserId={userId}
                                    initialIsFollowing={counts.isFollowing}
                                    onFollowChange={(newIsFollowing) => {
                                        setCounts(prev => ({
                                            ...prev,
                                            isFollowing: newIsFollowing,
                                            followerCount: newIsFollowing ? prev.followerCount + 1 : Math.max(0, prev.followerCount - 1)
                                        }));
                                    }}
                                />
                            )}

                            {isOwner ? (
                                <button className="btn btn-primary edit-profile-btn" onClick={onEditClick}>
                                    Edit Profile
                                </button>
                            ) : onReportClick ? (
                                <button className="btn btn-secondary report-profile-btn" onClick={onReportClick}>
                                    Report
                                </button>
                            ) : null}
                        </div>
                    </div>
                    
                    {profile?.bio && (
                        <p className="profile-bio">{profile.bio}</p>
                    )}
                </div>
            </div>

            {isOwner && showFollowersModal && (
                <FollowListModal 
                    mode="followers"
                    userId={userId}
                    onClose={() => setShowFollowersModal(false)}
                />
            )}

            {isOwner && showFollowingModal && (
                <FollowListModal 
                    mode="following"
                    userId={userId}
                    onClose={() => setShowFollowingModal(false)}
                    onUnfollowSuccess={() => {
                        setCounts(prev => ({
                            ...prev,
                            followingCount: Math.max(0, prev.followingCount - 1)
                        }));
                    }}
                />
            )}
        </div>
    );
};

export default ProfileHeader;

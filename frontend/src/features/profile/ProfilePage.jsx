import React, { useState, useEffect, useContext } from 'react';
import { Navigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import { profileService } from './profileService';
import ProfileHeader from './ProfileHeader';
import ProfileHeaderSkeleton from '../../components/skeletons/ProfileHeaderSkeleton';
import VideoGridSkeleton from '../../components/skeletons/VideoGridSkeleton';
import EditProfileModal from './EditProfileModal';
import ProfileVideosTab from './ProfileVideosTab';
import ProfilePlaylistsTab from './ProfilePlaylistsTab';
import './Profile.css';

//main user profile page displaying tabs for uploads and playlists
const ProfilePage = () => {
    const { user } = useContext(AuthContext);
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState('home');
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearch(search);
        }, 300);
        return () => clearTimeout(timer);
    }, [search]);

    const fetchProfile = async () => {
        try {
            setLoading(true);
            const data = await profileService.getMyProfile();
            setProfile(data);
            setError('');
        } catch (err) {
            console.error("Failed to fetch profile", err);
            setError("Failed to load profile. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (user) {
            fetchProfile();
        }
    }, [user]);

    const handleSaveSuccess = () => {
        setIsEditModalOpen(false);
        fetchProfile();
    };

    if (!user) {
        return <Navigate to="/" replace />;
    }

    if (loading) {
        return (
            <div className="page-container" style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
                <ProfileHeaderSkeleton />
                <VideoGridSkeleton count={4} />
            </div>
        );
    }

    if (error) {
        return (
            <div className="page-container" style={{ padding: '40px 24px', maxWidth: '600px', margin: '40px auto', textAlign: 'center', background: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)' }}>
                <p style={{ color: '#ef4444', fontSize: '15px', marginBottom: '16px' }}>{error}</p>
                <button className="btn btn-primary" onClick={fetchProfile}>
                    Try Again
                </button>
            </div>
        );
    }

    return (
        <div className="page-container" style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
            <ProfileHeader 
                profile={profile} 
                onEditClick={() => setIsEditModalOpen(true)} 
            />

            <div className="profile-tabs-section">
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
                    {profile?.uploadedVideoCount || 0} videos uploaded
                </div>
                <div className="profile-tabs-header">
                    <div className="profile-tabs-list">
                        <button 
                            className={`profile-tab-btn ${activeTab === 'home' ? 'active' : ''}`}
                            onClick={() => setActiveTab('home')}
                        >
                            Home
                        </button>
                        <button 
                            className={`profile-tab-btn ${activeTab === 'playlists' ? 'active' : ''}`}
                            onClick={() => setActiveTab('playlists')}
                        >
                            Playlists
                        </button>
                    </div>
                    <input 
                        type="text" 
                        placeholder={activeTab === 'playlists' ? "Search my playlists..." : "Search my videos..."}
                        className="profile-search-bar"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                </div>
                
                <div style={{ marginTop: '24px' }}>
                    {activeTab === 'home' && (
                        <ProfileVideosTab 
                            onVideoCountChange={(newCount) => setProfile(prev => ({...prev, uploadedVideoCount: newCount}))}
                            currentCount={profile?.uploadedVideoCount || 0}
                            search={debouncedSearch}
                        />
                    )}
                    {activeTab === 'playlists' && <ProfilePlaylistsTab search={debouncedSearch} />}
                </div>
            </div>

            {isEditModalOpen && (
                <EditProfileModal 
                    profile={profile} 
                    onClose={() => setIsEditModalOpen(false)}
                    onSaveSuccess={handleSaveSuccess}
                />
            )}
        </div>
    );
};

export default ProfilePage;

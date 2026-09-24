import { useEffect, useState, useContext } from 'react';
import { useParams } from 'react-router-dom';
import axiosInstance from '../../api/axiosInstance';
import ProfileHeader from './ProfileHeader';
import ProfileHeaderSkeleton from '../../components/skeletons/ProfileHeaderSkeleton';
import VideoGridSkeleton from '../../components/skeletons/VideoGridSkeleton';
import ProfileVideosTab from './ProfileVideosTab';
import ProfilePlaylistsTab from './ProfilePlaylistsTab';
import ReportModal from '../report/ReportModal';
import { AuthContext, useRequireAuth } from '../../context/AuthContext';
import './Profile.css';

//public channel view for browsing any user public videos and playlists
const PublicProfilePage = () => {
  const { userId } = useParams();
  const { user } = useContext(AuthContext);
  const requireAuth = useRequireAuth();
  
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [activeTab, setActiveTab] = useState('home');
  const [showReportModal, setShowReportModal] = useState(false);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  useEffect(() => {
      const timer = setTimeout(() => {
          setDebouncedSearch(search);
      }, 300);
      return () => clearTimeout(timer);
  }, [search]);

  const isOwner = Boolean(user && user.userId && userId && String(user.userId).toLowerCase() === String(userId).toLowerCase());

  const fetchProfile = () => {
    setLoading(true);
    setError(null);
    axiosInstance.get(`/users/${userId}/profile`)
      .then(res => {
        setProfile(res.data);
        setError(null);
      })
      .catch(err => {
        console.error("Failed to load public profile:", err);
        setError("User not found or failed to load profile.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchProfile();
  }, [userId]);

  const handleReportClick = () => {
    requireAuth(() => {
      setShowReportModal(true);
    });
  };

  if (loading) {
    return (
      <div className="page-container" style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
        <ProfileHeaderSkeleton />
        <VideoGridSkeleton count={4} />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="page-container" style={{ padding: '40px 24px', maxWidth: '600px', margin: '40px auto', textAlign: 'center', background: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)' }}>
        <p style={{ color: '#ef4444', fontSize: '15px', marginBottom: '16px' }}>{error || "User not found"}</p>
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
        userId={userId}
        isOwner={isOwner}
        onReportClick={!isOwner ? handleReportClick : undefined}
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
              placeholder={activeTab === 'playlists' ? "Search playlists..." : "Search videos..."}
              className="profile-search-bar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        
        <div style={{ marginTop: '24px' }}>
          {activeTab === 'home' && <ProfileVideosTab uploaderId={userId} search={debouncedSearch} />}
          {activeTab === 'playlists' && <ProfilePlaylistsTab ownerId={userId} search={debouncedSearch} />}
        </div>
      </div>

      {showReportModal && (
        <ReportModal 
          reportableType="PROFILE"
          reportableId={userId}
          onClose={() => setShowReportModal(false)}
          onSuccess={() => setShowReportModal(false)}
        />
      )}
    </div>
  );
};

export default PublicProfilePage;

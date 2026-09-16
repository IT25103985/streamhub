import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axiosInstance from '../../api/axiosInstance';
import ProfileHeader from './ProfileHeader';
import ProfileHeaderSkeleton from '../../components/skeletons/ProfileHeaderSkeleton';
import VideoGridSkeleton from '../../components/skeletons/VideoGridSkeleton';
import ProfileVideosTab from './ProfileVideosTab';
import ProfilePlaylistsTab from './ProfilePlaylistsTab';
import ReportModal from '../report/ReportModal';
import { useRequireAuth } from '../../context/AuthContext';

//public channel view for browsing any user public videos and playlists
const PublicProfilePage = () => {
  const { userId } = useParams();
  const requireAuth = useRequireAuth();
  
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [activeTab, setActiveTab] = useState('home');
  const [showReportModal, setShowReportModal] = useState(false);

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
      <div className="profile-page-container">
        <main className="profile-main-content">
          <ProfileHeaderSkeleton />
          <VideoGridSkeleton count={4} />
        </main>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="profile-page-container">
        <main className="profile-main-content">
          <div style={{ padding: '4rem 2rem', textAlign: 'center', background: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)', maxWidth: '500px', margin: '40px auto' }}>
            <p style={{ color: '#ef4444', fontSize: '15px', marginBottom: '16px' }}>{error || "User not found"}</p>
            <button className="btn btn-primary" onClick={fetchProfile}>
              Try Again
            </button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="profile-page-container">
      <main className="profile-main-content">
        <ProfileHeader 
          profile={profile} 
          isOwner={Boolean(user && user.userId === userId)}
          onReportClick={(!user || user.userId !== userId) ? handleReportClick : undefined}
        />
        
        <div className="profile-tabs-section">
          <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
            {profile?.uploadedVideoCount || 0} videos uploaded
          </div>
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
          
          <div className="profile-tab-content" style={{ marginTop: '24px' }}>
            {activeTab === 'home' && <ProfileVideosTab uploaderId={userId} />}
            {activeTab === 'playlists' && <ProfilePlaylistsTab ownerId={userId} />}
          </div>
        </div>
      </main>

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

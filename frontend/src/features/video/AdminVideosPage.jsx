import React, { useState, useEffect } from 'react';
import axiosInstance from '../../api/axiosInstance';
import AdminDataTable from '../../components/AdminDataTable';
import OptionsMenu from '../../components/OptionsMenu';
import VideoWatchPanel from './VideoWatchPanel';
import ModerateDeleteModal from './ModerateDeleteModal';
import { getFullImageUrl } from '../../utils/imageUtils';

const ICONS = {
  play: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polygon points="5 3 19 12 5 21 5 3" />
    </svg>
  ),
  trash: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
      <line x1="10" y1="11" x2="10" y2="17" />
      <line x1="14" y1="11" x2="14" y2="17" />
    </svg>
  )
};

//admin video catalog with moderation removal and filtering tools
const AdminVideosPage = () => {
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeVideoId, setActiveVideoId] = useState(null);
  const [modDeleteTargetVideo, setModDeleteTargetVideo] = useState(null);

  const fetchVideos = async (isBackground = false) => {
    try {
      if (!isBackground) setLoading(true);
      const response = await axiosInstance.get('/videos?includeHidden=true&size=100');
      const videoList = response.data?.content || (Array.isArray(response.data) ? response.data : []);
      setVideos(videoList);
      setError('');
    } catch (err) {
      console.error('Failed to fetch videos:', err);
      if (!isBackground) setError('Failed to load videos. Please try again later.');
    } finally {
      if (!isBackground) setLoading(false);
    }
  };

  useEffect(() => {
    fetchVideos(false);

    const handleUpdate = () => fetchVideos(true);
    window.addEventListener('streamhub-video-update', handleUpdate);
    window.addEventListener('focus', handleUpdate);
    document.addEventListener('visibilitychange', handleUpdate);

    const interval = setInterval(() => fetchVideos(true), 10000);

    //admin video catalog table section
  return () => {
      window.removeEventListener('streamhub-video-update', handleUpdate);
      window.removeEventListener('focus', handleUpdate);
      document.removeEventListener('visibilitychange', handleUpdate);
      clearInterval(interval);
    };
  }, []);

  const handleToggleHide = async (videoId) => {
    try {
      await axiosInstance.patch(`/videos/${videoId}/toggle-hide`);
      fetchVideos(true);
      window.dispatchEvent(new CustomEvent('streamhub-video-update'));
    } catch (err) {
      console.error('Failed to toggle video visibility:', err);
      alert('Failed to toggle video visibility.');
    }
  };

  const handleModerateDeleteSuccess = (deletedVideoId) => {
    setVideos(prev => prev.filter(v => (v.videoId || v.id) !== deletedVideoId));
    setModDeleteTargetVideo(null);
  };

  const columns = [
    {
      label: 'Thumbnail',
      key: 'thumbnail',
      render: (row) => {
        const videoId = row.videoId || row.id;
        return (
          <div 
            style={{ width: '80px', height: '45px', backgroundColor: '#1e293b', borderRadius: '6px', overflow: 'hidden', cursor: 'pointer', flexShrink: 0 }}
            onClick={() => setActiveVideoId(videoId)}
          >
            {row.thumbnailUrl ? (
              <img 
                src={getFullImageUrl(row.thumbnailUrl)} 
                alt={row.title} 
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            ) : (
              <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#64748b', fontSize: '10px' }}>
                No Thumb
              </div>
            )}
          </div>
        );
      }
    },
    { 
      label: 'Title', 
      key: 'title',
      render: (row) => {
        const videoId = row.videoId || row.id;
        return (
          <span 
            style={{ fontWeight: 600, color: 'var(--text)', cursor: 'pointer' }}
            onClick={() => setActiveVideoId(videoId)}
          >
            {row.title || 'Untitled'}
          </span>
        );
      }
    },
    { 
      label: 'Uploader', 
      key: 'uploaderName',
      render: (row) => (
        <span>{row.uploaderName || row.uploaderUsername || row.ownerUsername || row.creator || 'Unknown'}</span>
      )
    },
    { 
      label: 'Category', 
      key: 'category',
      render: (row) => {
        const catName = row.category?.name || (typeof row.category === 'string' ? row.category : 'Uncategorized');
        return (
          <span style={{ 
            padding: '3px 8px', 
            borderRadius: '4px', 
            fontSize: '11px', 
            fontWeight: 500,
            backgroundColor: 'rgba(59, 130, 246, 0.12)', 
            color: '#60a5fa' 
          }}>
            {catName}
          </span>
        );
      }
    },
    { 
      label: 'Status', 
      key: 'isHidden',
      render: (row) => (
        <span style={{
          padding: '3px 8px',
          borderRadius: '4px',
          fontSize: '11px',
          fontWeight: 600,
          backgroundColor: row.isHidden ? 'rgba(239, 68, 68, 0.15)' : 'rgba(16, 185, 129, 0.15)',
          color: row.isHidden ? '#ef4444' : '#34d399',
          border: `1px solid ${row.isHidden ? 'rgba(239, 68, 68, 0.3)' : 'rgba(16, 185, 129, 0.3)'}`
        }}>
          {row.isHidden ? 'Hidden' : 'Visible'}
        </span>
      )
    },
    {
      label: 'Views',
      key: 'viewCount',
      render: (row) => (
        <span style={{ fontFamily: 'monospace', fontSize: '13px', color: 'var(--text-dim)' }}>
          {row.viewCount ?? 0}
        </span>
      )
    },
    { 
      label: 'Upload Date', 
      key: 'createdAt',
      render: (row) => (
        <span style={{ color: 'var(--text-faint)', fontSize: '13px' }}>
          {row.createdAt ? new Date(row.createdAt).toLocaleDateString() : 'N/A'}
        </span>
      )
    }
  ];

  const actions = (row) => {
    const videoId = row.videoId || row.id;
    return (
      <div className="admin-table-actions">
        <OptionsMenu 
          items={[
            {
              label: 'Watch Video',
              icon: ICONS.play,
              onClick: () => setActiveVideoId(videoId)
            },
            {
              label: row.isHidden ? 'Unhide Video' : 'Hide Video',
              onClick: () => handleToggleHide(videoId)
            },
            {
              label: 'Delete (Moderate)',
              icon: ICONS.trash,
              className: 'danger',
              onClick: () => setModDeleteTargetVideo(row)
            }
          ]}
        />
      </div>
    );
  };

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <h2>Video Moderation</h2>
        <p>Manage platform videos, review content, and take moderation actions with reasoned notifications.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <AdminDataTable 
        columns={columns} 
        data={videos} 
        actions={actions} 
        loading={loading}
      />

      {activeVideoId && (
        <VideoWatchPanel 
          videoId={activeVideoId} 
          onClose={() => setActiveVideoId(null)} 
        />
      )}

      {modDeleteTargetVideo && (
        <ModerateDeleteModal 
          video={modDeleteTargetVideo}
          onClose={() => setModDeleteTargetVideo(null)}
          onSuccess={handleModerateDeleteSuccess}
        />
      )}
    </div>
  );
};

export default AdminVideosPage;

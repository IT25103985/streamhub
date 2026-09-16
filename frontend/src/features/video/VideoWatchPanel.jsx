import React, { useEffect, useState, useRef, useContext } from 'react';
import { Link } from 'react-router-dom';
import axiosInstance from '../../api/axiosInstance';
import watchHistoryService from '../watchhistory/watchHistoryService';
import VideoPlayer from './VideoPlayer';
import CommentList from '../social/CommentList';
import LikeButton from '../social/LikeButton';
import OptionsMenu from '../../components/OptionsMenu';
import ReportModal from '../report/ReportModal';
import AddToPlaylistMenu from '../playlist/AddToPlaylistMenu';
import { AuthContext, useRequireAuth } from '../../context/AuthContext';
import { getFullImageUrl } from '../../utils/imageUtils';
import Navbar from '../../components/Navbar';
import './VideoWatchPanel.css';

//full watch modal with video player details and comments section
const VideoWatchPanel = ({ videoId, onClose }) => {
  const { user } = useContext(AuthContext);
  const requireAuth = useRequireAuth();
  const [showReportModal, setShowReportModal] = useState(false);
  const [showPlaylistMenu, setShowPlaylistMenu] = useState(false);
  const [video, setVideo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [initialPosition, setInitialPosition] = useState(0);
  const [isDescriptionExpanded, setIsDescriptionExpanded] = useState(false);
  
  const panelRef = useRef(null);
  const hasCountedView = useRef(false);
  const currentPosRef = useRef(0);
  const heartbeatIntervalRef = useRef(null);

  const creatorName = video ? (
    (video.uploaderName && video.uploaderName.trim() !== '' && video.uploaderName.trim() !== 'null null') 
      ? video.uploaderName 
      : (video.ownerUsername || video.creator || 'Unknown')
  ) : 'Unknown';

  //save current playback position on unmount
  const saveProgress = (pos) => {
    if (!videoId) return;
    const targetPos = Math.max(0, Math.floor(typeof pos === 'number' ? pos : (currentPosRef.current || 0)));
    watchHistoryService.recordWatchHistory(videoId, targetPos)
      .then(() => {
        window.dispatchEvent(new CustomEvent('streamhub-video-update', { detail: { videoId } }));
      })
      .catch((err) => {
        console.warn('Watch history record skipped (guest or unauthenticated):', err);
      });
  };

  useEffect(() => {
    if (!videoId) return;
    hasCountedView.current = false;
    currentPosRef.current = 0;
    
    //lock background page scrolling while modal is open
    document.body.style.overflow = 'hidden';
    
    setLoading(true);
    
    //fetch video metadata and last saved timestamp concurrently
    Promise.all([
      axiosInstance.get(`/videos/${videoId}`).then(res => res.data).catch(() => null),
      watchHistoryService.getResumePosition(videoId).catch(() => 0)
    ]).then(([videoData, resumePos]) => {
      if (videoData) {
        setVideo(videoData);
      }
      if (resumePos && resumePos > 0) {
        setInitialPosition(resumePos);
        currentPosRef.current = resumePos;
      }
    }).finally(() => {
      setLoading(false);
    });

    //video watch panel stage section
  return () => {
      document.body.style.overflow = 'unset';
      if (heartbeatIntervalRef.current) {
        clearInterval(heartbeatIntervalRef.current);
      }
      if (hasCountedView.current) {
        saveProgress(currentPosRef.current);
      }
    };
  }, [videoId]);

  const handlePlay = (time) => {
    const playTime = typeof time === 'number' ? time : currentPosRef.current;
    currentPosRef.current = playTime;
    
    //record view count and watch history entry on playback start
    if (!hasCountedView.current) {
      hasCountedView.current = true;
      watchHistoryService.incrementViewCount(videoId)
        .then(() => {
          setVideo(prev => prev ? { ...prev, viewCount: (prev.viewCount || 0) + 1 } : prev);
          window.dispatchEvent(new CustomEvent('streamhub-video-update', { detail: { videoId } }));
        })
        .catch(() => {});
      
      saveProgress(playTime);
    }

    //periodic heartbeat to save watch position
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }
    heartbeatIntervalRef.current = setInterval(() => {
      saveProgress(currentPosRef.current);
    }, 15000);
  };

  const handlePause = (time) => {
    currentPosRef.current = time;
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
      heartbeatIntervalRef.current = null;
    }
    saveProgress(time);
  };

  const handleTimeUpdate = (time) => {
    currentPosRef.current = time;
  };

  const handleClose = () => {
    saveProgress(currentPosRef.current);
    window.dispatchEvent(new CustomEvent('streamhub-video-update', { detail: { videoId } }));
    onClose();
  };

  //close watch panel on escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  //close panel when clicking backdrop overlay
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      handleClose();
    }
  };

  if (!videoId) return null;

  return (
    <div className="watch-panel-backdrop" onClick={handleBackdropClick}>
      <div className="watch-panel-container" ref={panelRef}>
        
        <Navbar />

        
        <div className="watch-panel-content">
          {loading ? (
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: '400px',
              padding: '60px 20px',
              textAlign: 'center',
              color: 'var(--text-dim)'
            }}>
              <div style={{ fontSize: '18px', fontWeight: 500 }}>Loading video player...</div>
            </div>
          ) : !video ? (
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: '400px',
              padding: '60px 20px',
              textAlign: 'center',
              color: 'var(--text)'
            }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔒</div>
              <h2 style={{ fontSize: '24px', fontWeight: 600, marginBottom: '8px' }}>Video not found</h2>
              <p style={{ color: 'var(--text-dim)', maxWidth: '440px', marginBottom: '24px', lineHeight: '1.5' }}>
                This video is private or does not exist. It is only accessible to authorized viewers.
              </p>
              <button 
                className="btn btn-primary"
                onClick={handleClose}
                style={{ padding: '10px 24px', borderRadius: '8px' }}
              >
                Close Player
              </button>
            </div>
          ) : (
            <>
              <div className="watch-stage-container">
                
                <div className="watch-player-frame">
                  <button className="watch-panel-close-btn" onClick={handleClose} aria-label="Close video player">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="18" y1="6" x2="6" y2="18"></line>
                      <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                  </button>

                  <VideoPlayer 
                    videoId={videoId} 
                    initialPosition={initialPosition}
                    onPlay={handlePlay}
                    onPause={handlePause}
                    onTimeUpdate={handleTimeUpdate}
                  />
                </div>

                {video && video.isHidden && (
                  <div style={{
                    background: 'rgba(239, 68, 68, 0.15)',
                    border: '1px solid rgba(239, 68, 68, 0.3)',
                    color: '#ef4444',
                    padding: '10px 16px',
                    borderRadius: '8px',
                    marginTop: '16px',
                    marginBottom: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: '14px'
                  }}>
                    <span>🔒</span>
                    <span><strong>Moderation Notice:</strong> This video has been hidden by moderators and is only visible to you (the creator) and platform staff.</span>
                  </div>
                )}

                {video && video.privacy === 'PRIVATE' && (
                  <div style={{
                    background: 'rgba(55, 65, 81, 0.3)',
                    border: '1px solid var(--border, rgba(255, 255, 255, 0.1))',
                    color: 'var(--text-dim, #9aa3c0)',
                    padding: '10px 16px',
                    borderRadius: '8px',
                    marginTop: '16px',
                    marginBottom: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: '14px'
                  }}>
                    <span>🔒</span>
                    <span><strong>Private Video:</strong> This video is private and only visible to you and platform staff.</span>
                  </div>
                )}

                <h1 className="watch-video-title">
                  {video ? video.title : (loading ? 'Loading title...' : 'Untitled')}
                </h1>

            
            <div className="watch-primary-bar">
              
              <div className="watch-channel-col">
                <Link 
                  to={video ? `/profile/${video.uploaderId || video.ownerId}` : '#'} 
                  className="watch-profile-link"
                >
                  <div className="watch-profile-avatar">
                    {video && (video.uploaderAvatarUrl || video.ownerAvatarUrl) ? (
                      <img 
                        src={getFullImageUrl(video.uploaderAvatarUrl || video.ownerAvatarUrl)} 
                        alt={creatorName} 
                      />
                    ) : (
                      creatorName[0].toUpperCase()
                    )}
                  </div>
                  <div className="watch-profile-info">
                    <span className="watch-profile-name">{creatorName}</span>
                  </div>
                </Link>
              </div>

              
              <div className="watch-actions-col">
                <div className="watch-actions-pill">
                  <LikeButton videoId={videoId} />
                  <div className="watch-action-divider" />
                  <OptionsMenu 
                    items={[
                      {
                        label: 'Save to Playlist',
                        onClick: () => requireAuth(() => setShowPlaylistMenu(true))
                      },
                      ...((!user || !video || (user.userId !== video.ownerId && user.userId !== video.uploaderId && user.username !== video.ownerUsername && user.username !== video.uploaderUsername)) ? [
                        {
                          label: 'Report Video',
                          onClick: () => requireAuth(() => setShowReportModal(true))
                        }
                      ] : [])
                    ]} 
                  />
                </div>
              </div>
            </div>
          </div>

          
          <div className="watch-details-section">
            <div 
              className="yt-description-box"
              style={{ cursor: 'pointer' }}
              onClick={() => setIsDescriptionExpanded(!isDescriptionExpanded)}
            >
              <div className="watch-meta-counts">
                <span className="watch-view-count">
                  {new Intl.NumberFormat('en-US', { notation: "compact", compactDisplay: "short" }).format(video.viewCount || 0)} views
                </span>
                {video.createdAt && (
                  <span className="watch-upload-date">
                    {new Date(video.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                  </span>
                )}
                <span className="watch-category-badge">
                  {video.category ? (typeof video.category === 'object' ? video.category.name : video.category) : 'Uncategorized'}
                </span>
              </div>

              <div style={{ 
                whiteSpace: 'pre-wrap',
                marginTop: '8px',
                ...(isDescriptionExpanded ? {} : {
                  display: '-webkit-box',
                  WebkitLineClamp: 3,
                  WebkitBoxOrient: 'vertical',
                  overflow: 'hidden'
                })
              }}>
                {video.description || 'No description provided.'}
              </div>

              <div style={{ marginTop: '8px', fontSize: '13px', fontWeight: 'bold', color: '#9ca3af' }}>
                {isDescriptionExpanded ? 'Show less' : '...more'}
              </div>
            </div>

            <div className="yt-comments-section">
              <CommentList videoId={videoId} />
            </div>
          </div>
        </>
      )}
    </div>
  </div>

      {showReportModal && (
        <ReportModal
          reportableType="VIDEO"
          reportableId={videoId}
          onClose={() => setShowReportModal(false)}
          onSuccess={() => setShowReportModal(false)}
        />
      )}

      {showPlaylistMenu && (
        <AddToPlaylistMenu 
          videoId={videoId} 
          onClose={() => setShowPlaylistMenu(false)} 
        />
      )}
    </div>
  );
};

export default VideoWatchPanel;

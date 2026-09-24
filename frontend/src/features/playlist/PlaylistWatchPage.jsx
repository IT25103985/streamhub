import React, { useEffect, useState, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { playlistService } from './playlistService';
import VideoPlayer from '../video/VideoPlayer';
import CommentList from '../social/CommentList';
import LikeButton from '../social/LikeButton';
import OptionsMenu from '../../components/OptionsMenu';
import ReportModal from '../report/ReportModal';
import PlaylistSidebarPanel from './PlaylistSidebarPanel';
import SkeletonBase from '../../components/skeletons/SkeletonBase';
import CommentSkeleton from '../../components/skeletons/CommentSkeleton';
import { AuthContext, useRequireAuth } from '../../context/AuthContext';
import axiosInstance from '../../api/axiosInstance';
import '../video/VideoWatchPanel.css';

const renderDescriptionWithLinks = (text) => {
  if (!text) return 'No description provided.';
  const urlRegex = /(https?:\/\/[^\s]+)/g;
  const parts = text.split(urlRegex);
  return parts.map((part, i) => {
    if (part.match(urlRegex)) {
      return (
        <a 
          key={i} 
          href={part} 
          target="_blank" 
          rel="noopener noreferrer" 
          onClick={(e) => e.stopPropagation()}
          style={{ color: 'var(--accent-light, #818cf8)', textDecoration: 'none' }}
          onMouseEnter={(e) => e.target.style.textDecoration = 'underline'}
          onMouseLeave={(e) => e.target.style.textDecoration = 'none'}
        >
          {part}
        </a>
      );
    }
    return part;
  });
};

//dedicated playback view that automatically advances through playlist queue
const PlaylistWatchPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const requireAuth = useRequireAuth();
    const { user } = useContext(AuthContext);
    
    const [playlist, setPlaylist] = useState(null);
    const [currentVideoId, setCurrentVideoId] = useState(null);
    const [videoDetails, setVideoDetails] = useState(null);
    
    const [loadingPlaylist, setLoadingPlaylist] = useState(true);
    const [loadingVideo, setLoadingVideo] = useState(false);
    
    const [showReportModal, setShowReportModal] = useState(false);
    const [isDescriptionExpanded, setIsDescriptionExpanded] = useState(false);
    const [error, setError] = useState('');

    const fetchPlaylist = async () => {
        try {
            setLoadingPlaylist(true);
            const data = await playlistService.getPlaylistDetails(id);
            setPlaylist(data);
            
            //autoplay first video in playlist if none selected
            if (data.videos && data.videos.length > 0 && !currentVideoId) {
                setCurrentVideoId(data.videos[0].videoId);
            } else if (!data.videos || data.videos.length === 0) {
                setCurrentVideoId(null);
                setVideoDetails(null);
            }
            setError('');
        } catch (err) {
            console.error('Error fetching playlist details', err);
            setError(err.response?.status === 404 ? 'Playlist not found or is private.' : 'Failed to load playlist.');
        } finally {
            setLoadingPlaylist(false);
        }
    };

    useEffect(() => {
        fetchPlaylist();
    }, [id]);

    useEffect(() => {
        if (!currentVideoId) return;
        
        const fetchVideoDetails = async () => {
            setLoadingVideo(true);
            try {
                const res = await axiosInstance.get(`/videos/${currentVideoId}`);
                setVideoDetails(res.data);
            } catch (err) {
                console.error("Error fetching video details", err);
            } finally {
                setLoadingVideo(false);
            }
        };
        
        fetchVideoDetails();
    }, [currentVideoId]);

    const handleVideoSelect = (videoId) => {
        setCurrentVideoId(videoId);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    if (error) {
        return <div style={{ padding: '40px', textAlign: 'center', color: 'red' }}><h2>{error}</h2><button onClick={() => navigate('/')} className="btn btn-primary">Go Home</button></div>;
    }

    if (loadingPlaylist && !playlist) {
        return (
            <div style={{ display: 'flex', flexDirection: 'row', gap: '24px', padding: '24px', maxWidth: '1600px', margin: '0 auto', minHeight: 'calc(100vh - 140px)' }}>
                <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
                    <div style={{ width: '100%', aspectRatio: '16/9', borderRadius: '12px', overflow: 'hidden', marginBottom: '16px' }}>
                        <SkeletonBase width="100%" height="100%" borderRadius={12} />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
                        <SkeletonBase width="70%" height="24px" />
                        <SkeletonBase width="30%" height="16px" />
                    </div>
                </div>
                <div style={{ width: '400px', flexShrink: 0, display: 'flex', flexDirection: 'column', gap: '12px', padding: '16px', background: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)' }}>
                    <SkeletonBase width="60%" height="20px" style={{ marginBottom: '8px' }} />
                    {Array.from({ length: 5 }).map((_, i) => (
                        <CommentSkeleton key={`playlist-side-skel-${i}`} />
                    ))}
                </div>
            </div>
        );
    }

    const isOwner = user && playlist && user.username === playlist.ownerUsername;

    //playlist continuous player section
  return (
        <div style={{ display: 'flex', flexDirection: 'row', gap: '24px', padding: '24px', maxWidth: '1600px', margin: '0 auto', minHeight: 'calc(100vh - 140px)' }}>
            
            <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
                {currentVideoId ? (
                    <>
                        <div style={{ position: 'relative', width: '100%', aspectRatio: '16/9', backgroundColor: '#000', borderRadius: '12px', overflow: 'hidden', marginBottom: '16px' }}>
                            <VideoPlayer videoId={currentVideoId} autoPlay={true} />
                        </div>
                        
                        {loadingVideo ? (
                            <div className="watch-panel-details" style={{ padding: 0 }}>
                                <SkeletonBase width="75%" height="22px" style={{ marginBottom: '12px' }} />
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
                                    <SkeletonBase width="120px" height="16px" />
                                    <SkeletonBase width="80px" height="32px" borderRadius="8px" />
                                </div>
                            </div>
                        ) : videoDetails ? (
                            <div className="watch-panel-details" style={{ padding: 0 }}>
                                <h1 className="yt-title" style={{ fontSize: '20px', marginBottom: '8px' }}>{videoDetails.title}</h1>
                                
                                <div className="yt-meta-row" style={{ marginBottom: '16px' }}>
                                    <div className="yt-views-date">
                                        {new Intl.NumberFormat('en-US', { notation: "compact", compactDisplay: "short" }).format(videoDetails.viewCount || 0)} views
                                    </div>
                                    
                                    <div className="yt-actions">
                                        <div className="yt-action-btn-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                            <LikeButton videoId={currentVideoId} />
                                            {(!user || (user.userId !== videoDetails.uploaderId && user.userId !== videoDetails.ownerId && user.username !== videoDetails.uploaderUsername && user.username !== videoDetails.creator)) && (
                                                <OptionsMenu items={[
                                                    {
                                                        label: 'Report',
                                                        onClick: () => requireAuth(() => setShowReportModal(true))
                                                    }
                                                ]} />
                                            )}
                                        </div>
                                    </div>
                                </div>
                                
                                <div className="yt-channel-row" style={{ backgroundColor: 'rgba(255, 255, 255, 0.04)', border: 'none', padding: '6px 16px 6px 6px', borderRadius: '12px', marginBottom: '16px', display: 'inline-flex', alignItems: 'center', gap: '12px' }}>
                                    <div className="yt-avatar">
                                        {(videoDetails.uploaderUsername || videoDetails.creator || 'U')[0]}
                                    </div>
                                    <div className="yt-channel-info">
                                        <div className="yt-channel-name">{videoDetails.uploaderUsername || videoDetails.creator}</div>
                                    </div>
                                </div>
                                
                                <div 
                                    className="yt-description-box"
                                    style={{ cursor: 'pointer', backgroundColor: 'var(--bg-secondary)', padding: '12px', borderRadius: '12px', marginBottom: '24px' }}
                                    onClick={() => setIsDescriptionExpanded(!isDescriptionExpanded)}
                                >
                                    <div style={{ 
                                        whiteSpace: 'pre-wrap',
                                        ...(isDescriptionExpanded ? {} : {
                                            display: '-webkit-box',
                                            WebkitLineClamp: 3,
                                            WebkitBoxOrient: 'vertical',
                                            overflow: 'hidden'
                                        })
                                    }}>
                                        {renderDescriptionWithLinks(videoDetails.description)}
                                    </div>
                                    <div style={{ marginTop: '8px', fontSize: '14px', fontWeight: 'bold' }}>
                                        {isDescriptionExpanded ? 'Show less' : '...more'}
                                    </div>
                                </div>
                                
                                <div className="yt-comments-section">
                                    <CommentList videoId={currentVideoId} />
                                </div>
                            </div>
                        ) : (
                            <div style={{ padding: '20px', color: 'red' }}>Error loading video details.</div>
                        )}
                    </>
                ) : (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '400px', backgroundColor: 'var(--bg-secondary)', borderRadius: '12px' }}>
                        <p style={{ color: 'var(--text-secondary)' }}>This playlist has no videos.</p>
                    </div>
                )}
            </div>

            
            <div style={{ width: '400px', flexShrink: 0, height: 'calc(100vh - 100px)', position: 'sticky', top: '80px' }}>
                <PlaylistSidebarPanel 
                    playlist={playlist} 
                    currentVideoId={currentVideoId}
                    onVideoSelect={handleVideoSelect}
                    isOwner={isOwner}
                    onPlaylistUpdated={fetchPlaylist}
                />
            </div>
            
            {showReportModal && currentVideoId && (
                <ReportModal
                    reportableType="VIDEO"
                    reportableId={currentVideoId}
                    onClose={() => setShowReportModal(false)}
                    onSuccess={() => setShowReportModal(false)}
                />
            )}
        </div>
    );
};

export default PlaylistWatchPage;

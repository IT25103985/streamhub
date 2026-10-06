import React, { useState, useEffect, useContext } from 'react';
import watchHistoryService from './watchHistoryService';
import { AuthContext, useRequireAuth } from '../../context/AuthContext';
import { getSettings } from '../../api/settingsApi';
import { Link } from 'react-router-dom';
import { getFullImageUrl } from '../../utils/imageUtils';
import VideoWatchPanel from '../video/VideoWatchPanel';
import VideoGridSkeleton from '../../components/skeletons/VideoGridSkeleton';
import OptionsMenu from '../../components/OptionsMenu';
import ReportModal from '../report/ReportModal';
import AddToPlaylistMenu from '../playlist/AddToPlaylistMenu';
import './WatchHistoryPage.css';

const formatSeconds = (sec) => {
    if (!sec || isNaN(sec)) return '0:00';
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
};

const formatViews = (views) => {
    if (typeof views !== 'number') return views || '0';
    return new Intl.NumberFormat('en-US', { notation: 'compact', compactDisplay: 'short' }).format(views);
};

const getDateGroup = (dateStr) => {
    if (!dateStr) return 'Earlier';
    const date = new Date(dateStr);
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
        return 'Today';
    } else if (date.toDateString() === yesterday.toDateString()) {
        return 'Yesterday';
    } else {
        const diffDays = Math.round((today - date) / (1000 * 60 * 60 * 24));
        if (diffDays >= 2 && diffDays < 7) {
            return date.toLocaleDateString('en-US', { weekday: 'long' });
        } else {
            return date.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: today.getFullYear() !== date.getFullYear() ? 'numeric' : undefined });
        }
    }
};

const ICONS = {
    playlist: (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
        </svg>
    ),
    report: (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" />
            <line x1="4" y1="22" x2="4" y2="15" />
        </svg>
    ),
    trash: (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            <line x1="10" y1="11" x2="10" y2="17" />
            <line x1="14" y1="11" x2="14" y2="17" />
        </svg>
    )
};

//user watch history timeline grouped by date with resume progress bar
const WatchHistoryPage = () => {
    const { user } = useContext(AuthContext);
    const requireAuth = useRequireAuth();
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeVideoId, setActiveVideoId] = useState(null);
    
    //modal states for action popups
    const [playlistModalVideoId, setPlaylistModalVideoId] = useState(null);
    const [reportModalVideoId, setReportModalVideoId] = useState(null);

    const [isHistoryEnabled, setIsHistoryEnabled] = useState(true);

    const fetchHistory = async (isBackground = false) => {
        if (!user) return;
        try {
            if (!isBackground) setLoading(true);
            const [historyData, settingsData] = await Promise.all([
                watchHistoryService.getWatchHistory(0, 100).catch(() => ({ content: [] })),
                getSettings().catch(() => null)
            ]);
            setHistory(historyData?.content || []);
            if (settingsData && typeof settingsData.watchHistoryEnabled === 'boolean') {
                setIsHistoryEnabled(settingsData.watchHistoryEnabled);
            }
        } catch (err) {
            if (!isBackground) setError('Failed to load watch history');
            console.error(err);
        } finally {
            if (!isBackground) setLoading(false);
        }
    };

    //initial fetch of watch history
    useEffect(() => {
        fetchHistory(false);
    }, [user]);

    //listen for live history sync events
    useEffect(() => {
        if (!user) return;

        const handleLiveUpdate = () => {
            fetchHistory(true);
        };

        window.addEventListener('streamhub-video-update', handleLiveUpdate);
        window.addEventListener('focus', handleLiveUpdate);
        document.addEventListener('visibilitychange', handleLiveUpdate);

        //poll for updates while history page is open
        const interval = setInterval(handleLiveUpdate, 10000);

        //watch history timeline section
  return () => {
            window.removeEventListener('streamhub-video-update', handleLiveUpdate);
            window.removeEventListener('focus', handleLiveUpdate);
            document.removeEventListener('visibilitychange', handleLiveUpdate);
            clearInterval(interval);
        };
    }, [user]);

    //wipes entire watch history log for user
  const handleClearHistory = async () => {
        if (!window.confirm("Are you sure you want to clear your entire watch history?")) return;
        
        try {
            await watchHistoryService.clearWatchHistory();
            setHistory([]);
            window.dispatchEvent(new CustomEvent('streamhub-video-update'));
        } catch (err) {
            console.error('Failed to clear history:', err);
            alert('Failed to clear history.');
        }
    };

    const handleRemoveItem = async (watchHistoryId, e) => {
        if (e) e.stopPropagation();
        if (!window.confirm("Remove this video from your watch history?")) return;

        try {
            await watchHistoryService.removeFromWatchHistory(watchHistoryId);
            setHistory(prev => prev.filter(item => item.watchHistoryId !== watchHistoryId));
            window.dispatchEvent(new CustomEvent('streamhub-video-update'));
        } catch (err) {
            console.error('Failed to remove item from history:', err);
            alert('Failed to remove item from history.');
        }
    };

    if (!user) {
        return (
            <div className="watch-history-container">
                <div className="watch-history-empty">
                    <p className="watch-history-empty-title">Please log in to view your watch history.</p>
                    <Link to="/" style={{ color: '#60a5fa', textDecoration: 'none', fontWeight: 500 }}>Back to Home</Link>
                </div>
            </div>
        );
    }

    if (loading) {
        return (
            <div className="watch-history-container">
                <div className="watch-history-header">
                    <h1 className="watch-history-title">Watch History</h1>
                </div>
                <VideoGridSkeleton count={8} />
            </div>
        );
    }

    //bucket watch history items by relative date
    const groupedHistory = history.reduce((groups, item) => {
        if (!item.video) return groups;
        const groupName = getDateGroup(item.updatedAt || item.watchedAt);
        if (!groups[groupName]) {
            groups[groupName] = [];
        }
        groups[groupName].push(item);
        return groups;
    }, {});

    return (
        <div className="watch-history-container">
            
            <div className="watch-history-header">
                <h1 className="watch-history-title">Watch history</h1>
                
                <button 
                    onClick={handleClearHistory}
                    disabled={history.length === 0}
                    className="watch-history-clear-btn"
                >
                    {ICONS.trash}
                    <span>Clear all watch history</span>
                </button>
            </div>

            {!isHistoryEnabled && (
                <div style={{
                    background: 'rgba(234, 179, 8, 0.12)',
                    border: '1px solid rgba(234, 179, 8, 0.35)',
                    color: '#facc15',
                    padding: '12px 18px',
                    borderRadius: '8px',
                    marginBottom: '20px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '12px',
                    fontSize: '14px'
                }}>
                    <span>Watch history is paused in your settings. New videos you watch will not be recorded.</span>
                    <Link to="/settings" style={{
                        color: '#000',
                        background: '#facc15',
                        padding: '6px 14px',
                        borderRadius: '6px',
                        textDecoration: 'none',
                        fontWeight: 600,
                        fontSize: '13px',
                        flexShrink: 0
                    }}>
                        Go to Settings
                    </Link>
                </div>
            )}

            {error && (
                <div style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#ef4444', padding: '14px 18px', borderRadius: '8px', marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
                    <span>{error}</span>
                    <button 
                        onClick={() => { setError(null); fetchHistory(false); }} 
                        className="btn btn-secondary btn-sm"
                        style={{ flexShrink: 0 }}
                    >
                        Try Again
                    </button>
                </div>
            )}

            {history.length === 0 ? (
                <div className="watch-history-empty">
                    <p className="watch-history-empty-title">Your watch history is empty</p>
                    <p className="watch-history-empty-sub">Videos you watch will appear here.</p>
                    <Link to="/" style={{ display: 'inline-block', padding: '8px 18px', background: '#2563eb', color: '#fff', borderRadius: '8px', textDecoration: 'none', fontWeight: 500, fontSize: '14px' }}>
                        Explore videos
                    </Link>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
                    {Object.entries(groupedHistory).map(([groupTitle, items]) => (
                        <div key={groupTitle} className="watch-history-group">
                            <div className="watch-history-group-title">
                                {groupTitle}
                            </div>

                            <div className="watch-history-list">
                                {items.map((item) => {
                                    const video = item.video;
                                    const videoId = video.videoId || video.id;
                                    const lastPos = item.lastPosition || 0;
                                    const creator = video.ownerUsername || video.creator || 'Creator';
                                    const viewsFormatted = formatViews(video.viewCount);

                                    return (
                                        <div 
                                            key={item.watchHistoryId || videoId}
                                            className="watch-history-card"
                                        >
                                            
                                            <div 
                                                className="watch-history-thumb"
                                                onClick={() => setActiveVideoId(videoId)}
                                            >
                                                {video.thumbnailUrl ? (
                                                    <img 
                                                        src={getFullImageUrl(video.thumbnailUrl)} 
                                                        alt={video.title} 
                                                    />
                                                ) : (
                                                    <div className="watch-history-no-thumb">No Thumbnail</div>
                                                )}
                                                
                                                
                                                {video.duration && (
                                                    <span className="watch-history-duration">
                                                        {video.duration}
                                                    </span>
                                                )}

                                                
                                                {lastPos > 0 && (
                                                    <div className="watch-history-progress-bg">
                                                        <div 
                                                            className="watch-history-progress-fill" 
                                                            style={{ width: `${Math.min(100, Math.max(15, (lastPos % 100) * 1.5))}%` }}
                                                        />
                                                    </div>
                                                )}
                                            </div>

                                            
                                            <div className="watch-history-info">
                                                <h3 
                                                    className="watch-history-video-title"
                                                    onClick={() => setActiveVideoId(videoId)}
                                                >
                                                    {video.title}
                                                </h3>
                                                
                                                <div className="watch-history-meta">
                                                    <Link 
                                                        to={`/profile/${video.ownerId || video.uploaderId}`} 
                                                        className="watch-history-creator"
                                                        onClick={(e) => e.stopPropagation()}
                                                    >
                                                        {creator}
                                                    </Link>
                                                    <span>•</span>
                                                    <span>{viewsFormatted} views</span>
                                                </div>

                                                {video.description && (
                                                    <p className="watch-history-desc">
                                                        {video.description}
                                                    </p>
                                                )}

                                                {lastPos > 0 && (
                                                    <div className="watch-history-resume-tag">
                                                        Resume at {formatSeconds(lastPos)}
                                                    </div>
                                                )}
                                            </div>

                                            
                                            <div className="watch-history-options" onClick={(e) => e.stopPropagation()}>
                                                <OptionsMenu 
                                                    items={[
                                                        { 
                                                            label: 'Save to playlist', 
                                                            icon: ICONS.playlist,
                                                            onClick: () => requireAuth(() => setPlaylistModalVideoId(videoId))
                                                        },
                                                        ...((!user || (user.userId !== video.ownerId && user.userId !== video.uploaderId && user.username !== video.ownerUsername && user.username !== creator)) ? [
                                                            { 
                                                                label: 'Report', 
                                                                icon: ICONS.report,
                                                                onClick: () => requireAuth(() => setReportModalVideoId(videoId))
                                                            }
                                                        ] : []),
                                                        { 
                                                            label: 'Remove from watch history', 
                                                            icon: ICONS.trash,
                                                            className: 'danger',
                                                            onClick: (e) => handleRemoveItem(item.watchHistoryId, e)
                                                        }
                                                    ]} 
                                                />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            
            {activeVideoId && (
                <VideoWatchPanel 
                    videoId={activeVideoId} 
                    onClose={() => {
                        setActiveVideoId(null);
                        fetchHistory(true);
                    }} 
                />
            )}

            {playlistModalVideoId && (
                <AddToPlaylistMenu 
                    videoId={playlistModalVideoId} 
                    onClose={() => setPlaylistModalVideoId(null)} 
                />
            )}

            {reportModalVideoId && (
                <ReportModal 
                    reportableType="VIDEO" 
                    reportableId={reportModalVideoId} 
                    onClose={() => setReportModalVideoId(null)} 
                    onSuccess={() => setReportModalVideoId(null)} 
                />
            )}
        </div>
    );
};

export default WatchHistoryPage;

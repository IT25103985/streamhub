import React, { useState, useEffect, useCallback } from 'react';
import axios from '../../api/axiosInstance';
import VideoCard from '../../components/VideoCard';
import VideoGridSkeleton from '../../components/skeletons/VideoGridSkeleton';
import EditVideoModal from '../video/EditVideoModal';
import VideoWatchPanel from '../video/VideoWatchPanel';
import CategoryChipBar from '../../components/CategoryChipBar';
import AddToPlaylistMenu from '../playlist/AddToPlaylistMenu';
import './Profile.css';

//tab listing videos uploaded by user with management controls
const ProfileVideosTab = ({ uploaderId = 'me', onVideoCountChange, currentCount, search = '' }) => {
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [categoryId, setCategoryId] = useState(null);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [editingVideoDetails, setEditingVideoDetails] = useState(null);
    const [deletingId, setDeletingId] = useState(null);
    const [activeDropdownId, setActiveDropdownId] = useState(null);
    const [activeWatchVideoId, setActiveWatchVideoId] = useState(null);
    const [activeAddToPlaylistId, setActiveAddToPlaylistId] = useState(null);

    useEffect(() => {
        const handleClickOutside = () => setActiveDropdownId(null);
        document.addEventListener('click', handleClickOutside);
        return () => document.removeEventListener('click', handleClickOutside);
    }, []);

    const fetchVideos = useCallback(async (searchQuery = '', catId = null, pageNum = 0) => {
        setLoading(true);
        setError('');
        try {
            const params = {
                uploaderId: uploaderId,
                page: pageNum,
                size: 12
            };
            if (searchQuery) params.search = searchQuery;
            if (catId) params.categoryId = catId;

            const res = await axios.get('/videos', { params });
            //handle both paginated and flat list response formats
            if (res.data.content) {
                setVideos(res.data.content);
                setTotalPages(res.data.totalPages);
            } else {
                setVideos(res.data);
                setTotalPages(1);
            }
        } catch (err) {
            console.error("Failed to fetch videos", err);
            setError("Failed to load videos. Please try again.");
            setVideos([]);
        } finally {
            setLoading(false);
        }
    }, [uploaderId]);

    //refetch videos on search query or category or pagination change
    useEffect(() => {
        setPage(0);
    }, [search, categoryId]);

    useEffect(() => {
        fetchVideos(search, categoryId, page);
    }, [search, categoryId, page, fetchVideos]);

    const handleCategoryChange = (catId) => {
        setCategoryId(catId);
        setPage(0);
    };

    const handleDelete = async (videoId) => {
        if (!window.confirm("Are you sure you want to delete this video? This action cannot be undone.")) return;
        
        setDeletingId(videoId);
        try {
            await axios.delete(`/videos/${videoId}`);
            setVideos(videos.filter(v => v.videoId !== videoId));
            if (onVideoCountChange) {
                onVideoCountChange(Math.max(0, currentCount - 1));
            }
        } catch (err) {
            console.error("Failed to delete video", err);
            alert("Failed to delete video. Please try again.");
        } finally {
            setDeletingId(null);
        }
    };

    return (
        <div className="profile-tab-content">
            <div style={{ marginBottom: '24px' }}>
                <CategoryChipBar 
                    selectedCategoryId={categoryId} 
                    onCategoryChange={handleCategoryChange} 
                />
            </div>

            {editingVideoDetails && (
                <EditVideoModal 
                    video={editingVideoDetails} 
                    onClose={() => setEditingVideoDetails(null)} 
                    onSuccess={(updatedVideo) => {
                        setVideos(videos.map(v => v.videoId === updatedVideo.videoId ? updatedVideo : v));
                        setEditingVideoDetails(null);
                    }} 
                />
            )}

            {error ? (
                <p className="error-text">{error}</p>
            ) : (loading && videos.length === 0) ? (
                <VideoGridSkeleton count={8} />
            ) : videos.length > 0 ? (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '20px', opacity: loading ? 0.6 : 1, transition: 'opacity 0.2s ease' }}>
                    {videos.map(video => (
                        <div key={video.videoId} style={{ position: 'relative' }}>
                            <VideoCard video={video} onClick={() => setActiveWatchVideoId(video.videoId)} />
                            <div style={{ position: 'absolute', top: '10px', right: '10px', zIndex: 10 }}>
                                <button 
                                    className="btn btn-ghost"
                                    style={{ padding: '4px', background: 'rgba(10,13,22,0.8)', border: '1px solid rgba(255,255,255,0.2)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '32px', height: '32px', color: 'white' }}
                                    onClick={(e) => {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        setActiveDropdownId(activeDropdownId === video.videoId ? null : video.videoId);
                                    }}
                                >
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <circle cx="12" cy="5" r="1"></circle>
                                        <circle cx="12" cy="12" r="1"></circle>
                                        <circle cx="12" cy="19" r="1"></circle>
                                    </svg>
                                </button>
                                {activeDropdownId === video.videoId && (
                                    <div 
                                        style={{ position: 'absolute', top: '100%', right: '0', marginTop: '4px', background: '#1c1f2e', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', padding: '4px 0', minWidth: '150px', boxShadow: '0 4px 12px rgba(0,0,0,0.5)' }}
                                        onClick={(e) => {
                                           e.preventDefault();
                                           e.stopPropagation();
                                        }}
                                    >
                                        <button 
                                            style={{ display: 'block', width: '100%', padding: '8px 16px', textAlign: 'left', background: 'none', border: 'none', color: 'white', cursor: 'pointer', fontSize: '14px' }}
                                            onMouseOver={(e) => e.target.style.background = 'rgba(255,255,255,0.1)'}
                                            onMouseOut={(e) => e.target.style.background = 'none'}
                                            onClick={(e) => { e.preventDefault(); e.stopPropagation(); setEditingVideoDetails(video); setActiveDropdownId(null); }}
                                        >
                                            Edit Details
                                        </button>
                                        <button 
                                            style={{ display: 'block', width: '100%', padding: '8px 16px', textAlign: 'left', background: 'none', border: 'none', color: 'white', cursor: 'pointer', fontSize: '14px' }}
                                            onMouseOver={(e) => e.target.style.background = 'rgba(255,255,255,0.1)'}
                                            onMouseOut={(e) => e.target.style.background = 'none'}
                                            onClick={(e) => { e.preventDefault(); e.stopPropagation(); setActiveAddToPlaylistId(video.videoId); setActiveDropdownId(null); }}
                                        >
                                            Add to playlist
                                        </button>
                                        <button 
                                            style={{ display: 'block', width: '100%', padding: '8px 16px', textAlign: 'left', background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: '14px' }}
                                            onMouseOver={(e) => e.target.style.background = 'rgba(239,68,68,0.1)'}
                                            onMouseOut={(e) => e.target.style.background = 'none'}
                                            onClick={(e) => { e.preventDefault(); e.stopPropagation(); handleDelete(video.videoId); setActiveDropdownId(null); }}
                                            disabled={deletingId === video.videoId}
                                        >
                                            {deletingId === video.videoId ? 'Deleting...' : 'Delete'}
                                        </button>
                                    </div>
                                )}
                            </div>
                            
                            {activeDropdownId === video.videoId && (
                                <div 
                                    style={{ position: 'absolute', inset: 0, zIndex: 9 }}
                                    onClick={(e) => { e.stopPropagation(); e.preventDefault(); setActiveDropdownId(null); }}
                                />
                            )}
                        </div>
                    ))}
                </div>
            ) : (
                <p>{search ? 'No videos match your search' : 'No videos found.'}</p>
            )}

            {totalPages > 1 && (
                <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '20px' }}>
                    <button 
                        className="btn btn-secondary" 
                        disabled={page === 0} 
                        onClick={() => setPage(p => p - 1)}
                    >
                        Previous
                    </button>
                    <button 
                        className="btn btn-secondary" 
                        disabled={page >= totalPages - 1} 
                        onClick={() => setPage(p => p + 1)}
                    >
                        Next
                    </button>
                </div>
            )}

            {activeWatchVideoId && (
                <VideoWatchPanel 
                    videoId={activeWatchVideoId} 
                    onClose={() => setActiveWatchVideoId(null)} 
                />
            )}

            {activeAddToPlaylistId && (
                <AddToPlaylistMenu 
                    videoId={activeAddToPlaylistId} 
                    onClose={() => setActiveAddToPlaylistId(null)} 
                />
            )}
        </div>
    );
};

export default ProfileVideosTab;

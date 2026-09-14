import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import OptionsMenu from '../../components/OptionsMenu';
import PlaylistFormModal from './PlaylistFormModal';
import { playlistService } from './playlistService';
import { getFullImageUrl } from '../../utils/imageUtils';

//collapsible sidebar panel showing playlist queue and drag reordering
const PlaylistSidebarPanel = ({ playlist, currentVideoId, onVideoSelect, onPlaylistUpdated, isOwner }) => {
    const navigate = useNavigate();
    const [isReordering, setIsReordering] = useState(false);
    const [videos, setVideos] = useState(playlist.videos || []);
    const [draggedItemIndex, setDraggedItemIndex] = useState(null);
    const [savingReorder, setSavingReorder] = useState(false);
    
    const [showEditModal, setShowEditModal] = useState(false);

    //sync local video ordering with incoming playlist prop
    React.useEffect(() => {
        setVideos(playlist.videos || []);
    }, [playlist.videos]);

    const handleDragStart = (e, index) => {
        setDraggedItemIndex(index);
        e.dataTransfer.effectAllowed = 'move';
        //firefox requires drag data to be set
        e.dataTransfer.setData('text/html', e.target.parentNode);
    };

    const handleDragOver = (e, index) => {
        e.preventDefault();
        };

    const handleDrop = (e, targetIndex) => {
        e.preventDefault();
        
        if (draggedItemIndex === null || draggedItemIndex === targetIndex) return;

        const newVideos = [...videos];
        const draggedItem = newVideos[draggedItemIndex];
        
            newVideos.splice(draggedItemIndex, 1);
            newVideos.splice(targetIndex, 0, draggedItem);
        
        setVideos(newVideos);
        setDraggedItemIndex(null);
    };

    const handleSaveReorder = async () => {
        setSavingReorder(true);
        try {
            const orderedIds = videos.map(v => v.videoId);
            await playlistService.reorderVideos(playlist.playlistId, orderedIds);
            setIsReordering(false);
            if (onPlaylistUpdated) onPlaylistUpdated();
        } catch (err) {
            console.error('Failed to save reorder', err);
            alert('Failed to save the new order. Please try again.');
        } finally {
            setSavingReorder(false);
        }
    };

    const handleDeletePlaylist = async () => {
        if (window.confirm('Are you sure you want to delete this playlist?')) {
            try {
                await playlistService.deletePlaylist(playlist.playlistId);
                navigate('/my-playlists');
            } catch (err) {
                console.error('Failed to delete playlist', err);
                alert('Failed to delete playlist.');
            }
        }
    };

    const handleRemoveVideo = async (videoId, e) => {
        e.stopPropagation();
        if (window.confirm('Remove this video from the playlist?')) {
            try {
                setVideos(prev => prev.filter(v => v.videoId !== videoId));
                await playlistService.removeVideoFromPlaylist(playlist.playlistId, videoId);
                if (onPlaylistUpdated) onPlaylistUpdated();
            } catch (err) {
                console.error('Failed to remove video', err);
                alert('Failed to remove video.');
                if (onPlaylistUpdated) onPlaylistUpdated();
            }
        }
    };

    //playlist queue sidebar section
  return (
        <div style={{ backgroundColor: 'var(--bg-secondary)', borderRadius: '8px', overflow: 'hidden', display: 'flex', flexDirection: 'column', height: '100%' }}>
            
            <div style={{ padding: '16px', borderBottom: '1px solid var(--border-color)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                        <h2 style={{ margin: '0 0 8px 0', fontSize: '18px' }}>{playlist.name}</h2>
                        <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                            {playlist.ownerUsername} • {videos.length} videos
                        </div>
                        {playlist.privacy === 'PRIVATE' && (
                            <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px' }}>🔒 Private</div>
                        )}
                    </div>
                    {isOwner && !isReordering && (
                        <OptionsMenu items={[
                            { label: 'Reorder videos', onClick: () => setIsReordering(true) },
                            { label: 'Edit playlist', onClick: () => setShowEditModal(true) },
                            { label: 'Delete playlist', onClick: handleDeletePlaylist }
                        ]} />
                    )}
                </div>
            </div>

            
            <div style={{ flex: 1, overflowY: 'auto' }}>
                {videos.length === 0 ? (
                    <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                        This playlist is empty.
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                        {videos.map((video, index) => {
                            const isPlaying = currentVideoId === video.videoId;
                            
                            return (
                                <div 
                                    key={video.videoId}
                                    draggable={isReordering}
                                    onDragStart={(e) => handleDragStart(e, index)}
                                    onDragOver={(e) => handleDragOver(e, index)}
                                    onDrop={(e) => handleDrop(e, index)}
                                    onClick={() => !isReordering && onVideoSelect(video.videoId)}
                                    style={{ 
                                        display: 'flex', padding: '8px 16px', gap: '12px', 
                                        cursor: isReordering ? 'grab' : 'pointer',
                                        backgroundColor: isPlaying ? 'rgba(255, 255, 255, 0.1)' : 'transparent',
                                        borderBottom: '1px solid var(--border-color)',
                                        alignItems: 'center',
                                        opacity: draggedItemIndex === index ? 0.5 : 1,
                                    }}
                                >
                                    {isReordering && (
                                        <div style={{ cursor: 'grab', color: 'var(--text-secondary)', padding: '4px' }}>
                                            ≡
                                        </div>
                                    )}
                                    {!isReordering && (
                                        <div style={{ width: '16px', fontSize: '12px', color: 'var(--text-secondary)', textAlign: 'right' }}>
                                            {isPlaying ? '▶' : index + 1}
                                        </div>
                                    )}
                                    
                                    <div style={{ width: '120px', flexShrink: 0, position: 'relative' }}>
                                        <img 
                                            src={getFullImageUrl(video.thumbnailUrl)} 
                                            alt={video.title} 
                                            style={{ width: '100%', aspectRatio: '16/9', objectFit: 'cover', borderRadius: '4px' }} 
                                        />
                                        <div style={{ position: 'absolute', bottom: '4px', right: '4px', backgroundColor: 'rgba(0,0,0,0.8)', color: 'white', fontSize: '10px', padding: '2px 4px', borderRadius: '2px' }}>
                                            {video.duration || '0:00'}
                                        </div>
                                    </div>
                                    
                                    <div style={{ flex: 1, minWidth: 0 }}>
                                        <div style={{ fontWeight: isPlaying ? 'bold' : 'normal', fontSize: '14px', marginBottom: '4px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                            {video.title}
                                        </div>
                                        <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                                            {video.uploaderUsername}
                                        </div>
                                    </div>

                                    {isOwner && !isReordering && (
                                        <button 
                                            onClick={(e) => handleRemoveVideo(video.videoId, e)}
                                            style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '4px', fontSize: '16px' }}
                                            title="Remove from playlist"
                                        >
                                            &times;
                                        </button>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            
            {isReordering && (
                <div style={{ padding: '16px', borderTop: '1px solid var(--border-color)', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                    <button 
                        className="btn-cancel"
                        onClick={() => {
                            setIsReordering(false);
                            setVideos(playlist.videos || []);
                        }}
                        disabled={savingReorder}
                    >
                        Cancel
                    </button>
                    <button 
                        className="btn btn-primary"
                        onClick={handleSaveReorder}
                        disabled={savingReorder}
                    >
                        {savingReorder ? 'Saving...' : 'Done'}
                    </button>
                </div>
            )}

            
            {showEditModal && (
                <PlaylistFormModal 
                    playlist={playlist}
                    onClose={() => setShowEditModal(false)}
                    onSaveSuccess={() => {
                        setShowEditModal(false);
                        if (onPlaylistUpdated) onPlaylistUpdated();
                    }}
                />
            )}
        </div>
    );
};

export default PlaylistSidebarPanel;

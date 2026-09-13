import React, { useState, useEffect, useRef } from 'react';
import ReactDOM from 'react-dom';
import { playlistService } from './playlistService';
import PlaylistFormModal from './PlaylistFormModal';
import SkeletonBase from '../../components/skeletons/SkeletonBase';
import Loader from '../../components/Loader';
import '../profile/Profile.css';

const AddToPlaylistMenu = ({ videoId, onClose }) => {
    const [playlists, setPlaylists] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [addedStatus, setAddedStatus] = useState({}); //track per playlist addition state
    
    const [showCreateModal, setShowCreateModal] = useState(false);
    
    const menuRef = useRef(null);

    useEffect(() => {
        fetchPlaylists();
        
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target) && !showCreateModal) {
                onClose();
            }
        };
        
        const handleEscape = (e) => {
            if (e.key === 'Escape' && !showCreateModal) {
                onClose();
            }
        };
        
        document.addEventListener('mousedown', handleClickOutside);
        window.addEventListener('keydown', handleEscape);
        
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            window.removeEventListener('keydown', handleEscape);
        };
    }, [onClose, showCreateModal]);

    const fetchPlaylists = async () => {
        try {
            const data = await playlistService.getUserPlaylists(false);
            setPlaylists(data);
        } catch (err) {
            console.error('Failed to fetch playlists:', err);
            setError('Failed to load playlists.');
        } finally {
            setLoading(false);
        }
    };

    const handleAddToPlaylist = async (playlistId) => {
        setAddedStatus(prev => ({ ...prev, [playlistId]: { status: 'loading' } }));
        try {
            await playlistService.addVideoToPlaylist(playlistId, videoId);
            setAddedStatus(prev => ({ ...prev, [playlistId]: { status: 'success', text: '✓ Added' } }));
            
            setTimeout(() => {
                setAddedStatus(prev => ({ ...prev, [playlistId]: null }));
            }, 2500);
        } catch (err) {
            console.error('Failed to add video to playlist:', err);
            const msg = err.response?.data?.message || 'Failed';
            const isAlreadyAdded = msg.toLowerCase().includes('already');
            setAddedStatus(prev => ({ 
                ...prev, 
                [playlistId]: { 
                    status: isAlreadyAdded ? 'warning' : 'error', 
                    text: isAlreadyAdded ? 'Already added' : 'Failed' 
                } 
            }));
            
            setTimeout(() => {
                setAddedStatus(prev => ({ ...prev, [playlistId]: null }));
            }, 2500);
        }
    };

    const handleCreateSuccess = async () => {
        setShowCreateModal(false);
        setLoading(true);
        try {
            const data = await playlistService.getUserPlaylists(false);
            setPlaylists(data);
            
            //add to newest playlist created by user
            if (data.length > 0) {
                const newestPlaylist = data[0];
                await handleAddToPlaylist(newestPlaylist.playlistId);
            }
        } catch (err) {
            console.error('Failed to refresh playlists after create:', err);
        } finally {
            setLoading(false);
        }
    };

    return ReactDOM.createPortal(
        <div className="profile-modal-overlay" onClick={(e) => e.stopPropagation()} style={{ zIndex: 1000, backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="profile-modal-content" ref={menuRef} onClick={(e) => e.stopPropagation()} style={{ width: '350px', padding: '16px', maxHeight: '80vh', overflowY: 'auto' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                    <h3 style={{ margin: 0 }}>Add to playlist</h3>
                    <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '20px', cursor: 'pointer', color: 'var(--text-secondary)' }}>&times;</button>
                </div>
                
                <button 
                    onClick={() => setShowCreateModal(true)}
                    style={{ 
                        width: '100%', padding: '12px', marginBottom: '16px', 
                        display: 'flex', alignItems: 'center', gap: '8px',
                        background: 'var(--bg-tertiary)', border: '1px dashed var(--border-color)', 
                        color: 'var(--primary-color)', cursor: 'pointer', borderRadius: '4px',
                        fontWeight: 'bold'
                    }}
                >
                    <span style={{ fontSize: '18px' }}>+</span> Create new playlist
                </button>
                
                {error && <div className="error-text" style={{marginBottom: '16px'}}>{error}</div>}
                
                {loading ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <SkeletonBase width="100%" height="42px" borderRadius="4px" />
                        <SkeletonBase width="100%" height="42px" borderRadius="4px" />
                        <SkeletonBase width="100%" height="42px" borderRadius="4px" />
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        {playlists.length === 0 ? (
                            <p style={{ textAlign: 'center', color: 'var(--text-secondary)' }}>You don't have any playlists yet.</p>
                        ) : (
                            playlists.map(playlist => (
                                <button 
                                    key={playlist.playlistId} 
                                    onClick={() => handleAddToPlaylist(playlist.playlistId)}
                                    disabled={addedStatus[playlist.playlistId]?.status === 'loading'}
                                    style={{ 
                                        width: '100%', padding: '12px', textAlign: 'left',
                                        background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', 
                                        color: 'var(--text-primary)', cursor: 'pointer', borderRadius: '4px',
                                        display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                                    }}
                                >
                                    <span style={{ fontWeight: '500' }}>{playlist.name}</span>
                                    {addedStatus[playlist.playlistId] && (
                                        <span style={{ 
                                            fontSize: '12px', 
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '4px',
                                            color: addedStatus[playlist.playlistId].status === 'success' ? '#10b981' :
                                                   addedStatus[playlist.playlistId].status === 'warning' ? '#f59e0b' : 
                                                   addedStatus[playlist.playlistId].status === 'loading' ? 'var(--text-secondary)' : '#ef4444'
                                        }}>
                                            {addedStatus[playlist.playlistId].status === 'loading' ? (
                                                <Loader size={14} />
                                            ) : (
                                                addedStatus[playlist.playlistId].text
                                            )}
                                        </span>
                                    )}
                                </button>
                            ))
                        )}
                    </div>
                )}
            </div>
            
            {showCreateModal && (
                <PlaylistFormModal 
                    onClose={() => setShowCreateModal(false)}
                    onSaveSuccess={handleCreateSuccess}
                />
            )}
        </div>,
        document.body
    );
};

export default AddToPlaylistMenu;

import React, { useEffect, useState, useContext } from 'react';
import { playlistService } from '../features/playlist/playlistService';
import PlaylistCard from '../features/playlist/PlaylistCard';
import PlaylistCardSkeleton from '../components/skeletons/PlaylistCardSkeleton';
import PlaylistFormModal from '../features/playlist/PlaylistFormModal';
import { AuthContext, useRequireAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';

//user playlist library view with create playlist modal
const MyPlaylistsPage = () => {
    const { user, openAuthModal } = useContext(AuthContext);
    const requireAuth = useRequireAuth();
    const [playlists, setPlaylists] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showCreateModal, setShowCreateModal] = useState(false);

    const fetchPlaylists = async () => {
        if (!user) return;
        setLoading(true);
        try {
            const data = await playlistService.getUserPlaylists(false);
            setPlaylists(data || []);
            setError('');
        } catch (err) {
            console.error('Failed to fetch playlists:', err);
            setError('Failed to load playlists.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (user) {
            fetchPlaylists();
        } else {
            setLoading(false);
        }
    }, [user]);

    const handleCreateClick = () => {
        requireAuth(() => {
            setShowCreateModal(true);
        });
    };

    const handleCreateSuccess = () => {
        setShowCreateModal(false);
        fetchPlaylists();
    };

    if (!user) {
        return (
            <div style={{ padding: '40px 24px', maxWidth: '600px', margin: '40px auto', textAlign: 'center', background: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)' }}>
                <h2>My Playlists</h2>
                <p style={{ color: 'var(--text-secondary)', marginBottom: '20px' }}>Please sign in to view and create playlists.</p>
                <button className="btn btn-primary" onClick={() => openAuthModal('login')}>Sign In</button>
            </div>
        );
    }

    return (
        <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto', minHeight: 'calc(100vh - 140px)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                <h1 style={{ margin: 0 }}>My Playlists</h1>
                <button 
                    onClick={handleCreateClick} 
                    className="btn btn-primary"
                >
                    + Create Playlist
                </button>
            </div>

            {error && (
                <div style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#ef4444', padding: '14px 18px', borderRadius: '8px', marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span>{error}</span>
                    <button className="btn btn-secondary btn-sm" onClick={fetchPlaylists}>Try Again</button>
                </div>
            )}

            {loading ? (
                <div className="videos-grid home-4col-grid" id="my-playlists-grid">
                    {Array.from({ length: 8 }).map((_, i) => (
                        <PlaylistCardSkeleton key={`my-playlist-skel-${i}`} />
                    ))}
                </div>
            ) : playlists.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>
                    You haven't created any playlists yet.
                </div>
            ) : (
                <div className="videos-grid home-4col-grid" id="my-playlists-grid">
                    {playlists.map(playlist => (
                        <PlaylistCard 
                            key={playlist.playlistId} 
                            playlist={playlist} 
                            isOwner={true}
                            onPlaylistUpdated={fetchPlaylists}
                        />
                    ))}
                </div>
            )}

            {showCreateModal && (
                <PlaylistFormModal 
                    onClose={() => setShowCreateModal(false)}
                    onSaveSuccess={handleCreateSuccess}
                />
            )}
        </div>
    );
};

export default MyPlaylistsPage;

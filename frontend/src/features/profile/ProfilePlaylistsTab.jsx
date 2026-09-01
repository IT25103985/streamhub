import React, { useState, useEffect, useCallback } from 'react';
import PlaylistCard from '../playlist/PlaylistCard';
import PlaylistCardSkeleton from '../../components/skeletons/PlaylistCardSkeleton';
import PlaylistFormModal from '../playlist/PlaylistFormModal';
import { playlistService } from '../playlist/playlistService';
import './Profile.css';

//tab displaying playlists created by the profile owner
const ProfilePlaylistsTab = ({ ownerId = 'me', search = '' }) => {
    const [playlists, setPlaylists] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showCreateModal, setShowCreateModal] = useState(false);

    const fetchPlaylists = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const data = ownerId === 'me' 
                ? await playlistService.getUserPlaylists(true, search)
                : await playlistService.getPlaylistsByOwner(ownerId, true);
            setPlaylists(data || []);
        } catch (err) {
            console.error("Failed to fetch playlists", err);
            setError("Failed to load playlists. Please try again.");
            setPlaylists([]);
        } finally {
            setLoading(false);
        }
    }, [ownerId, search]);

    useEffect(() => {
        fetchPlaylists();
    }, [fetchPlaylists]);

    const handleCreateSuccess = () => {
        setShowCreateModal(false);
        fetchPlaylists();
    };

    return (
        <div className="profile-tab-content">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3 style={{ margin: 0 }}>{ownerId === 'me' ? 'My Playlists (Own Videos Only)' : 'Playlists'}</h3>
                {ownerId === 'me' && (
                    <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>+ Create Playlist</button>
                )}
            </div>
            
            {error ? (
                <p className="error-text">{error}</p>
            ) : (loading && playlists.length === 0) ? (
                <div className="videos-grid home-4col-grid">
                    {Array.from({ length: 8 }).map((_, i) => (
                        <PlaylistCardSkeleton key={`profile-playlist-skel-${i}`} />
                    ))}
                </div>
            ) : playlists.length > 0 ? (
                <div className="videos-grid home-4col-grid" style={{ opacity: loading ? 0.6 : 1, transition: 'opacity 0.2s ease' }}>
                    {playlists.map(playlist => (
                        <PlaylistCard 
                            key={playlist.playlistId} 
                            playlist={playlist} 
                            isOwner={ownerId === 'me'}
                            onPlaylistUpdated={fetchPlaylists}
                        />
                    ))}
                </div>
            ) : (
                <p>{search ? 'No playlists match your search' : 'No playlists found containing exclusively your own videos.'}</p>
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

export default ProfilePlaylistsTab;

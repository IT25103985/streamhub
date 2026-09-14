import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { getFullImageUrl } from '../../utils/imageUtils';
import OptionsMenu from '../../components/OptionsMenu';
import PlaylistFormModal from './PlaylistFormModal';
import ReportModal from '../report/ReportModal';
import { playlistService } from './playlistService';
import { useRequireAuth } from '../../context/AuthContext';

const PLAY_SVG = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
    <path d="M8 5v14l11-7z" />
  </svg>
);

const PLAYLIST_ICON = (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" style={{ marginRight: '4px', verticalAlign: '-1.5px' }}>
    <path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H8V4h12v12z" />
  </svg>
);

//card tile displaying playlist thumbnail title and video count styled identically to video cards
const PlaylistCard = ({ playlist, isOwner, onPlaylistUpdated }) => {
  const navigate = useNavigate();
  const requireAuth = useRequireAuth();
  const [showEditModal, setShowEditModal] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const playlistId = playlist.playlistId || playlist.id;
  const title = playlist.name || playlist.title || 'Untitled Playlist';
  const ownerId = playlist.ownerId || playlist.uploaderId || playlist.userId;
  const creator = playlist.ownerUsername || playlist.uploaderUsername || playlist.creator || 'Unknown';
  const videoCount = typeof playlist.videoCount === 'number' ? playlist.videoCount : (playlist.videos?.length || 0);
  const isPrivate = playlist.privacy === 'PRIVATE' || playlist.isPublic === false;

  const handleCardClick = () => {
    navigate(`/playlist/${playlistId}`);
  };

  const handleEditClick = () => {
    setShowEditModal(true);
  };

  const handleDeleteClick = async () => {
    if (window.confirm('Are you sure you want to delete this playlist?')) {
      try {
        await playlistService.deletePlaylist(playlistId);
        if (onPlaylistUpdated) onPlaylistUpdated();
      } catch (err) {
        console.error('Failed to delete playlist:', err);
        alert('Failed to delete playlist.');
      }
    }
  };

  const handleReportClick = () => {
    requireAuth(() => {
      setShowReportModal(true);
    });
  };

  const menuItems = isOwner
    ? [
        { label: 'Edit', onClick: handleEditClick },
        { label: 'Delete', onClick: handleDeleteClick }
      ]
    : [
        { label: 'Report', onClick: handleReportClick }
      ];

  return (
    <>
      <article
        className="tile"
        data-cat="gaming"
        data-title={title.toLowerCase()}
        onClick={handleCardClick}
        style={{ cursor: 'pointer', position: 'relative' }}
      >
        {isMenuOpen && (
          <div
            style={{ position: 'absolute', inset: 0, zIndex: 9 }}
            onClick={(e) => { e.stopPropagation(); e.preventDefault(); }}
          />
        )}
        <div className="thumb">
          {menuItems.length > 0 && (
            <div
              style={{ position: 'absolute', top: '8px', right: '8px', zIndex: 10 }}
              onClick={(e) => { e.stopPropagation(); e.preventDefault(); }}
            >
              <OptionsMenu
                onToggle={(open) => setIsMenuOpen(open)}
                items={menuItems}
              />
            </div>
          )}

          {playlist.thumbnailUrl ? (
            <img
              src={getFullImageUrl(playlist.thumbnailUrl)}
              alt={title}
              className="thumb-img"
            />
          ) : (
            <div
              className="art gaming"
              style={{ backgroundPosition: '50% 50%' }}
            />
          )}

          {playlist.isHidden && (
            <span
              className="trending-badge"
              style={{
                backgroundColor: 'rgba(239, 68, 68, 0.9)',
                color: 'white',
                left: '8px',
                right: 'auto',
                border: '1px solid rgba(255, 255, 255, 0.2)'
              }}
            >
              🔒 Hidden
            </span>
          )}

          {isPrivate && (
            <span
              className="trending-badge"
              style={{
                backgroundColor: 'var(--surface-3, #2f2f3d)',
                color: 'var(--text, #F5F5F7)',
                left: playlist.isHidden ? '84px' : '8px',
                right: 'auto',
                border: '1px solid var(--border, rgba(255, 255, 255, 0.2))'
              }}
            >
              🔒 Private
            </span>
          )}

          <span className="duration mono" style={{ display: 'flex', alignItems: 'center' }}>
            {PLAYLIST_ICON}
            {videoCount} {videoCount === 1 ? 'video' : 'videos'}
          </span>
          <span className="play">{PLAY_SVG}</span>
        </div>

        <div className="tile-body">
          <h5>{title}</h5>
          <div className="tile-meta">
            {ownerId ? (
              <Link to={`/profile/${ownerId}`} className="creator" onClick={(e) => e.stopPropagation()}>
                {playlist.ownerAvatarUrl || playlist.uploaderAvatarUrl ? (
                  <img
                    src={getFullImageUrl(playlist.ownerAvatarUrl || playlist.uploaderAvatarUrl)}
                    alt={`${creator}'s avatar`}
                    className="creator-avatar"
                  />
                ) : (
                  <span className="creator-avatar-fallback">
                    {creator.charAt(0).toUpperCase()}
                  </span>
                )}
                <span className="creator-name">{creator}</span>
              </Link>
            ) : (
              <div className="creator">
                <span className="creator-avatar-fallback">
                  {creator.charAt(0).toUpperCase()}
                </span>
                <span className="creator-name">{creator}</span>
              </div>
            )}
            <span className="mono views-count">{videoCount} {videoCount === 1 ? 'video' : 'videos'}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span className="tag gaming">Playlist</span>
            {isPrivate && (
              <span className="tag" style={{ backgroundColor: 'var(--surface-3, #2f2f3d)', color: 'var(--text-dim, #9aa3c0)', border: '1px solid var(--border)' }}>
                Private
              </span>
            )}
          </div>
        </div>
      </article>

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

      {showReportModal && (
        <div onClick={(e) => { e.stopPropagation(); }}>
          <ReportModal
            reportableType="PLAYLIST"
            reportableId={playlistId}
            onClose={() => setShowReportModal(false)}
            onSuccess={() => setShowReportModal(false)}
          />
        </div>
      )}
    </>
  );
};

export default PlaylistCard;

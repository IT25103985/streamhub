import React, { useState, useEffect, useContext } from 'react';
import { Link } from 'react-router-dom';
import { getFullImageUrl } from '../utils/imageUtils';
import { formatDuration, getVideoDuration } from '../utils/durationUtils';
import OptionsMenu from './OptionsMenu';
import ReportModal from '../features/report/ReportModal';
import AddToPlaylistMenu from '../features/playlist/AddToPlaylistMenu';
import { AuthContext, useRequireAuth } from '../context/AuthContext';

const LABELS = { gaming: "Gaming", music: "Music", movies: "Movies", default: "Video" };

const PLAY_SVG = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
    <path d="M8 5v14l11-7z" />
  </svg>
);

//video preview card displaying thumbnail creator details and views
const VideoCard = ({ video, featured = false, onClick }) => {
  const { user } = useContext(AuthContext);
  const requireAuth = useRequireAuth();
  const [showReportModal, setShowReportModal] = useState(false);
  const [showAddMenu, setShowAddMenu] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const handleReportClick = () => {
    requireAuth(() => {
      setShowReportModal(true);
    });
  };

  const handleAddToPlaylistClick = () => {
    requireAuth(() => {
      setShowAddMenu(true);
    });
  };

  //fallback fields for varied dto structures
  const title = video.title || 'Untitled';
  const uploaderId = video.uploaderId || video.ownerId;
  const creator = video.uploaderName || video.uploaderUsername || video.ownerUsername || video.creator || 'Unknown';
  
  //resolve category whether object or string
  const isCategoryObj = video.category && typeof video.category === 'object';
  const catSlug = isCategoryObj 
    ? (video.category.slug || video.category.name?.toLowerCase() || '') 
    : (typeof video.category === 'string' ? video.category.toLowerCase() : '');
  const catName = isCategoryObj 
    ? (video.category.name || LABELS.default) 
    : (LABELS[catSlug] || video.category || LABELS.default);
  
  //default slug fallback for theme styles
  const styleCat = ['gaming', 'music', 'movies'].includes(catSlug) ? catSlug : 'gaming';
  
  const views = typeof video.viewCount === 'number' 
    ? new Intl.NumberFormat('en-US', { notation: "compact", compactDisplay: "short" }).format(video.viewCount) 
    : (video.viewCount || '0');

  const [realDuration, setRealDuration] = useState(() => {
    if (video.duration && video.duration !== '0:00') return video.duration;
    if (typeof video.durationSeconds === 'number' && video.durationSeconds > 0) {
      return formatDuration(video.durationSeconds);
    }
    return null;
  });

  useEffect(() => {
    if (realDuration && realDuration !== '0:00') return;
    const vidId = video.videoId || video.id;
    if (!vidId) return;

    let isMounted = true;
    getVideoDuration(vidId).then((dur) => {
      if (isMounted && dur) {
        setRealDuration(dur);
      }
    });

    return () => {
      isMounted = false;
    };
  }, [video.videoId, video.id, video.duration, video.durationSeconds, realDuration]);

  const displayDuration = realDuration || video.duration || '0:00';
  const trending = video.trending;
  const variant = (video.id || 0) % 3;

  //video card section
  return (
    <article 
      className="tile" 
      data-cat={styleCat} 
      data-title={title.toLowerCase()}
      onClick={(e) => {
        console.log('VideoCard clicked', video);
        if (onClick) onClick(e);
      }}
      style={{ cursor: onClick ? 'pointer' : 'default', position: 'relative' }}
    >
      {isMenuOpen && (
        <div 
          style={{ position: 'absolute', inset: 0, zIndex: 9 }}
          onClick={(e) => { e.stopPropagation(); e.preventDefault(); }}
        />
      )}
      <div className="thumb">
        <div 
          style={{ position: 'absolute', top: '8px', right: '8px', zIndex: 10 }}
          onClick={(e) => { e.stopPropagation(); e.preventDefault(); }}
        >
          <OptionsMenu 
            onToggle={(open) => setIsMenuOpen(open)}
            items={[
              { label: 'Add to playlist', onClick: handleAddToPlaylistClick },
              ...((!user || (user.userId !== uploaderId && user.username !== creator)) ? [
                { label: 'Report', onClick: handleReportClick }
              ] : [])
            ]} 
          />
        </div>
        
        {video.thumbnailUrl ? (
          <img 
            src={getFullImageUrl(video.thumbnailUrl)} 
            alt={title} 
            className="thumb-img" 
          />
        ) : (
          <div 
            className={`art ${styleCat}`} 
            style={{ 
              backgroundPosition: `${variant * 30}% ${variant * 20}%`
            }}
          />
        )}

        {trending && <span className="trending-badge">Hot</span>}
        {video.isHidden && (
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
        {video.privacy === 'PRIVATE' && (
          <span 
            className="trending-badge" 
            style={{ 
              backgroundColor: 'var(--surface-3, #2f2f3d)', 
              color: 'var(--text, #F5F5F7)',
              left: video.isHidden ? '84px' : '8px',
              right: 'auto',
              border: '1px solid var(--border, rgba(255, 255, 255, 0.2))'
            }}
          >
            🔒 Private
          </span>
        )}
        <span className="duration mono">{displayDuration}</span>
        <span className="play">{PLAY_SVG}</span>
      </div>
      <div className="tile-body">
        <h5>{title}</h5>
        <div className="tile-meta">
          <Link to={`/profile/${uploaderId}`} className="creator" onClick={(e) => e.stopPropagation()}>
            {video.uploaderAvatarUrl || video.ownerAvatarUrl ? (
              <img 
                src={getFullImageUrl(video.uploaderAvatarUrl || video.ownerAvatarUrl)} 
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
          <span className="mono views-count">{views} views</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span className={`tag ${styleCat}`}>{catName}</span>
          {video.privacy === 'PRIVATE' && (
            <span className="tag" style={{ backgroundColor: 'var(--surface-3, #2f2f3d)', color: 'var(--text-dim, #9aa3c0)', border: '1px solid var(--border)' }}>
              Private
            </span>
          )}
        </div>
      </div>
      {showReportModal && (
        <div onClick={(e) => { e.stopPropagation(); }}>
          <ReportModal 
            reportableType="VIDEO" 
            reportableId={video.videoId || video.id} 
            onClose={() => setShowReportModal(false)}
            onSuccess={() => setShowReportModal(false)}
          />
        </div>
      )}
      {showAddMenu && (
        <div onClick={(e) => { e.stopPropagation(); }}>
          <AddToPlaylistMenu 
            videoId={video.videoId || video.id} 
            onClose={() => setShowAddMenu(false)}
          />
        </div>
      )}
    </article>
  );
};

export default VideoCard;

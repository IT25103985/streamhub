import { Link } from 'react-router-dom';

const ICONS = { gaming: "🎮", music: "🎵", movies: "🎬", default: "📺" };
const LABELS = { gaming: "Gaming", music: "Music", movies: "Movies", default: "Video" };

const PLAY_SVG = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
    <path d="M8 5v14l11-7z" />
  </svg>
);

const VideoCard = ({ video, featured = false }) => {
  // Defensive fallbacks in case DTO fields are slightly different
  const title = video.title || 'Untitled';
  const creator = video.uploaderUsername || video.creator || 'Unknown';
  
  // Category might be an object or string
  const catSlug = typeof video.category === 'object' ? video.category.slug : video.category;
  const catName = typeof video.category === 'object' ? video.category.name : LABELS[catSlug] || LABELS.default;
  const icon = ICONS[catSlug] || ICONS.default;
  
  // Safe default for styling
  const styleCat = ['gaming', 'music', 'movies'].includes(catSlug) ? catSlug : 'gaming';
  
  const views = typeof video.viewCount === 'number' 
    ? new Intl.NumberFormat('en-US', { notation: "compact", compactDisplay: "short" }).format(video.viewCount) 
    : (video.viewCount || '0');

  const duration = video.duration || '0:00';
  const trending = video.trending;
  const variant = (video.id || 0) % 3;

  return (
    <article className="tile" data-cat={styleCat} data-title={title.toLowerCase()}>
      <div className="thumb">
        <div 
          className={`art ${styleCat}`} 
          style={{ backgroundPosition: `${variant * 30}% ${variant * 20}%` }}
        ></div>
        <span className="cat-icon" title={catName}>{icon}</span>
        {trending && <span className="trending-badge">🔥 Hot</span>}
        <span className="duration mono">{duration}</span>
        <span className="play">{PLAY_SVG}</span>
      </div>
      <div className="tile-body">
        <h5>{title}</h5>
        <div className="tile-meta">
          <span className="creator">{creator}</span>
          <span className="mono">{views} views</span>
        </div>
        <span className={`tag ${styleCat}`}>{catName}</span>
      </div>
    </article>
  );
};

export default VideoCard;

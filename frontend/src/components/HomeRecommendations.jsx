import React, { useState } from 'react';
import { getFullImageUrl } from '../utils/imageUtils';
import './HomeRecommendations.css';

const DEFAULT_RECOMMENDATIONS = [
  {
    id: 'rec-1',
    title: 'Spider Man',
    category: 'Science Fiction, Fantasy',
    thumbnail: 'https://images.unsplash.com/photo-1604200213928-ba3cf4fc8436?auto=format&fit=crop&w=300&q=80',
    avatars: [
      'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=60&q=80',
      'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=60&q=80'
    ]
  },
  {
    id: 'rec-2',
    title: 'Wonder Woman',
    category: 'Adventure, Action',
    thumbnail: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=300&q=80',
    avatars: [
      'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=60&q=80',
      'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=60&q=80'
    ]
  },
  {
    id: 'rec-3',
    title: 'Mortal Engines',
    category: 'Science Fiction , Suspense',
    thumbnail: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=300&q=80',
    isActive: true,
    avatars: [
      'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=60&q=80',
      'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=60&q=80'
    ]
  },
  {
    id: 'rec-4',
    title: 'Rampage',
    category: 'Adventure, Action',
    thumbnail: 'https://images.unsplash.com/photo-1563089145-599997674d42?auto=format&fit=crop&w=300&q=80',
    avatars: [
      'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=60&q=80',
      'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=60&q=80'
    ]
  }
];

//personalized video recommendation carousel on home page
const HomeRecommendations = ({ videos = [], onSelectVideo }) => {
  const [activeCardIndex, setActiveCardIndex] = useState(2);

  const displayList = videos && videos.length >= 2
    ? videos.slice(0, 4).map((v, i) => ({
        id: v.videoId || v.id,
        rawVideo: v,
        title: v.title,
        category: v.category 
          ? (typeof v.category === 'object' ? v.category.name : v.category) 
          : 'Trending, Action',
        thumbnail: v.thumbnailUrl 
          ? getFullImageUrl(v.thumbnailUrl) 
          : DEFAULT_RECOMMENDATIONS[i % DEFAULT_RECOMMENDATIONS.length].thumbnail,
        avatars: [
          v.uploaderAvatarUrl || v.ownerAvatarUrl 
            ? getFullImageUrl(v.uploaderAvatarUrl || v.ownerAvatarUrl) 
            : DEFAULT_RECOMMENDATIONS[i % DEFAULT_RECOMMENDATIONS.length].avatars[0],
          DEFAULT_RECOMMENDATIONS[i % DEFAULT_RECOMMENDATIONS.length].avatars[1]
        ]
      }))
    : DEFAULT_RECOMMENDATIONS;

  const handleCardClick = (item, idx) => {
    setActiveCardIndex(idx);
    if (item.rawVideo && onSelectVideo) {
      onSelectVideo(item.rawVideo.videoId || item.rawVideo.id);
    } else if (videos[idx] && onSelectVideo) {
      onSelectVideo(videos[idx].videoId || videos[idx].id);
    }
  };

  //recommendation logic carousel section
  return (
    <section className="home-recs-section">
      <div className="home-recs-header-box">
        <h3 className="home-recs-title">RECOMMENDED FOR YOU</h3>
        <p className="home-recs-sub">Tailored to what you enjoy watching</p>
      </div>

      <div className="home-recs-grid">
        {displayList.map((item, idx) => (
          <div 
            key={item.id || idx}
            className={`rec-card-compact ${idx === activeCardIndex ? 'active' : ''}`}
            onClick={() => handleCardClick(item, idx)}
          >
            {idx === activeCardIndex && <div className="rec-card-active-bar" />}

            <div className="rec-card-top-row">
              <div className="rec-card-thumb-wrap">
                <img src={item.thumbnail} alt={item.title} />
                <div className="rec-card-play-overlay">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="white">
                    <polygon points="5 3 19 12 5 21 5 3"/>
                  </svg>
                </div>
              </div>

              <div className="rec-card-avatar-stack">
                {item.avatars.map((av, avIdx) => (
                  <div 
                    key={avIdx} 
                    className="avatar-stack-item"
                    style={{ zIndex: 3 - avIdx }}
                  >
                    <img src={av} alt="Uploader" />
                  </div>
                ))}
              </div>
            </div>

            <div className="rec-card-meta">
              <h4 className="rec-card-title">{item.title}</h4>
              <span className="rec-card-genre">{item.category}</span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};

export default HomeRecommendations;

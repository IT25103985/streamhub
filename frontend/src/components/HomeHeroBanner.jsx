import React, { useState, useEffect } from 'react';
import { getFullImageUrl } from '../utils/imageUtils';
import './HomeHeroBanner.css';

const DEFAULT_FEATURED_SLIDES = [
  {
    id: 'hero-1',
    titlePrefix: 'The',
    titleMain: 'SOUL',
    titleSub: 'CONDUCTOR',
    rating: '7.8',
    language: 'English',
    backdrop: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1600&q=80',
    thumbnail: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=240&q=80',
    description: 'A mesmerizing journey into the mysterious depths of mind and soul.'
  },
  {
    id: 'hero-2',
    titlePrefix: 'Shadow',
    titleMain: 'HUNTER',
    titleSub: 'LEGACY',
    rating: '8.4',
    language: 'English',
    backdrop: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=1600&q=80',
    thumbnail: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=240&q=80',
    description: 'An epic tale of courage and destiny in a futuristic cyber world.'
  },
  {
    id: 'hero-3',
    titlePrefix: 'Beyond',
    titleMain: 'THE',
    titleSub: 'HORIZON',
    rating: '8.1',
    language: 'English',
    backdrop: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=1600&q=80',
    thumbnail: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=240&q=80',
    description: 'Discover untouched lands and ancient wonders across endless horizons.'
  }
];

//top hero carousel spotlighting featured video content
const HomeHeroBanner = ({ featuredVideos = [], onSelectVideo }) => {
  const [activeIndex, setActiveIndex] = useState(0);

  //use fetched videos for carousel slides when available
  const slides = featuredVideos.length >= 2 
    ? featuredVideos.slice(0, 4).map((v, i) => {
        const words = (v.title || 'Featured Video').split(' ');
        const prefix = words.length > 2 ? words.slice(0, 1).join(' ') : 'Featured';
        const main = words.length > 2 ? words.slice(1, 2).join(' ') : words[0] || 'WATCH';
        const sub = words.length > 2 ? words.slice(2).join(' ') : words.slice(1).join(' ') || 'NOW';
        return {
          id: v.videoId || v.id,
          rawVideo: v,
          titlePrefix: prefix,
          titleMain: main,
          titleSub: sub,
          rating: '7.8',
          language: 'English',
          backdrop: v.thumbnailUrl ? getFullImageUrl(v.thumbnailUrl) : DEFAULT_FEATURED_SLIDES[i % 3].backdrop,
          thumbnail: v.thumbnailUrl ? getFullImageUrl(v.thumbnailUrl) : DEFAULT_FEATURED_SLIDES[i % 3].thumbnail,
          description: v.description || 'Watch now on StreamHub'
        };
      })
    : DEFAULT_FEATURED_SLIDES;

  const currentSlide = slides[activeIndex] || slides[0];

  const handlePrev = (e) => {
    e.stopPropagation();
    setActiveIndex((prev) => (prev === 0 ? slides.length - 1 : prev - 1));
  };

  const handleNext = (e) => {
    e.stopPropagation();
    setActiveIndex((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
  };

  const handleSlideClick = () => {
    if (currentSlide.rawVideo && onSelectVideo) {
      onSelectVideo(currentSlide.rawVideo.videoId || currentSlide.rawVideo.id);
    } else if (featuredVideos[0] && onSelectVideo) {
      onSelectVideo(featuredVideos[0].videoId || featuredVideos[0].id);
    }
  };

  //home hero banner section
  return (
    <div className="home-hero-banner" onClick={handleSlideClick}>
      <div 
        className="hero-backdrop-img" 
        style={{ backgroundImage: `url(${currentSlide.backdrop})` }}
      />
      <div className="hero-backdrop-overlay" />

      <div className="hero-banner-content">
        <h1 className="hero-banner-title">
          <span className="hero-title-prefix">{currentSlide.titlePrefix}</span>{' '}
          <span className="hero-title-main">{currentSlide.titleMain}</span>
          <br />
          <span className="hero-title-sub">{currentSlide.titleSub}</span>
        </h1>

        <div className="hero-banner-meta">
          <div className="hero-badge-imdb">
            <span className="imdb-tag">IMDb</span>
            <span className="imdb-score">{currentSlide.rating}</span>
          </div>
          <span className="hero-meta-bullet">•</span>
          <div className="hero-badge-lang">
            <span className="flag-icon">🇺🇸</span>
            <span>{currentSlide.language}</span>
          </div>
        </div>
      </div>

      <div className="hero-carousel-nav" onClick={(e) => e.stopPropagation()}>
        <button className="carousel-nav-btn prev-btn" onClick={handlePrev} aria-label="Previous featured video">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>

        <div className="carousel-thumbs-row">
          {slides.map((s, idx) => (
            <div 
              key={s.id || idx}
              className={`carousel-thumb-card ${idx === activeIndex ? 'active' : ''}`}
              onClick={() => setActiveIndex(idx)}
            >
              <img src={s.thumbnail} alt={s.titleMain} />
              {idx === activeIndex && <div className="thumb-active-border" />}
            </div>
          ))}
        </div>

        <button className="carousel-nav-btn next-btn" onClick={handleNext} aria-label="Next featured video">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>
    </div>
  );
};

export default HomeHeroBanner;

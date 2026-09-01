import React, { useState, useEffect } from 'react';
import watchHistoryService from '../watchhistory/watchHistoryService';
import VideoCard from '../../components/VideoCard';
import VideoGridSkeleton from '../../components/skeletons/VideoGridSkeleton';

//trending video grid section visible to all users and guests
const TrendingSection = ({ limit = 4, onSelectVideo }) => {
  const [trendingVideos, setTrendingVideos] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchTrending = async () => {
    try {
      //fetch public platform trending videos
      const data = await watchHistoryService.getTrendingVideos(limit);
      setTrendingVideos(data || []);
    } catch (err) {
      console.error('Failed to load trending videos:', err);
      setTrendingVideos([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTrending();

    const handleUpdate = () => fetchTrending();
    window.addEventListener('streamhub-video-update', handleUpdate);
    window.addEventListener('focus', handleUpdate);

    return () => {
      window.removeEventListener('streamhub-video-update', handleUpdate);
      window.removeEventListener('focus', handleUpdate);
    };
  }, [limit]);

  if (!loading && trendingVideos.length === 0) {
    return null;
  }

  //render trending section with flame icon
  return (
    <section className="block" id="trending-section" style={{ marginBottom: '24px' }}>
      <div className="block-head" style={{ marginBottom: '14px' }}>
        <div>
          <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f97316" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z" />
            </svg>
            Trending
          </h3>
        </div>
      </div>
      <div className="block-inner">
        {loading && trendingVideos.length === 0 ? (
          <VideoGridSkeleton count={limit} id="trending-grid" />
        ) : (
          <div className="videos-grid home-4col-grid" id="trending-grid">
            {trendingVideos.map((video) => (
              <VideoCard
                key={video.videoId || video.id}
                video={video}
                onClick={() => onSelectVideo && onSelectVideo(video.videoId || video.id)}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
};

export default TrendingSection;

import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import watchHistoryService from '../watchhistory/watchHistoryService';
import VideoCard from '../../components/VideoCard';
import VideoGridSkeleton from '../../components/skeletons/VideoGridSkeleton';

//personalized recommendation video grid only mounted for authenticated users
const RecommendedSection = ({ limit = 4, onSelectVideo }) => {
  const { user, showInterestsModal } = useContext(AuthContext);
  const [recommendedVideos, setRecommendedVideos] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchRecommendations = async () => {
    //guard against guest execution and onboarding pending state
    if (!user || user.needsOnboarding || showInterestsModal) {
      setLoading(false);
      setRecommendedVideos([]);
      return;
    }
    try {
      //fetch personalized recommendations
      const data = await watchHistoryService.getRecommendations(limit);
      setRecommendedVideos(data || []);
    } catch (err) {
      console.error('Failed to load recommendations:', err);
      setRecommendedVideos([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    //only fetch if authenticated user exists and onboarding is complete
    if (!user || user.needsOnboarding || showInterestsModal) {
      setLoading(false);
      setRecommendedVideos([]);
      return;
    }

    fetchRecommendations();

    const handleUpdate = () => fetchRecommendations();
    window.addEventListener('streamhub-video-update', handleUpdate);
    window.addEventListener('focus', handleUpdate);

    return () => {
      window.removeEventListener('streamhub-video-update', handleUpdate);
      window.removeEventListener('focus', handleUpdate);
    };
  }, [user, limit, showInterestsModal]);

  //never render section for unauthenticated guests or while onboarding is pending
  if (!user || user.needsOnboarding || showInterestsModal || (!loading && recommendedVideos.length === 0)) {
    return null;
  }

  //render recommendations section with sparkle icon
  return (
    <section className="block" id="recommended-section" style={{ marginBottom: '24px' }}>
      <div className="block-head" style={{ marginBottom: '14px' }}>
        <div>
          <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6366f1" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2l2.4 7.4H22l-6 4.6 2.3 7-6.3-4.6-6.3 4.6 2.3-7-6-4.6h7.6z" />
            </svg>
            Recommended for you
          </h3>
        </div>
      </div>
      <div className="block-inner">
        {loading && recommendedVideos.length === 0 ? (
          <VideoGridSkeleton count={limit} id="recommended-grid" />
        ) : (
          <div className="videos-grid home-4col-grid" id="recommended-grid">
            {recommendedVideos.map((video) => (
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

export default RecommendedSection;

import React from 'react';
import VideoCardSkeleton from './VideoCardSkeleton';

//grid of videocardskeletons matching active grid columns
const VideoGridSkeleton = ({ count = 8, className = 'videos-grid home-4col-grid', id }) => {
  return (
    <div className={className} id={id} aria-busy="true" aria-label="Loading content">
      {Array.from({ length: count }).map((_, index) => (
        <VideoCardSkeleton key={`video-skeleton-${index}`} />
      ))}
    </div>
  );
};

export default VideoGridSkeleton;

import React, { useRef, useEffect } from 'react';
import axiosInstance from '../../api/axiosInstance';

//custom video player wrapper with resume position support
const VideoPlayer = ({ 
  videoId, 
  initialPosition = 0,
  onPlay,
  onPause,
  onEnded,
  onTimeUpdate
}) => {
  const videoRef = useRef(null);
  const hasSeekedInitial = useRef(false);

  useEffect(() => {
    hasSeekedInitial.current = false;
  }, [videoId]);

  if (!videoId) return null;

  const streamUrl = `${axiosInstance.defaults.baseURL}/videos/${videoId}/stream`;

  const handleLoadedMetadata = () => {
    if (videoRef.current && initialPosition > 0 && !hasSeekedInitial.current) {
      if (initialPosition < (videoRef.current.duration || Infinity) - 2) {
        videoRef.current.currentTime = initialPosition;
      }
      hasSeekedInitial.current = true;
    }
  };

  const handleTimeUpdate = (e) => {
    if (onTimeUpdate) {
      onTimeUpdate(e.target.currentTime, e.target.duration);
    }
  };

  //video player core section
  return (
    <div className="video-player-container">
      <video 
        ref={videoRef}
        controls 
        className="native-video-player"
        src={streamUrl}
        controlsList="nodownload"
        onLoadedMetadata={handleLoadedMetadata}
        onPlay={(e) => onPlay && onPlay(e.target.currentTime)}
        onPause={(e) => onPause && onPause(e.target.currentTime)}
        onEnded={(e) => onEnded && onEnded(e.target.currentTime)}
        onTimeUpdate={handleTimeUpdate}
      >
        Your browser does not support the video tag.
      </video>
    </div>
  );
};

export default VideoPlayer;

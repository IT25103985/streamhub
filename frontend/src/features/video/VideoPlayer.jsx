import React, { useRef, useEffect, useState, useCallback } from 'react';
import axiosInstance, { getToken } from '../../api/axiosInstance';
import './VideoPlayer.css';

const SPEED_OPTIONS = [0.5, 0.75, 1, 1.5, 2, 2.5, 3, 3.5, 4];

const formatTime = (timeInSeconds) => {
  if (isNaN(timeInSeconds) || !isFinite(timeInSeconds) || timeInSeconds < 0) {
    return '0:00';
  }
  const totalSeconds = Math.floor(timeInSeconds);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  const paddedSeconds = seconds < 10 ? `0${seconds}` : seconds;
  if (hours > 0) {
    const paddedMinutes = minutes < 10 ? `0${minutes}` : minutes;
    return `${hours}:${paddedMinutes}:${paddedSeconds}`;
  }
  return `${minutes}:${paddedSeconds}`;
};

const VideoPlayer = ({
  videoId,
  initialPosition = 0,
  autoPlay = false,
  onPlay,
  onPause,
  onEnded,
  onTimeUpdate
}) => {
  const videoRef = useRef(null);
  const wrapperRef = useRef(null);
  const seekTrackRef = useRef(null);
  const speedMenuRef = useRef(null);
  const hideControlsTimerRef = useRef(null);
  const hasSeekedInitial = useRef(false);

  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [bufferedPercent, setBufferedPercent] = useState(0);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [showSpeedMenu, setShowSpeedMenu] = useState(false);
  const [isDraggingSeek, setIsDraggingSeek] = useState(false);
  const [dragProgress, setDragProgress] = useState(0);

  // When videoId changes, reset initial seek flag and reset playback rate to 1x
  useEffect(() => {
    hasSeekedInitial.current = false;
    setCurrentTime(0);
    setDuration(0);
    setBufferedPercent(0);
    setShowSpeedMenu(false);
    setPlaybackRate(1);

    if (videoRef.current) {
      videoRef.current.playbackRate = 1;
    }
  }, [videoId]);

  // Fullscreen change listener
  useEffect(() => {
    const handleFullscreenChange = () => {
      const isCurrentFullscreen = !!(
        document.fullscreenElement ||
        document.webkitFullscreenElement ||
        document.mozFullScreenElement ||
        document.msFullscreenElement
      );
      setIsFullscreen(isCurrentFullscreen);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
    document.addEventListener('mozfullscreenchange', handleFullscreenChange);
    document.addEventListener('MSFullscreenChange', handleFullscreenChange);

    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
      document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
      document.removeEventListener('MSFullscreenChange', handleFullscreenChange);
    };
  }, []);

  // Outside click & Escape key to close speed menu
  useEffect(() => {
    if (!showSpeedMenu) return;

    const handleClickOutside = (e) => {
      if (speedMenuRef.current && !speedMenuRef.current.contains(e.target)) {
        setShowSpeedMenu(false);
      }
    };

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        setShowSpeedMenu(false);
      }
    };

    document.addEventListener('pointerdown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('pointerdown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [showSpeedMenu]);

  // Auto-hide controls logic
  const resetHideTimer = useCallback(() => {
    setShowControls(true);
    if (hideControlsTimerRef.current) {
      clearTimeout(hideControlsTimerRef.current);
    }
    // Only auto-hide if video is playing, seek is not being dragged, and speed menu is not open
    if (isPlaying && !showSpeedMenu && !isDraggingSeek) {
      hideControlsTimerRef.current = setTimeout(() => {
        setShowControls(false);
      }, 3000);
    }
  }, [isPlaying, showSpeedMenu, isDraggingSeek]);

  useEffect(() => {
    resetHideTimer();
    return () => {
      if (hideControlsTimerRef.current) {
        clearTimeout(hideControlsTimerRef.current);
      }
    };
  }, [resetHideTimer]);

  const handleMouseMove = () => {
    resetHideTimer();
  };

  const handleLoadedMetadata = () => {
    if (!videoRef.current) return;
    const dur = videoRef.current.duration || 0;
    setDuration(dur);

    if (initialPosition > 0 && !hasSeekedInitial.current) {
      if (initialPosition < dur - 2) {
        videoRef.current.currentTime = initialPosition;
      }
      hasSeekedInitial.current = true;
    }

    if (autoPlay) {
      videoRef.current.play().catch(() => {});
    }
  };

  const updateBuffered = () => {
    const video = videoRef.current;
    if (!video || !video.duration || video.duration === 0) return;
    try {
      const count = video.buffered.length;
      if (count > 0) {
        // Find buffer range covering current time
        for (let i = 0; i < count; i++) {
          if (video.buffered.start(i) <= video.currentTime && video.currentTime <= video.buffered.end(i)) {
            setBufferedPercent((video.buffered.end(i) / video.duration) * 100);
            return;
          }
        }
        // Fallback to highest buffered end
        setBufferedPercent((video.buffered.end(count - 1) / video.duration) * 100);
      }
    } catch {
      // Ignore buffer query errors
    }
  };

  const handleTimeUpdate = (e) => {
    if (!isDraggingSeek) {
      setCurrentTime(e.target.currentTime);
    }
    updateBuffered();
    if (onTimeUpdate) {
      onTimeUpdate(e.target.currentTime, e.target.duration);
    }
  };

  const handleVideoPlay = (e) => {
    setIsPlaying(true);
    if (onPlay) {
      onPlay(e.target.currentTime);
    }
  };

  const handleVideoPause = (e) => {
    setIsPlaying(false);
    setShowControls(true);
    if (onPause) {
      onPause(e.target.currentTime);
    }
  };

  const handleVideoEnded = (e) => {
    setIsPlaying(false);
    setShowControls(true);
    if (onEnded) {
      onEnded(e.target.currentTime);
    }
  };

  // Video element ratechange is the single source of truth for display
  const handleRateChange = () => {
    if (videoRef.current) {
      setPlaybackRate(videoRef.current.playbackRate);
    }
  };

  const togglePlay = () => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) {
      video.play().catch(() => {});
    } else {
      video.pause();
    }
  };

  const handleSkip = (seconds) => {
    const video = videoRef.current;
    if (!video) return;
    const dur = video.duration;
    if (isNaN(dur) || !isFinite(dur) || dur <= 0) return;

    const newTime = Math.min(Math.max(0, video.currentTime + seconds), dur);
    video.currentTime = newTime;
    setCurrentTime(newTime);
  };

  // Seek Drag & Click Handlers
  const calculateSeekTime = (clientX) => {
    if (!seekTrackRef.current || !videoRef.current) return 0;
    const rect = seekTrackRef.current.getBoundingClientRect();
    const pos = Math.min(Math.max(0, clientX - rect.left), rect.width);
    const percentage = rect.width > 0 ? pos / rect.width : 0;
    const dur = videoRef.current.duration || 0;
    return { percentage: percentage * 100, targetTime: percentage * dur };
  };

  const handleSeekPointerDown = (e) => {
    if (!videoRef.current || !videoRef.current.duration) return;
    e.preventDefault();
    setIsDraggingSeek(true);

    const { percentage, targetTime } = calculateSeekTime(e.clientX);
    setDragProgress(percentage);
    setCurrentTime(targetTime);

    const handlePointerMove = (moveEvent) => {
      const { percentage: curPercent, targetTime: curTime } = calculateSeekTime(moveEvent.clientX);
      setDragProgress(curPercent);
      setCurrentTime(curTime);
    };

    const handlePointerUp = (upEvent) => {
      setIsDraggingSeek(false);
      const { targetTime: finalTime } = calculateSeekTime(upEvent.clientX);
      if (videoRef.current) {
        videoRef.current.currentTime = finalTime;
        setCurrentTime(finalTime);
      }
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
  };

  // Volume & Mute Handlers
  const toggleMute = () => {
    const video = videoRef.current;
    if (!video) return;
    if (video.muted || video.volume === 0) {
      video.muted = false;
      const restoredVol = volume === 0 ? 1 : volume;
      video.volume = restoredVol;
      setIsMuted(false);
      setVolume(restoredVol);
    } else {
      video.muted = true;
      setIsMuted(true);
    }
  };

  const handleVolumeChange = (e) => {
    const newVol = parseFloat(e.target.value);
    const video = videoRef.current;
    if (!video) return;
    video.volume = newVol;
    setVolume(newVol);
    if (newVol === 0) {
      video.muted = true;
      setIsMuted(true);
    } else {
      video.muted = false;
      setIsMuted(false);
    }
  };

  // Speed Handler
  const handleSelectSpeed = (rate) => {
    if (videoRef.current) {
      videoRef.current.playbackRate = rate;
    }
    setShowSpeedMenu(false);
  };

  // Fullscreen Handler
  const toggleFullscreen = () => {
    const wrapper = wrapperRef.current;
    const video = videoRef.current;
    if (!wrapper) return;

    if (!document.fullscreenElement && !document.webkitFullscreenElement) {
      if (wrapper.requestFullscreen) {
        wrapper.requestFullscreen().catch(() => {
          if (video?.webkitEnterFullscreen) {
            video.webkitEnterFullscreen();
          }
        });
      } else if (wrapper.webkitRequestFullscreen) {
        wrapper.webkitRequestFullscreen();
      } else if (video?.webkitEnterFullscreen) {
        video.webkitEnterFullscreen();
      }
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen().catch(() => {});
      } else if (document.webkitExitFullscreen) {
        document.webkitExitFullscreen();
      }
    }
  };

  if (!videoId) return null;

  const token = getToken();
  const streamUrl = `${axiosInstance.defaults.baseURL}/videos/${videoId}/stream${token ? `?token=${encodeURIComponent(token)}` : ''}`;
  const playedPercentage = duration > 0 ? (isDraggingSeek ? dragProgress : (currentTime / duration) * 100) : 0;

  return (
    <div className="video-player-container">
      <div 
        ref={wrapperRef}
        className="video-player-wrapper"
        onMouseMove={handleMouseMove}
        onTouchStart={handleMouseMove}
        onMouseLeave={() => isPlaying && !showSpeedMenu && !isDraggingSeek && setShowControls(false)}
      >
        <video
          ref={videoRef}
          className="custom-video-element"
          src={streamUrl}
          playsInline
          onLoadedMetadata={handleLoadedMetadata}
          onTimeUpdate={handleTimeUpdate}
          onProgress={updateBuffered}
          onPlay={handleVideoPlay}
          onPause={handleVideoPause}
          onEnded={handleVideoEnded}
          onRateChange={handleRateChange}
          onClick={togglePlay}
        >
          Your browser does not support the video tag.
        </video>

        {/* Video Area overlay to catch clicks for play/pause toggle */}
        <div 
          className="video-screen-overlay" 
          onClick={togglePlay} 
          aria-label={isPlaying ? 'Pause video' : 'Play video'} 
        />

        {/* Custom Control Bar */}
        <div 
          className={`player-control-bar ${!showControls && isPlaying && !showSpeedMenu && !isDraggingSeek ? 'hidden' : ''}`}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Left Controls: Skip Back 10s, Play/Pause, Skip Forward 10s */}
          <div className="player-controls-left">
            <button 
              className="player-btn"
              onClick={() => handleSkip(-10)}
              aria-label="Skip backward 10 seconds"
              title="Skip back 10s"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                {/* 10s back icon */}
                <path d="M1 4v6h6" />
                <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
                <text x="12" y="15" fill="currentColor" stroke="none" fontSize="7" fontWeight="bold" textAnchor="middle">10</text>
              </svg>
            </button>

            <button 
              className="player-btn"
              onClick={togglePlay}
              aria-label={isPlaying ? 'Pause' : 'Play'}
              title={isPlaying ? 'Pause (Space)' : 'Play (Space)'}
            >
              {isPlaying ? (
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <rect x="6" y="4" width="4" height="16" rx="1.5" />
                  <rect x="14" y="4" width="4" height="16" rx="1.5" />
                </svg>
              ) : (
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <polygon points="5 3 19 12 5 21 5 3" />
                </svg>
              )}
            </button>

            <button 
              className="player-btn"
              onClick={() => handleSkip(10)}
              aria-label="Skip forward 10 seconds"
              title="Skip forward 10s"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                {/* 10s forward icon */}
                <path d="M23 4v6h-6" />
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
                <text x="12" y="15" fill="currentColor" stroke="none" fontSize="7" fontWeight="bold" textAnchor="middle">10</text>
              </svg>
            </button>
          </div>

          {/* Time Display */}
          <div className="player-time-display" aria-label="Playback time">
            <span className="player-time-current">{formatTime(currentTime)}</span>
            <span className="player-time-divider">/</span>
            <span className="player-time-duration">{formatTime(duration)}</span>
          </div>

          {/* Seek Bar */}
          <div 
            className={`player-seek-container ${isDraggingSeek ? 'dragging' : ''}`}
            ref={seekTrackRef}
            onPointerDown={handleSeekPointerDown}
            role="slider"
            aria-label="Seek video"
            aria-valuemin="0"
            aria-valuemax={duration || 0}
            aria-valuenow={currentTime}
            tabIndex="0"
            onKeyDown={(e) => {
              if (e.key === 'ArrowLeft') handleSkip(-5);
              if (e.key === 'ArrowRight') handleSkip(5);
            }}
          >
            <div className="player-seek-track">
              <div 
                className="player-seek-buffered" 
                style={{ width: `${Math.min(100, Math.max(0, bufferedPercent))}%` }} 
              />
              <div 
                className="player-seek-played" 
                style={{ width: `${Math.min(100, Math.max(0, playedPercentage))}%` }} 
              />
              <div 
                className="player-seek-thumb" 
                style={{ left: `${Math.min(100, Math.max(0, playedPercentage))}%` }} 
              />
            </div>
          </div>

          {/* Right Controls: Volume with Slider, Speed Menu, Fullscreen */}
          <div className="player-controls-right">
            {/* Volume Group */}
            <div className="player-volume-group">
              <button 
                className="player-btn"
                onClick={toggleMute}
                aria-label={isMuted || volume === 0 ? 'Unmute' : 'Mute'}
                title={isMuted || volume === 0 ? 'Unmute' : 'Mute'}
              >
                {isMuted || volume === 0 ? (
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="1" y1="1" x2="23" y2="23" />
                    <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6" />
                    <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a6.97 6.97 0 0 1-.5 2.5" />
                    <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" fill="currentColor" stroke="none" />
                    <line x1="23" y1="9" x2="17" y2="15" />
                    <line x1="17" y1="9" x2="23" y2="15" />
                  </svg>
                ) : volume < 0.5 ? (
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" fill="currentColor" stroke="none" />
                    <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" fill="currentColor" stroke="none" />
                    <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
                    <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
                  </svg>
                )}
              </button>

              <div className="player-volume-slider-wrap">
                <input 
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  value={isMuted ? 0 : volume}
                  onChange={handleVolumeChange}
                  className="player-volume-slider"
                  aria-label="Volume slider"
                />
              </div>
            </div>

            {/* Speed Button & Dropdown */}
            <div className="player-speed-menu-wrap" ref={speedMenuRef}>
              <button 
                className="player-btn player-speed-btn"
                onClick={() => setShowSpeedMenu(!showSpeedMenu)}
                aria-label={`Playback speed: ${playbackRate}x`}
                aria-haspopup="true"
                aria-expanded={showSpeedMenu}
                title="Playback Speed"
              >
                {playbackRate}x
              </button>

              {showSpeedMenu && (
                <div className="player-speed-menu" role="menu">
                  {SPEED_OPTIONS.map((rate) => (
                    <button
                      key={rate}
                      className={`player-speed-option ${playbackRate === rate ? 'active' : ''}`}
                      onClick={() => handleSelectSpeed(rate)}
                      role="menuitem"
                    >
                      <span>{rate}x</span>
                      {playbackRate === rate && (
                        <svg className="check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                      )}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Fullscreen Button */}
            <button 
              className="player-btn"
              onClick={toggleFullscreen}
              aria-label={isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen'}
              title={isFullscreen ? 'Exit Fullscreen (f)' : 'Enter Fullscreen (f)'}
            >
              {isFullscreen ? (
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3" />
                </svg>
              ) : (
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
                </svg>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VideoPlayer;

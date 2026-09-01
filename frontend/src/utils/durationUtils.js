//in-memory cache for resolved video durations
const durationCache = new Map();
const pendingRequests = new Map();

//formats numeric seconds into mm:ss or hh:mm:ss
export const formatDuration = (sec) => {
  if (typeof sec !== 'number' || isNaN(sec) || sec <= 0 || !isFinite(sec)) {
    return '0:00';
  }
  const totalSec = Math.floor(sec);
  const hours = Math.floor(totalSec / 3600);
  const minutes = Math.floor((totalSec % 3600) / 60);
  const seconds = totalSec % 60;

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
};

//fetches real-time video duration from metadata using native browser media loader
export const getVideoDuration = (videoId) => {
  if (!videoId) return Promise.resolve(null);

  const key = String(videoId);
  if (durationCache.has(key)) {
    return Promise.resolve(durationCache.get(key));
  }
  if (pendingRequests.has(key)) {
    return pendingRequests.get(key);
  }

  const promise = new Promise((resolve) => {
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.src = `http://localhost:8081/api/v1/videos/${key}/stream`;

    const cleanup = () => {
      video.removeAttribute('src');
      video.load();
      pendingRequests.delete(key);
    };

    video.onloadedmetadata = () => {
      const dur = video.duration;
      if (dur && !isNaN(dur) && dur > 0 && isFinite(dur)) {
        const formatted = formatDuration(dur);
        durationCache.set(key, formatted);
        cleanup();
        resolve(formatted);
      } else {
        cleanup();
        resolve(null);
      }
    };

    video.onerror = () => {
      cleanup();
      resolve(null);
    };

    //safety timeout
    setTimeout(() => {
      if (pendingRequests.has(key)) {
        cleanup();
        resolve(null);
      }
    }, 6000);
  });

  pendingRequests.set(key, promise);
  return promise;
};

import api from '../../api/axiosInstance';

const recordWatchHistory = async (videoId, positionSeconds = 0) => {
    const response = await api.post('/watch-history', {
        videoId,
        positionSeconds: Math.floor(positionSeconds || 0)
    });
    return response.data;
};

const getWatchHistory = async (page = 0, size = 20) => {
    const response = await api.get('/watch-history', {
        params: { page, size }
    });
    return response.data;
};

const clearWatchHistory = async () => {
    const response = await api.delete('/watch-history');
    return response.data;
};

const removeFromWatchHistory = async (videoId) => {
    const response = await api.delete(`/watch-history/${videoId}`);
    return response.data;
};

const getRecommendations = async (limit = 10) => {
    const response = await api.get('/watch-history/recommendations', {
        params: { limit }
    });
    return response.data;
};

const getResumePosition = async (videoId) => {
    const response = await api.get(`/videos/${videoId}/resume-position`);
    return response.data?.positionSeconds ?? 0;
};

const incrementViewCount = async (videoId) => {
    const response = await api.post(`/videos/${videoId}/view`);
    return response.data;
};

const getTrendingVideos = async (limit = 10) => {
    const response = await api.get('/videos/trending', {
        params: { limit }
    });
    return response.data;
};

const getAdminTrendingVideos = async (limit = 10) => {
    const response = await api.get('/admin/analytics/trending', {
        params: { limit }
    });
    return response.data;
};

//api helper for logging watch timestamps recommendations and trending videos
const watchHistoryService = {
    recordWatchHistory,
    getWatchHistory,
    clearWatchHistory,
    removeFromWatchHistory,
    getRecommendations,
    getTrendingVideos,
    getResumePosition,
    incrementViewCount,
    getAdminTrendingVideos
};

export default watchHistoryService;

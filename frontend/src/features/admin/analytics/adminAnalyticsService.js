import axiosInstance from '../../../api/axiosInstance';

//api client for admin analytics aggregated counts and trending videos
const adminAnalyticsService = {
    getAnalytics: async () => {
        const response = await axiosInstance.get('/admin/analytics');
        return response.data;
    },
    getTrendingVideos: async (limit = 10) => {
        const response = await axiosInstance.get('/admin/analytics/trending', {
            params: { limit }
        });
        return response.data;
    }
};

export default adminAnalyticsService;

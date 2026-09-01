import axiosInstance from './axiosInstance';

export //api service for notification counts and read status updates
const notificationApi = {
    getNotifications: async (page = 0, size = 20) => {
        const response = await axiosInstance.get('/notifications', {
            params: { page, size }
        });
        return response.data;
    },

    getUnreadCount: async () => {
        const response = await axiosInstance.get('/notifications/unread-count');
        return response.data;
    },

    markAsRead: async (id) => {
        const response = await axiosInstance.patch(`/notifications/${id}/read`);
        return response.data;
    },

    markAllAsRead: async () => {
        const response = await axiosInstance.patch('/notifications/read-all');
        return response.data;
    }
};

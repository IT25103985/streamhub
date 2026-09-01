import axiosInstance from './axiosInstance';

export const reportApi = {
    createReport: async (data) => {
        const response = await axiosInstance.post('/reports', data);
        return response.data;
    },

    getMyReports: async (page = 0, size = 10) => {
        const response = await axiosInstance.get('/reports/mine', {
            params: { page, size }
        });
        return response.data;
    },

    getReports: async (status, type, page = 0, size = 100) => {
        const params = { page, size };
        if (status && status !== 'ALL') params.status = status;
        if (type && type !== 'ALL') params.type = type;

        const response = await axiosInstance.get('/reports', { params });
        return response.data;
    },

    getReport: async (id) => {
        const response = await axiosInstance.get(`/reports/${id}`);
        return response.data;
    },

    updateStatus: async (id, status) => {
        const response = await axiosInstance.patch(`/reports/${id}/status`, { status });
        return response.data;
    },

    takeAction: async (id, action, actionReason) => {
        const response = await axiosInstance.post(`/reports/${id}/action`, { action, actionReason });
        return response.data;
    },

    reverseAction: async (id, reason) => {
        const response = await axiosInstance.post(`/reports/${id}/reverse`, { reason });
        return response.data;
    }
};

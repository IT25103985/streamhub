import axiosInstance from '../../api/axiosInstance';

//api service for creating reports and submitting moderator actions
export const reportService = {
  createReport: async (data) => {
    const response = await axiosInstance.post('/reports', data);
    return response.data;
  },
  
  getAllReportsAdmin: async (status, type, page = 0, size = 100) => {
    const params = { page, size };
    if (status && status !== 'ALL') params.status = status;
    if (type && type !== 'ALL') params.type = type;

    const response = await axiosInstance.get('/reports', { params });
    return response.data;
  },

  getReportDetails: async (reportId) => {
    const response = await axiosInstance.get(`/reports/${reportId}`);
    return response.data;
  },

  updateReportStatusAdmin: async (reportId, status) => {
    const response = await axiosInstance.patch(`/reports/${reportId}/status`, { status });
    return response.data;
  },

  takeAction: async (reportId, action, actionReason) => {
    const response = await axiosInstance.post(`/reports/${reportId}/action`, { action, actionReason });
    return response.data;
  },

  reverseActionAdmin: async (reportId, reason) => {
    const response = await axiosInstance.post(`/reports/${reportId}/reverse`, { reason });
    return response.data;
  }
};


import axiosInstance from '../../../api/axiosInstance';

//api client for fetching and updating user roles and statuses
const adminUserService = {
    getAllUsers: async () => {
        const response = await axiosInstance.get('/admin/users');
        return response.data;
    },
    
    updateUserRole: async (userId, role) => {
        const response = await axiosInstance.patch(`/admin/users/${userId}/role`, { role });
        return response.data;
    },
    
    updateUserStatus: async (userId, status) => {
        const response = await axiosInstance.patch(`/admin/users/${userId}/status`, { status });
        return response.data;
    },

    deleteUser: async (userId) => {
        const response = await axiosInstance.delete(`/admin/users/${userId}`);
        return response.data;
    }
};

export default adminUserService;

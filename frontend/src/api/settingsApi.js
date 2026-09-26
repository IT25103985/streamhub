import axiosInstance from './axiosInstance';

export const getSettings = async () => {
    const response = await axiosInstance.get('/settings');
    return response.data;
};

export const updateSettings = async (settings) => {
    const response = await axiosInstance.put('/settings', settings);
    return response.data;
};

export const deleteAccount = async () => {
    await axiosInstance.delete('/settings/account');
};

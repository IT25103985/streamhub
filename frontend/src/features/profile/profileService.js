import axios from '../../api/axiosInstance';

const getMyProfile = async () => {
    const response = await axios.get('/users/me/profile');
    return response.data;
};

const updateProfile = async (profileData) => {
    const response = await axios.patch('/users/me/profile', profileData);
    return response.data;
};

const uploadAvatar = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axios.post('/users/me/avatar', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
    return response.data;
};

const uploadCover = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axios.post('/users/me/cover', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
    return response.data;
};

export //api helper for user profile details and image uploads
const profileService = {
    getMyProfile,
    updateProfile,
    uploadAvatar,
    uploadCover
};

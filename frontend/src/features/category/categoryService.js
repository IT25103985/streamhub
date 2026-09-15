import axiosInstance from '../../api/axiosInstance';

export //api helper for category listing and creation requests
const getCategories = async (search = '') => {
    const url = search ? `/categories?search=${encodeURIComponent(search)}` : '/categories';
    const response = await axiosInstance.get(url);
    return response.data;
};

export const getCategoryById = async (id) => {
    const response = await axiosInstance.get(`/categories/${id}`);
    return response.data;
};

export const createCategory = async (categoryData) => {
    const response = await axiosInstance.post('/categories', categoryData);
    return response.data;
};

export const updateCategory = async (id, categoryData) => {
    const response = await axiosInstance.put(`/categories/${id}`, categoryData);
    return response.data;
};

export const deleteCategory = async (id) => {
    const response = await axiosInstance.delete(`/categories/${id}`);
    return response.data;
};

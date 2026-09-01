import api from './axiosInstance';

//fetches whether current user needs category interests onboarding
export const checkNeedsOnboarding = async () => {
  const response = await api.get('/users/me/needs-onboarding');
  return response.data;
};

//submits onboarding category preferences
export const saveCategoryPreferences = async (categoryIds) => {
  const response = await api.post('/users/me/category-preferences', { categoryIds });
  return response.data;
};

//marks onboarding as completed/skipped
export const skipOnboarding = async () => {
  const response = await api.post('/users/me/skip-onboarding');
  return response.data;
};


import axios from "axios";

const axiosInstance = axios.create({
  baseURL: "http://localhost:8081/api/v1",
  withCredentials: true // Important for sending/receiving the refresh token cookie
});

let inMemoryToken = null;

export const setToken = (token) => {
  inMemoryToken = token;
};

export const getToken = () => inMemoryToken;

axiosInstance.interceptors.request.use((config) => {
  if (inMemoryToken) {
    config.headers.Authorization = `Bearer ${inMemoryToken}`;
  }
  return config;
});

// Setup response interceptor for silent refresh
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry && originalRequest.url !== '/auth/refresh' && originalRequest.url !== '/auth/login') {
      originalRequest._retry = true;
      try {
        const res = await axiosInstance.post('/auth/refresh');
        setToken(res.data.token);
        originalRequest.headers.Authorization = `Bearer ${res.data.token}`;
        return axiosInstance(originalRequest);
      } catch (refreshError) {
        // Refresh failed, user needs to login again
        setToken(null);
        // We could dispatch an event here if we wanted to let AuthContext know
        window.dispatchEvent(new Event('auth-refresh-failed'));
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;

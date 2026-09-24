import axiosInstance from '../../api/axiosInstance';

export //api client helper for playlist operations and endpoints
const playlistService = {
  createPlaylist: async (formData) => {
    const response = await axiosInstance.post('/playlists', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  updatePlaylist: async (id, formData) => {
    const response = await axiosInstance.patch(`/playlists/${id}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  getUserPlaylists: async (onlyOwnVideos = false, search = '') => {
    let url = `/playlists/mine?onlyOwnVideos=${onlyOwnVideos}`;
    if (search) {
      url += `&search=${encodeURIComponent(search)}`;
    }
    const response = await axiosInstance.get(url);
    return response.data;
  },
  getPublicPlaylists: async () => {
    const response = await axiosInstance.get('/playlists/public');
    return response.data;
  },
  //searches public playlists by query and category filter with pagination
  searchPlaylists: async (params = {}) => {
    const response = await axiosInstance.get('/playlists/search', { params });
    return response.data;
  },
  getPlaylistsByOwner: async (ownerId, onlyOwnVideos = false, search = '') => {
    let url = `/playlists?ownerId=${ownerId}&onlyOwnVideos=${onlyOwnVideos}`;
    if (search) {
      url += `&search=${encodeURIComponent(search)}`;
    }
    const response = await axiosInstance.get(url);
    return response.data;
  },
  getPlaylistDetails: async (id) => {
    const response = await axiosInstance.get(`/playlists/${id}`);
    return response.data;
  },
  addVideoToPlaylist: async (playlistId, videoId) => {
    const response = await axiosInstance.post(`/playlists/${playlistId}/videos`, { videoId });
    return response.data;
  },
  removeVideoFromPlaylist: async (playlistId, videoId) => {
    const response = await axiosInstance.delete(`/playlists/${playlistId}/videos/${videoId}`);
    return response.data;
  },
  reorderVideos: async (playlistId, orderedVideoIds) => {
    const response = await axiosInstance.patch(`/playlists/${playlistId}/videos/reorder`, orderedVideoIds);
    return response.data;
  },
  deletePlaylist: async (id) => {
    const response = await axiosInstance.delete(`/playlists/${id}`);
    return response.data;
  },
  
  //admin playlist management api calls
  getAllPlaylistsAdmin: async () => {
    const response = await axiosInstance.get('/admin/playlists');
    return response.data;
  },
  togglePlaylistHiddenAdmin: async (id) => {
    const response = await axiosInstance.patch(`/admin/playlists/${id}/hide`);
    return response.data;
  },
  deletePlaylistAdmin: async (id) => {
    const response = await axiosInstance.delete(`/admin/playlists/${id}`);
    return response.data;
  }
};

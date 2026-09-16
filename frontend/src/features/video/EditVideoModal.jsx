import React, { useState, useEffect } from 'react';
import axios from '../../api/axiosInstance';
import { getFullImageUrl } from '../../utils/imageUtils';

//modal dialog for updating video title description and category
const EditVideoModal = ({ video, onClose, onSuccess }) => {
  const [title, setTitle] = useState(video.title || '');
  const [description, setDescription] = useState(video.description || '');
  const [thumbnail, setThumbnail] = useState(null);
  const [thumbnailPreview, setThumbnailPreview] = useState(null);
  const [categoryId, setCategoryId] = useState(video.category?.id || video.categoryId || '');
  const [privacy, setPrivacy] = useState(video.privacy || 'PUBLIC');
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    axios.get('/categories')
      .then(res => setCategories(res.data))
      .catch(err => console.error('Error fetching categories', err));
  }, []);

  const handleThumbnailChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const selectedThumbnail = e.target.files[0];
      setThumbnail(selectedThumbnail);
      setThumbnailPreview(URL.createObjectURL(selectedThumbnail));
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleThumbnailDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const selectedThumbnail = e.dataTransfer.files[0];
      setThumbnail(selectedThumbnail);
      setThumbnailPreview(URL.createObjectURL(selectedThumbnail));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setError("Title is required");
      return;
    }
    if (!categoryId) {
      setError("Category is required");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      let res = await axios.put(`/videos/${video.videoId}`, {
        title,
        description,
        categoryId: categoryId,
        privacy
      });
      
      if (thumbnail) {
        const formData = new FormData();
        formData.append('thumbnail', thumbnail);
        res = await axios.patch(`/videos/${video.videoId}/thumbnail`, formData);
      }
      
      onSuccess(res.data);
    } catch (err) {
      console.error(err);
      setError("Failed to update video. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 10000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div className="upload-container" onClick={e => e.stopPropagation()} style={{ width: '90%', maxWidth: '600px', maxHeight: '90vh', overflowY: 'auto' }}>
        <button className="modal-close" onClick={onClose} style={{ color: 'var(--text-dim)' }}>&times;</button>
        <div className="upload-header" style={{ textAlign: 'left', marginBottom: '20px' }}>
          <h2 style={{ margin: 0 }}>Edit Video</h2>
          <p style={{ margin: '5px 0 0 0', color: 'var(--text-secondary)' }}>Update your video details and thumbnail.</p>
        </div>
        
        {error && <div className="upload-error" style={{ marginBottom: '15px' }}>{error}</div>}

        <form onSubmit={handleSubmit} className="upload-form">
          <div className="form-group">
            <label htmlFor="title">Title</label>
            <input 
              type="text" 
              id="title" 
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Give your video a catchy title"
              disabled={loading}
              required
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea 
              id="description" 
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Tell viewers what this video is about"
              rows={4}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="categoryId">Category</label>
            <select 
              id="categoryId"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              disabled={loading}
              required
              style={{ width: '100%', padding: '12px', background: '#333', border: '1px solid #555', borderRadius: '4px', color: '#fff', outline: 'none' }}
            >
              <option value="" disabled>Select a category</option>
              {categories.map(cat => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="privacy">Privacy</label>
            <select 
              id="privacy"
              value={privacy}
              onChange={(e) => setPrivacy(e.target.value)}
              disabled={loading}
              style={{ width: '100%', padding: '12px', background: '#333', border: '1px solid #555', borderRadius: '4px', color: '#fff', outline: 'none' }}
            >
              <option value="PUBLIC">Public</option>
              <option value="PRIVATE">Private</option>
            </select>
          </div>

          <div className="form-group">
            <label>Thumbnail Image</label>
            <div style={{ marginBottom: '15px', color: 'var(--text-secondary)', fontSize: '14px' }}>Current Thumbnail:</div>
            <img 
                src={getFullImageUrl(video.thumbnailUrl)} 
                alt="Current thumbnail" 
                style={{ width: '100%', borderRadius: '8px', opacity: thumbnailPreview ? 0.3 : 1, marginBottom: '15px', maxHeight: '150px', objectFit: 'cover' }} 
            />
            
            <div 
              className={`file-drop-zone ${thumbnail ? 'has-file' : ''}`}
              onDragOver={handleDragOver}
              onDrop={handleThumbnailDrop}
              style={thumbnailPreview ? { padding: '10px' } : {}}
            >
              <input 
                type="file" 
                id="thumbnail-update" 
                accept="image/jpeg,image/png,image/webp" 
                onChange={handleThumbnailChange} 
                disabled={loading}
              />
              {!thumbnail ? (
                <div className="drop-content">
                  <div className="drop-icon">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                  </div>
                  <span className="drop-title">Drag & drop a new thumbnail</span>
                  <span className="drop-subtitle">or click to browse files</span>
                </div>
              ) : (
                <div className="drop-content">
                  <img src={thumbnailPreview} alt="New thumbnail preview" style={{ maxWidth: '100%', maxHeight: '150px', borderRadius: '8px' }} />
                  <span className="drop-title" style={{ marginTop: '10px' }}>{thumbnail.name}</span>
                  <span className="drop-subtitle">{(thumbnail.size / (1024 * 1024)).toFixed(2)} MB • Click to change file</span>
                </div>
              )}
            </div>
          </div>

          <div className="form-actions" style={{ marginTop: '24px' }}>
            <button 
              type="button" 
              className="btn btn-ghost" 
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditVideoModal;

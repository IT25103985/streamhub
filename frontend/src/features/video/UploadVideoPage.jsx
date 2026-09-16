import { useState, useContext, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axiosInstance from '../../api/axiosInstance';
import { AuthContext } from '../../context/AuthContext';

//video upload interface with thumbnail picker title and category selector
const UploadVideoPage = () => {
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();
  
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [privacy, setPrivacy] = useState('PUBLIC');
  const [categories, setCategories] = useState([]);
  const [file, setFile] = useState(null);
  const [thumbnail, setThumbnail] = useState(null);
  const [thumbnailPreview, setThumbnailPreview] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    axiosInstance.get('/categories')
      .then(res => setCategories(res.data))
      .catch(err => console.error('Error fetching categories', err));
  }, []);

  //redirect unauthenticated users to home
  if (!user) {
    return (
      <div className="upload-container" style={{ textAlign: 'center', marginTop: '100px' }}>
        <h2>Unauthorized</h2>
        <p>You must be registered to perform this action.</p>
        <button className="btn btn-primary" onClick={() => navigate('/')} style={{ marginTop: '20px' }}>Go Home</button>
      </div>
    );
  }

  const [durationSeconds, setDurationSeconds] = useState(null);

  const processVideoFile = (selectedFile) => {
    setFile(selectedFile);
    try {
      const tempVideo = document.createElement('video');
      tempVideo.preload = 'metadata';
      const objUrl = URL.createObjectURL(selectedFile);
      tempVideo.src = objUrl;
      tempVideo.onloadedmetadata = () => {
        URL.revokeObjectURL(objUrl);
        if (tempVideo.duration && !isNaN(tempVideo.duration) && isFinite(tempVideo.duration)) {
          setDurationSeconds(Math.round(tempVideo.duration));
        }
      };
      tempVideo.onerror = () => {
        URL.revokeObjectURL(objUrl);
      };
    } catch (ignored) {}
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      processVideoFile(e.target.files[0]);
    }
  };

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

  const handleDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      processVideoFile(e.dataTransfer.files[0]);
    }
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
    if (!title || !description || !categoryId || !file || !thumbnail) {
      setError('Please fill in all fields, select a category, and upload both a video file and a thumbnail.');
      return;
    }
    
    setIsUploading(true);
    setError('');
    
    const formData = new FormData();
    formData.append('title', title);
    formData.append('description', description);
    formData.append('categoryId', categoryId);
    formData.append('privacy', privacy);
    formData.append('file', file);
    formData.append('thumbnail', thumbnail);
    if (durationSeconds) {
      formData.append('durationSeconds', durationSeconds);
    }
    
    try {
      const response = await axiosInstance.post('/videos', formData);
      
      //broadcast custom event so open feeds update immediately
      window.dispatchEvent(new CustomEvent('streamhub-video-update', { detail: { videoId: response.data?.videoId } }));
      
      //redirect user back to main feed
      navigate(`/`);
    } catch (err) {
      console.error('Upload failed:', err);
      setError(err.response?.data?.message || 'Failed to upload video. Please try again.');
      setIsUploading(false);
    }
  };

  return (
    <div className="upload-page">
      <div className="upload-container">
        <div className="upload-header">
          <h2>Upload Video</h2>
          <p>Share your next great moment with the <Link to="/" onClick={() => { window.dispatchEvent(new CustomEvent('streamhub-reset-home')); window.scrollTo({ top: 0, behavior: 'smooth' }); }} style={{ color: 'inherit', textDecoration: 'none', fontWeight: 600 }}>Stream<span style={{ color: '#e50914' }}>Hub</span></Link> community.</p>
        </div>
        
        {error && <div className="upload-error">{error}</div>}
        
        <form onSubmit={handleSubmit} className="upload-form">
          <div className="form-group">
            <label htmlFor="title">Title</label>
            <input 
              type="text" 
              id="title" 
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Give your video a catchy title"
              disabled={isUploading}
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
              disabled={isUploading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="categoryId">Category</label>
            <select 
              id="categoryId"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              disabled={isUploading}
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
              disabled={isUploading}
              style={{ width: '100%', padding: '12px', background: '#333', border: '1px solid #555', borderRadius: '4px', color: '#fff', outline: 'none' }}
            >
              <option value="PUBLIC">Public</option>
              <option value="PRIVATE">Private</option>
            </select>
          </div>
          
          <div className="form-group">
            <label>Video File</label>
            <div 
              className={`file-drop-zone ${file ? 'has-file' : ''}`}
              onDragOver={handleDragOver}
              onDrop={handleDrop}
            >
              <input 
                type="file" 
                id="video-upload" 
                accept="video/*" 
                onChange={handleFileChange} 
                disabled={isUploading}
              />
              {!file ? (
                <div className="drop-content">
                  <div className="drop-icon">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                  </div>
                  <span className="drop-title">Drag & drop your video here</span>
                  <span className="drop-subtitle">or click to browse files</span>
                </div>
              ) : (
                <div className="drop-content">
                  <div className="drop-icon success">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                  </div>
                  <span className="drop-title">{file.name}</span>
                  <span className="drop-subtitle">{(file.size / (1024 * 1024)).toFixed(2)} MB • Click to change file</span>
                </div>
              )}
            </div>
          </div>
          
          <div className="form-group">
            <label>Thumbnail Image</label>
            <div 
              className={`file-drop-zone ${thumbnail ? 'has-file' : ''}`}
              onDragOver={handleDragOver}
              onDrop={handleThumbnailDrop}
              style={thumbnailPreview ? { padding: '10px' } : {}}
            >
              <input 
                type="file" 
                id="thumbnail-upload" 
                accept="image/jpeg,image/png,image/webp" 
                onChange={handleThumbnailChange} 
                disabled={isUploading}
              />
              {!thumbnail ? (
                <div className="drop-content">
                  <div className="drop-icon">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                  </div>
                  <span className="drop-title">Drag & drop a thumbnail image</span>
                  <span className="drop-subtitle">or click to browse files</span>
                </div>
              ) : (
                <div className="drop-content">
                  <img src={thumbnailPreview} alt="Thumbnail preview" style={{ maxWidth: '100%', maxHeight: '200px', borderRadius: '8px' }} />
                  <span className="drop-title" style={{ marginTop: '10px' }}>{thumbnail.name}</span>
                  <span className="drop-subtitle">{(thumbnail.size / (1024 * 1024)).toFixed(2)} MB • Click to change file</span>
                </div>
              )}
            </div>
          </div>
          
          <div className="form-actions">
            <button 
              type="button" 
              className="btn btn-ghost" 
              onClick={() => navigate('/')}
              disabled={isUploading}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-primary upload-submit-btn" 
              disabled={isUploading || !title || !description || !categoryId || !file || !thumbnail}
            >
              {isUploading ? (
                <>
                  <span className="spinner"></span>
                  Uploading...
                </>
              ) : (
                <>Upload Video</>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UploadVideoPage;

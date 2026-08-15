import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosInstance from '../../api/axiosInstance';
import { AuthContext } from '../../context/AuthContext';

const UploadVideoPage = () => {
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();
  
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [file, setFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState('');
  
  // Basic check: only creators or admins can upload
  if (!user || (user.role !== 'CONTENT_CREATOR' && user.role !== 'ADMINISTRATOR' && user.role !== 'ADMIN')) {
    return (
      <div className="upload-container" style={{ textAlign: 'center', marginTop: '100px' }}>
        <h2>Unauthorized</h2>
        <p>You must be a Content Creator or Admin to upload videos.</p>
        <button className="btn btn-primary" onClick={() => navigate('/')} style={{ marginTop: '20px' }}>Go Home</button>
      </div>
    );
  }

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setFile(e.dataTransfer.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title || !description || !file) {
      setError('Please fill in all fields and select a video file.');
      return;
    }
    
    setIsUploading(true);
    setError('');
    
    const formData = new FormData();
    formData.append('title', title);
    formData.append('description', description);
    formData.append('file', file);
    
    try {
      // The backend expects a POST to /api/v1/videos with multipart/form-data
      const response = await axiosInstance.post('/videos', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      
      // Navigate to the video details page (or home if no detail page yet)
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
          <p>Share your next great moment with the StreamHub community.</p>
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
              className="btn primary upload-submit-btn" 
              disabled={isUploading || !title || !description || !file}
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

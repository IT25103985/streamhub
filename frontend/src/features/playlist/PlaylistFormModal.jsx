import React, { useState, useEffect, useRef } from 'react';
import ReactDOM from 'react-dom';
import { playlistService } from './playlistService';
import { getFullImageUrl } from '../../utils/imageUtils';
import '../profile/Profile.css';

//modal dialog to create or edit a playlist name description and cover art
const PlaylistFormModal = ({ playlist, onClose, onSaveSuccess }) => {
    const isEditMode = !!playlist;
    const fileInputRef = useRef(null);
    
    const [name, setName] = useState(playlist?.name || '');
    const [privacy, setPrivacy] = useState(playlist?.privacy || 'PUBLIC');
    const [description, setDescription] = useState(playlist?.description || '');
    const [thumbnailFile, setThumbnailFile] = useState(null);
    const [previewUrl, setPreviewUrl] = useState(null);
    const [removeThumbnail, setRemoveThumbnail] = useState(false);
    
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

    useEffect(() => {
        if (thumbnailFile) {
            const objectUrl = URL.createObjectURL(thumbnailFile);
            setPreviewUrl(objectUrl);
            return () => URL.revokeObjectURL(objectUrl);
        } else {
            setPreviewUrl(null);
        }
    }, [thumbnailFile]);

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!validTypes.includes(file.type)) {
            setError('Thumbnail must be JPG, PNG, or WEBP.');
            return;
        }

        if (file.size > 2 * 1024 * 1024) {
            setError('Thumbnail must be less than 2MB.');
            return;
        }

        setError('');
        setThumbnailFile(file);
        setRemoveThumbnail(false);
    };

    const handleRemoveThumbnail = () => {
        setThumbnailFile(null);
        setPreviewUrl(null);
        setRemoveThumbnail(true);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleSave = async (e) => {
        e.preventDefault();
        
        if (!name.trim()) {
            setError("Playlist title is required.");
            return;
        }

        setLoading(true);
        setError('');

        try {
            const formData = new FormData();
            
            const dataBlob = new Blob([JSON.stringify({
                name: name.trim(),
                privacy,
                description: description ? description.trim() : '',
                removeThumbnail
            })], { type: 'application/json' });
            
            formData.append('data', dataBlob);
            
            if (thumbnailFile) {
                formData.append('thumbnail', thumbnailFile);
            }

            if (isEditMode) {
                await playlistService.updatePlaylist(playlist.playlistId, formData);
            } else {
                await playlistService.createPlaylist(formData);
            }

            onSaveSuccess();
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || 'Failed to save playlist. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async () => {
        setLoading(true);
        try {
            await playlistService.deletePlaylist(playlist.playlistId);
            onSaveSuccess();
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || 'Failed to delete playlist.');
            setLoading(false);
            setShowDeleteConfirm(false);
        }
    };

    return ReactDOM.createPortal(
        <div className="profile-modal-overlay" onClick={(e) => e.stopPropagation()} style={{ zIndex: 10000 }}>
            <div className="profile-modal-content" onClick={(e) => e.stopPropagation()}>
                <h2 className="profile-modal-title">{isEditMode ? 'Edit Playlist' : 'Create Playlist'}</h2>
                
                {error && <div className="error-text" style={{marginBottom: '16px'}}>{error}</div>}

                <form onSubmit={handleSave}>
                    <div className="profile-form-group">
                        <label>Title <span style={{color: 'red'}}>*</span></label>
                        <input 
                            type="text" 
                            value={name} 
                            onChange={(e) => setName(e.target.value)}
                            className="profile-form-control"
                            required
                        />
                    </div>

                    <div className="profile-form-group">
                        <label>Privacy</label>
                        <select 
                            value={privacy} 
                            onChange={(e) => setPrivacy(e.target.value)}
                            className="profile-form-control"
                        >
                            <option value="PUBLIC">Public</option>
                            <option value="PRIVATE">Private</option>
                        </select>
                    </div>

                    <div className="profile-form-group">
                        <label>Description</label>
                        <textarea 
                            value={description} 
                            onChange={(e) => setDescription(e.target.value)}
                            className="profile-form-control"
                            rows="3"
                        />
                    </div>

                    <div className="profile-form-group">
                        <label>Custom Thumbnail (Max 2MB)</label>
                        {previewUrl ? (
                            <div style={{ marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                                <img src={previewUrl} alt="Thumbnail preview" style={{ width: '120px', aspectRatio: '16/9', objectFit: 'cover', borderRadius: '6px', border: '1px solid var(--border-color, rgba(255,255,255,0.2))' }} />
                                <button type="button" onClick={handleRemoveThumbnail} style={{ color: '#ef4444', border: 'none', background: 'none', cursor: 'pointer', fontSize: '13px', fontWeight: '500' }}>Remove image</button>
                            </div>
                        ) : (isEditMode && playlist.thumbnailUrl && !removeThumbnail && (
                            <div style={{ marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                                <img src={getFullImageUrl(playlist.thumbnailUrl)} alt="Current thumbnail" style={{ width: '120px', aspectRatio: '16/9', objectFit: 'cover', borderRadius: '6px', border: '1px solid var(--border-color, rgba(255,255,255,0.2))' }} />
                                <button type="button" onClick={handleRemoveThumbnail} style={{ color: '#ef4444', border: 'none', background: 'none', cursor: 'pointer', fontSize: '13px', fontWeight: '500' }}>Remove</button>
                            </div>
                        ))}
                        <input 
                            ref={fileInputRef}
                            type="file" 
                            accept="image/jpeg, image/png, image/webp" 
                            onChange={handleFileChange}
                            className="profile-form-control"
                        />
                    </div>

                    <div className="profile-form-actions" style={{ justifyContent: 'space-between', display: 'flex' }}>
                        <div>
                            {isEditMode && (
                                <>
                                    {!showDeleteConfirm ? (
                                        <button type="button" className="btn-cancel" onClick={() => setShowDeleteConfirm(true)} disabled={loading} style={{ color: 'red' }}>
                                            Delete Playlist
                                        </button>
                                    ) : (
                                        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                                            <span style={{ fontSize: '14px', color: 'red' }}>Are you sure?</span>
                                            <button type="button" className="btn btn-primary" onClick={handleDelete} disabled={loading} style={{ backgroundColor: 'red', borderColor: 'red' }}>
                                                Yes, Delete
                                            </button>
                                            <button type="button" className="btn-cancel" onClick={() => setShowDeleteConfirm(false)} disabled={loading}>
                                                Cancel
                                            </button>
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                        
                        <div style={{ display: 'flex', gap: '8px' }}>
                            <button type="button" className="btn-cancel" onClick={onClose} disabled={loading}>
                                Cancel
                            </button>
                            <button type="submit" className="btn btn-primary btn-save" disabled={loading}>
                                {loading ? 'Saving...' : (isEditMode ? 'Save' : 'Create')}
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>,
        document.body
    );
};

export default PlaylistFormModal;

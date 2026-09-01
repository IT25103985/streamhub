import React, { useState, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import { profileService } from './profileService';
import './Profile.css';

//modal popup for editing profile display name bio and avatars
const EditProfileModal = ({ profile, onClose, onSaveSuccess }) => {
    const { updateUserAvatar } = useContext(AuthContext);
    const [firstName, setFirstName] = useState(profile?.firstName || '');
    const [lastName, setLastName] = useState(profile?.lastName || '');
    const [bio, setBio] = useState(profile?.bio || '');
    const [avatarFile, setAvatarFile] = useState(null);
    const [coverFile, setCoverFile] = useState(null);
    
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleFileChange = (e, setFile, type, maxSizeMB) => {
        const file = e.target.files[0];
        if (!file) return;

        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!validTypes.includes(file.type)) {
            setError(`${type} must be JPG, PNG, or WEBP.`);
            return;
        }

        if (file.size > maxSizeMB * 1024 * 1024) {
            setError(`${type} must be less than ${maxSizeMB}MB.`);
            return;
        }

        setError('');
        setFile(file);
    };

    const handleSave = async (e) => {
        e.preventDefault();
        
        if (!firstName.trim() || !lastName.trim()) {
            setError("First name and Last name are required.");
            return;
        }

        if (bio.length > 300) {
            setError("Bio must be less than 300 characters.");
            return;
        }

        setLoading(true);
        setError('');

        try {
                  await profileService.updateProfile({ firstName, lastName, bio });

            //upload changed avatar or cover images
            if (avatarFile) {
                const response = await profileService.uploadAvatar(avatarFile);
                updateUserAvatar(response.avatarUrl);
            }
            if (coverFile) {
                await profileService.uploadCover(coverFile);
            }

            onSaveSuccess();
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || 'Failed to update profile. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="profile-modal-overlay">
            <div className="profile-modal-content">
                <h2 className="profile-modal-title">Edit Profile</h2>
                
                {error && <div className="error-text" style={{marginBottom: '16px'}}>{error}</div>}

                <form onSubmit={handleSave}>
                    <div className="profile-form-group">
                        <label>Avatar (Max 2MB)</label>
                        <input 
                            type="file" 
                            accept="image/jpeg, image/png, image/webp" 
                            onChange={(e) => handleFileChange(e, setAvatarFile, 'Avatar', 2)}
                            className="profile-form-control"
                        />
                    </div>

                    <div className="profile-form-group">
                        <label>Cover Picture (Max 5MB)</label>
                        <input 
                            type="file" 
                            accept="image/jpeg, image/png, image/webp" 
                            onChange={(e) => handleFileChange(e, setCoverFile, 'Cover Picture', 5)}
                            className="profile-form-control"
                        />
                    </div>

                    <div style={{display: 'flex', gap: '16px'}}>
                        <div className="profile-form-group" style={{flex: 1}}>
                            <label>First Name</label>
                            <input 
                                type="text" 
                                value={firstName} 
                                onChange={(e) => setFirstName(e.target.value)}
                                className="profile-form-control"
                                required
                            />
                        </div>
                        <div className="profile-form-group" style={{flex: 1}}>
                            <label>Last Name</label>
                            <input 
                                type="text" 
                                value={lastName} 
                                onChange={(e) => setLastName(e.target.value)}
                                className="profile-form-control"
                                required
                            />
                        </div>
                    </div>

                    <div className="profile-form-group">
                        <label>Email (Cannot be changed here)</label>
                        <input 
                            type="email" 
                            value={profile?.email || ''} 
                            className="profile-form-control"
                            disabled
                        />
                    </div>

                    <div className="profile-form-group">
                        <label>Bio</label>
                        <textarea 
                            value={bio} 
                            onChange={(e) => setBio(e.target.value)}
                            className="profile-form-control"
                            rows="4"
                        />
                        <span className="char-counter">
                            {bio.length} / 300
                        </span>
                    </div>

                    <div className="profile-form-actions">
                        <button type="button" className="btn-cancel" onClick={onClose} disabled={loading}>
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-primary btn-save" disabled={loading}>
                            {loading ? 'Saving...' : 'Save Changes'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default EditProfileModal;

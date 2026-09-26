import React, { useState, useEffect, useContext } from 'react';
import { getSettings, updateSettings, deleteAccount } from '../api/settingsApi';
import { AuthContext } from '../context/AuthContext';
import '../styles/SettingsPage.css';

const SettingsPage = () => {
    const { user, logout } = useContext(AuthContext);

    const [settings, setSettings] = useState({
        watchHistoryEnabled: true,
        notifySocial: true,
        notifyModeration: true,
        notifySystem: true
    });

    const [autoplay, setAutoplay] = useState(true);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [saveStatus, setSaveStatus] = useState(null);
    const [isSaving, setIsSaving] = useState(false);

    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [deleteConfirmText, setDeleteConfirmText] = useState('');
    const [isDeleting, setIsDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState(null);

    useEffect(() => {
        const localAutoplay = localStorage.getItem('autoplay_enabled');
        if (localAutoplay !== null) setAutoplay(localAutoplay === 'true');

        const fetchSettings = async () => {
            try {
                if (user) {
                    const data = await getSettings();
                    setSettings({
                        watchHistoryEnabled: data.watchHistoryEnabled,
                        notifySocial: data.notifySocial,
                        notifyModeration: data.notifyModeration,
                        notifySystem: data.notifySystem
                    });
                }
            } catch (err) {
                console.error('Failed to load settings:', err);
                setError('Failed to load your settings. Please try again later.');
            } finally {
                setLoading(false);
            }
        };
        fetchSettings();
    }, [user]);

    const showSaveStatus = (message) => {
        setSaveStatus(message);
        setTimeout(() => setSaveStatus(null), 3000);
    };

    const handleLocalToggle = (key, value) => {
        if (key === 'autoplay') {
            setAutoplay(value);
            localStorage.setItem('autoplay_enabled', value);
            showSaveStatus('Local settings updated');
        }
    };

    const handleBackendToggle = async (key) => {
        if (isSaving) return;
        const newValue = !settings[key];
        const newSettings = { ...settings, [key]: newValue };
        setSettings(newSettings);
        setIsSaving(true);
        try {
            await updateSettings(newSettings);
            showSaveStatus('Settings saved successfully');
        } catch (err) {
            console.error('Failed to update settings:', err);
            setSettings({ ...settings, [key]: !newValue });
            setError('Failed to save changes. Please try again.');
            setTimeout(() => setError(null), 3000);
        } finally {
            setIsSaving(false);
        }
    };

    const openDeleteModal = () => {
        setDeleteConfirmText('');
        setDeleteError(null);
        setShowDeleteModal(true);
    };

    const closeDeleteModal = () => {
        if (isDeleting) return;
        setShowDeleteModal(false);
        setDeleteConfirmText('');
        setDeleteError(null);
    };

    const handleDeleteAccount = async () => {
        if (deleteConfirmText !== 'DELETE') {
            setDeleteError('Please type DELETE exactly to confirm.');
            return;
        }
        setIsDeleting(true);
        setDeleteError(null);
        try {
            await deleteAccount();
            await logout();
        } catch (err) {
            console.error('Failed to delete account:', err);
            setDeleteError('Something went wrong. Please try again.');
            setIsDeleting(false);
        }
    };

    if (loading) return <div className="settings-loading">Loading settings...</div>;
    if (!user)   return <div className="settings-error">Please log in to view your settings.</div>;

    return (
        <div className="settings-page">
            <div className="settings-header">
                <h1>Account Settings</h1>
                <p>Manage your preferences, privacy, and notifications.</p>
            </div>

            {error && <div className="settings-error">{error}</div>}

            <div className="settings-section">
                <h2>
                    <svg style={{width:'1.5rem',height:'1.5rem',marginRight:'0.5rem'}} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                    Playback
                </h2>
                <div className="setting-item">
                    <div className="setting-info">
                        <h3>Autoplay Next Video</h3>
                        <p>Automatically play the next related video when the current one finishes. This setting is stored locally on this device.</p>
                    </div>
                    <label className="toggle-switch">
                        <input type="checkbox" checked={autoplay} onChange={(e) => handleLocalToggle('autoplay', e.target.checked)} />
                        <span className="toggle-slider"></span>
                    </label>
                </div>
            </div>

            <div className="settings-section">
                <h2>
                    <svg style={{width:'1.5rem',height:'1.5rem',marginRight:'0.5rem'}} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                    Privacy &amp; History
                </h2>
                <div className="setting-item">
                    <div className="setting-info">
                        <h3>Watch History</h3>
                        <p>Keep track of the videos you watch. This improves your recommendations. Disabling this will stop recording new views.</p>
                    </div>
                    <label className="toggle-switch">
                        <input type="checkbox" checked={settings.watchHistoryEnabled} onChange={() => handleBackendToggle('watchHistoryEnabled')} disabled={isSaving} />
                        <span className="toggle-slider"></span>
                    </label>
                </div>
            </div>

            <div className="settings-section">
                <h2>
                    <svg style={{width:'1.5rem',height:'1.5rem',marginRight:'0.5rem'}} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>
                    Notifications
                </h2>
                <div className="setting-item">
                    <div className="setting-info">
                        <h3>Social Interactions</h3>
                        <p>Get notified when someone follows you, or likes/replies to your comments and videos.</p>
                    </div>
                    <label className="toggle-switch">
                        <input type="checkbox" checked={settings.notifySocial} onChange={() => handleBackendToggle('notifySocial')} disabled={isSaving} />
                        <span className="toggle-slider"></span>
                    </label>
                </div>
                <div className="setting-item">
                    <div className="setting-info">
                        <h3>Moderation Alerts</h3>
                        <p>Receive important alerts regarding your account standing, warnings, and content removals.</p>
                    </div>
                    <label className="toggle-switch">
                        <input type="checkbox" checked={settings.notifyModeration} onChange={() => handleBackendToggle('notifyModeration')} disabled={isSaving} />
                        <span className="toggle-slider"></span>
                    </label>
                </div>
                <div className="setting-item">
                    <div className="setting-info">
                        <h3>System Updates</h3>
                        <p>Stay informed about new features, maintenance, and platform announcements.</p>
                    </div>
                    <label className="toggle-switch">
                        <input type="checkbox" checked={settings.notifySystem} onChange={() => handleBackendToggle('notifySystem')} disabled={isSaving} />
                        <span className="toggle-slider"></span>
                    </label>
                </div>
            </div>

            <div className="settings-section danger-zone-section">
                <h2 className="danger-zone-heading">
                    <svg style={{width:'1.5rem',height:'1.5rem',marginRight:'0.5rem'}} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>
                    Danger Zone
                </h2>
                <div className="setting-item danger-item">
                    <div className="setting-info">
                        <h3 className="danger-title">Delete Account</h3>
                        <p>Permanently remove your account and all associated data including videos, comments, playlists, followers and watch history. This cannot be undone.</p>
                    </div>
                    <button id="delete-account-btn" className="danger-btn" onClick={openDeleteModal}>
                        Delete Account
                    </button>
                </div>
            </div>

            {saveStatus && <div className="save-status">{saveStatus}</div>}

            {showDeleteModal && (
                <div className="delete-modal-overlay" onClick={closeDeleteModal}>
                    <div className="delete-modal" onClick={e => e.stopPropagation()}>
                        <div className="delete-modal-icon">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                            </svg>
                        </div>
                        <h2 className="delete-modal-title">Delete Your Account?</h2>
                        <p className="delete-modal-body">
                            This will permanently erase <strong>everything</strong> tied to <strong>@{user.username}</strong>:
                        </p>
                        <ul className="delete-modal-list">
                            <li>All uploaded videos and thumbnails</li>
                            <li>All comments, replies and likes</li>
                            <li>All playlists and watch history</li>
                            <li>All followers and following relationships</li>
                            <li>Your account profile and settings</li>
                        </ul>
                        <p className="delete-modal-warn">There is no way to recover this account after deletion.</p>
                        <label className="delete-modal-label">Type <strong>DELETE</strong> to confirm</label>
                        <input
                            id="delete-confirm-input"
                            className={`delete-modal-input${deleteConfirmText === 'DELETE' ? ' input-valid' : ''}`}
                            type="text"
                            value={deleteConfirmText}
                            onChange={e => setDeleteConfirmText(e.target.value)}
                            placeholder="DELETE"
                            disabled={isDeleting}
                            autoFocus
                        />
                        {deleteError && <p className="delete-modal-error">{deleteError}</p>}
                        <div className="delete-modal-actions">
                            <button className="delete-modal-cancel" onClick={closeDeleteModal} disabled={isDeleting}>Cancel</button>
                            <button
                                id="delete-confirm-btn"
                                className="delete-modal-confirm"
                                onClick={handleDeleteAccount}
                                disabled={isDeleting || deleteConfirmText !== 'DELETE'}
                            >
                                {isDeleting ? 'Deleting...' : 'Yes, delete my account'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default SettingsPage;

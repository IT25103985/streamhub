import React, { useState, useEffect, useContext } from 'react';
import { getSettings, updateSettings } from '../api/settingsApi';
import { AuthContext } from '../context/AuthContext';
import '../styles/SettingsPage.css';

const SettingsPage = () => {
    const { user } = useContext(AuthContext);
    
    // Backend Settings
    const [settings, setSettings] = useState({
        watchHistoryEnabled: true,
        notifySocial: true,
        notifyModeration: true,
        notifySystem: true
    });
    
    // Local Settings
    const [autoplay, setAutoplay] = useState(true);
    
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [saveStatus, setSaveStatus] = useState(null);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        // Load Local Settings
        const localAutoplay = localStorage.getItem('autoplay_enabled');
        if (localAutoplay !== null) {
            setAutoplay(localAutoplay === 'true');
        }

        // Load Backend Settings
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
                console.error("Failed to load settings:", err);
                setError("Failed to load your settings. Please try again later.");
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
            showSaveStatus("Local settings updated");
        }
    };

    const handleBackendToggle = async (key) => {
        if (isSaving) return;
        
        const newValue = !settings[key];
        const newSettings = { ...settings, [key]: newValue };
        
        // Optimistic update
        setSettings(newSettings);
        setIsSaving(true);
        
        try {
            await updateSettings(newSettings);
            showSaveStatus("Settings saved successfully");
        } catch (err) {
            console.error("Failed to update settings:", err);
            // Revert on failure
            setSettings({ ...settings, [key]: !newValue });
            setError("Failed to save changes. Please try again.");
            setTimeout(() => setError(null), 3000);
        } finally {
            setIsSaving(false);
        }
    };

    if (loading) {
        return <div className="settings-loading">Loading settings...</div>;
    }

    if (!user) {
        return <div className="settings-error">Please log in to view your settings.</div>;
    }

    return (
        <div className="settings-page">
            <div className="settings-header">
                <h1>Account Settings</h1>
                <p>Manage your preferences, privacy, and notifications.</p>
            </div>

            {error && <div className="settings-error">{error}</div>}

            <div className="settings-section">
                <h2>
                    <svg style={{width:'1.5rem', height:'1.5rem', marginRight:'0.5rem'}} fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    Playback
                </h2>
                
                <div className="setting-item">
                    <div className="setting-info">
                        <h3>Autoplay Next Video</h3>
                        <p>Automatically play the next related video when the current one finishes. This setting is stored locally on this device.</p>
                    </div>
                    <label className="toggle-switch">
                        <input 
                            type="checkbox" 
                            checked={autoplay}
                            onChange={(e) => handleLocalToggle('autoplay', e.target.checked)}
                        />
                        <span className="toggle-slider"></span>
                    </label>
                </div>
            </div>

            <div className="settings-section">
                <h2>
                    <svg style={{width:'1.5rem', height:'1.5rem', marginRight:'0.5rem'}} fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    Privacy & History
                </h2>
                
                <div className="setting-item">
                    <div className="setting-info">
                        <h3>Watch History</h3>
                        <p>Keep track of the videos you watch. This improves your recommendations. Disabling this will stop recording new views.</p>
                    </div>
                    <label className="toggle-switch">
                        <input 
                            type="checkbox" 
                            checked={settings.watchHistoryEnabled}
                            onChange={() => handleBackendToggle('watchHistoryEnabled')}
                            disabled={isSaving}
                        />
                        <span className="toggle-slider"></span>
                    </label>
                </div>
            </div>

            <div className="settings-section">
                <h2>
                    <svg style={{width:'1.5rem', height:'1.5rem', marginRight:'0.5rem'}} fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
                    Notifications
                </h2>
                
                <div className="setting-item">
                    <div className="setting-info">
                        <h3>Social Interactions</h3>
                        <p>Get notified when someone follows you, or likes/replies to your comments and videos.</p>
                    </div>
                    <label className="toggle-switch">
                        <input 
                            type="checkbox" 
                            checked={settings.notifySocial}
                            onChange={() => handleBackendToggle('notifySocial')}
                            disabled={isSaving}
                        />
                        <span className="toggle-slider"></span>
                    </label>
                </div>

                <div className="setting-item">
                    <div className="setting-info">
                        <h3>
                            <svg style={{display: 'inline', width:'1.2rem', height:'1.2rem', marginRight:'5px', color: 'var(--text-muted)'}} fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>
                            Moderation Alerts
                        </h3>
                        <p>Receive important alerts regarding your account standing, warnings, and content removals.</p>
                    </div>
                    <label className="toggle-switch">
                        <input 
                            type="checkbox" 
                            checked={settings.notifyModeration}
                            onChange={() => handleBackendToggle('notifyModeration')}
                            disabled={isSaving}
                        />
                        <span className="toggle-slider"></span>
                    </label>
                </div>

                <div className="setting-item">
                    <div className="setting-info">
                        <h3>
                            <svg style={{display: 'inline', width:'1.2rem', height:'1.2rem', marginRight:'5px', color: 'var(--text-muted)'}} fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
                            System Updates
                        </h3>
                        <p>Stay informed about new features, maintenance, and platform announcements.</p>
                    </div>
                    <label className="toggle-switch">
                        <input 
                            type="checkbox" 
                            checked={settings.notifySystem}
                            onChange={() => handleBackendToggle('notifySystem')}
                            disabled={isSaving}
                        />
                        <span className="toggle-slider"></span>
                    </label>
                </div>
            </div>

            {saveStatus && (
                <div className="save-status">
                    {saveStatus}
                </div>
            )}
        </div>
    );
};

export default SettingsPage;

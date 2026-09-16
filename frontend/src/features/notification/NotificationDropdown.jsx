import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotification } from '../../context/NotificationContext';
import { notificationApi } from '../../api/notificationApi';
import NotificationItemSkeleton from '../../components/skeletons/NotificationItemSkeleton';
import './Notification.css';

//dropdown list showing user notifications with instant mark read
const NotificationDropdown = ({ isOpen, onClose }) => {
    const { fetchUnreadCount } = useNotification();
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(false);
    const dropdownRef = useRef(null);
    const navigate = useNavigate();

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                onClose();
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [onClose]);

    useEffect(() => {
        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };
        document.addEventListener('keydown', handleEscape);
        return () => document.removeEventListener('keydown', handleEscape);
    }, [onClose]);

    useEffect(() => {
        if (isOpen) {
            loadNotifications();
        }
    }, [isOpen]);

    const loadNotifications = async () => {
        setLoading(true);
        try {
            const data = await notificationApi.getNotifications(0, 20);
            setNotifications(data.content || []);
        } catch (error) {
            console.error('Failed to load notifications', error);
        } finally {
            setLoading(false);
        }
    };

    const handleMarkAllAsRead = async () => {
        try {
            await notificationApi.markAllAsRead();
            setNotifications(notifications.map(n => ({ ...n, isRead: true })));
            fetchUnreadCount();
        } catch (error) {
            console.error('Failed to mark all as read', error);
        }
    };

    const handleNotificationClick = async (notification) => {
        if (!notification.isRead) {
            try {
                await notificationApi.markAsRead(notification.id);
                setNotifications(notifications.map(n => 
                    n.id === notification.id ? { ...n, isRead: true } : n
                ));
                fetchUnreadCount();
            } catch (error) {
                console.error('Failed to mark as read', error);
            }
        }
        
        onClose();
        
        //map notification type to target page route
        if (notification.relatedEntityType === 'VIDEO' && notification.relatedEntityId) {
            navigate(`/?watch=${notification.relatedEntityId}`);
        } else if (notification.relatedEntityType === 'PLAYLIST' && notification.relatedEntityId) {
            navigate(`/playlist/${notification.relatedEntityId}`);
        } else if (notification.relatedEntityType === 'REPORT') {
            navigate(`/my-reports`);
        } else if (notification.relatedEntityType === 'PROFILE' && notification.relatedEntityId) {
            navigate(`/profile/${notification.relatedEntityId}`);
        } else {
            navigate('/');
        }
    };

    const formatTimeAgo = (timestamp) => {
        const seconds = Math.floor((new Date() - new Date(timestamp)) / 1000);
        let interval = seconds / 31536000;
        if (interval > 1) return Math.floor(interval) + 'y ago';
        interval = seconds / 2592000;
        if (interval > 1) return Math.floor(interval) + 'mo ago';
        interval = seconds / 86400;
        if (interval > 1) return Math.floor(interval) + 'd ago';
        interval = seconds / 3600;
        if (interval > 1) return Math.floor(interval) + 'h ago';
        interval = seconds / 60;
        if (interval > 1) return Math.floor(interval) + 'm ago';
        return Math.floor(seconds) + 's ago';
    };

    if (!isOpen) return null;

    return (
        <div className="notification-dropdown-container" ref={dropdownRef}>
            <div className="notification-menu">
                <div className="notification-header">
                    <h3 className="notification-title">Notifications</h3>
                    <button className="notification-read-all" onClick={handleMarkAllAsRead}>
                        Mark all as read
                    </button>
                </div>
                
                <div className="notification-list">
                    {loading ? (
                        <>
                            <NotificationItemSkeleton />
                            <NotificationItemSkeleton />
                            <NotificationItemSkeleton />
                        </>
                    ) : notifications.length === 0 ? (
                        <div className="notification-empty">No notifications yet</div>
                    ) : (
                        notifications.map(n => (
                            <div 
                                key={n.id} 
                                className={`notification-item ${!n.isRead ? 'unread' : ''}`}
                                onClick={() => handleNotificationClick(n)}
                            >
                                <div className="notification-item-content">
                                    <h4 className="notification-item-title">{n.title}</h4>
                                    <p className="notification-item-message">{n.message}</p>
                                    <span className="notification-item-time">{formatTimeAgo(n.createdAt)}</span>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};

export default NotificationDropdown;

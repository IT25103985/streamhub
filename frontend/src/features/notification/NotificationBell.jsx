import React, { useState } from 'react';
import { useNotification } from '../../context/NotificationContext';
import NotificationDropdown from './NotificationDropdown';
import './Notification.css';

//header bell icon with live unread badge and dropdown toggle
const NotificationBell = () => {
    const { unreadCount } = useNotification();
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);

    const toggleDropdown = (e) => {
        //prevent outside click handler from firing
        e.stopPropagation();
        setIsDropdownOpen(!isDropdownOpen);
    };

    return (
        <div className="notification-bell-wrapper">
            <button 
                className="notification-bell-btn" 
                onClick={toggleDropdown}
                aria-label="Notifications"
            >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="bell-icon">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                </svg>
                {unreadCount > 0 && (
                    <span className="notification-badge">
                        {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                )}
            </button>
            
            <NotificationDropdown 
                isOpen={isDropdownOpen} 
                onClose={() => setIsDropdownOpen(false)} 
            />
        </div>
    );
};

export default NotificationBell;

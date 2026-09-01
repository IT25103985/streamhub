import React, { createContext, useContext, useState, useEffect } from 'react';
import { AuthContext } from './AuthContext';
import { notificationApi } from '../api/notificationApi';

//context provider managing notification polling and unread count
const NotificationContext = createContext();

export const useNotification = () => useContext(NotificationContext);

export const NotificationProvider = ({ children }) => {
    const { user } = useContext(AuthContext);
    const [unreadCount, setUnreadCount] = useState(0);

    const fetchUnreadCount = async () => {
        try {
            const data = await notificationApi.getUnreadCount();
            setUnreadCount(data.count);
        } catch (error) {
            console.error('Failed to fetch unread notifications count:', error);
        }
    };

    useEffect(() => {
        let intervalId;

        if (user) {
            fetchUnreadCount(); //initial load
            
            intervalId = setInterval(() => {
                fetchUnreadCount();
            }, 30000); //periodic 30s interval
        } else {
            setUnreadCount(0); //reset unread count on logout
        }

        return () => {
            if (intervalId) {
                clearInterval(intervalId);
            }
        };
    }, [user]);

    const decrementUnreadCount = () => {
        setUnreadCount(prev => Math.max(0, prev - 1));
    };

    const clearUnreadCount = () => {
        setUnreadCount(0);
    };

    return (
        <NotificationContext.Provider value={{
            unreadCount,
            fetchUnreadCount,
            decrementUnreadCount,
            clearUnreadCount
        }}>
            {children}
        </NotificationContext.Provider>
    );
};

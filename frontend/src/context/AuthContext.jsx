import React, { createContext, useState, useEffect, useContext } from 'react';
import axiosInstance, { setToken } from '../api/axiosInstance';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Auth Modal State
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState('login'); // 'login' or 'register'

  const openAuthModal = (mode = 'login') => {
    setAuthModalMode(mode);
    setIsAuthModalOpen(true);
  };

  const closeAuthModal = () => {
    setIsAuthModalOpen(false);
  };

  useEffect(() => {
    // Attempt silent refresh on mount
    const initAuth = async () => {
      try {
        const res = await axiosInstance.post('/auth/refresh');
        setToken(res.data.token);
        setUser({
          userId: res.data.userId,
          username: res.data.username,
          role: res.data.role
        });
      } catch (err) {
        // Not logged in or token expired
      } finally {
        setLoading(false);
      }
    };
    initAuth();

    // Listen for refresh failures from interceptor
    const handleRefreshFailed = () => setUser(null);
    window.addEventListener('auth-refresh-failed', handleRefreshFailed);
    return () => window.removeEventListener('auth-refresh-failed', handleRefreshFailed);
  }, []);

  const login = async (email, password, keepMeLoggedIn) => {
    try {
      const response = await axiosInstance.post('/auth/login', {
        email,
        password,
        keepMeLoggedIn
      });
      const token = response.data.token;
      setToken(token);
      
      setUser({
        userId: response.data.userId,
        username: response.data.username,
        role: response.data.role
      });
      return true;
    } catch (err) {
      console.error("Login failed", err);
      throw err;
    }
  };

  const register = async (userData) => {
    try {
      await axiosInstance.post('/auth/register', userData);
      return true;
    } catch (err) {
      console.error("Registration failed", err);
      throw err;
    }
  };

  const logout = async () => {
    try {
      await axiosInstance.post('/auth/logout');
    } catch (err) {
      console.error("Logout error", err);
    } finally {
      setUser(null);
      setToken(null);
    }
  };

  if (loading) {
    return <div className="flex h-screen items-center justify-center">Loading...</div>; // Could replace with a nice spinner
  }

  return (
    <AuthContext.Provider value={{ 
      user, login, logout, register, 
      isAuthModalOpen, authModalMode, openAuthModal, closeAuthModal, setAuthModalMode 
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useRequireAuth = () => {
  const { user, openAuthModal } = useContext(AuthContext);

  const requireAuth = (actionCallback) => {
    if (user) {
      actionCallback();
    } else {
      openAuthModal('register');
    }
  };

  return requireAuth;
};

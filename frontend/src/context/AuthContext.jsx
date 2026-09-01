import React, { createContext, useState, useEffect, useContext } from 'react';
import axiosInstance, { setToken } from '../api/axiosInstance';
import { checkNeedsOnboarding } from '../api/onboardingApi';

export //authentication provider managing user session token and login state
const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState('login');
  const [showInterestsModal, setShowInterestsModal] = useState(false);

  const openAuthModal = (mode = 'login') => {
    setAuthModalMode(mode);
    setIsAuthModalOpen(true);
  };

  const closeAuthModal = () => {
    setIsAuthModalOpen(false);
  };

  const isStaffRole = (role) => {
    if (!role) return false;
    const r = role.toUpperCase();
    return r === 'ADMINISTRATOR' || r === 'CONTENT_MODERATOR' || r === 'ADMIN' || r === 'MODERATOR';
  };

  useEffect(() => {
    //attempt silent token refresh on app launch
    const initAuth = async () => {
      try {
        const res = await axiosInstance.post('/auth/refresh');
        setToken(res.data.token);
        const isStaff = isStaffRole(res.data.role);
        const needsOnboarding = Boolean(res.data.needsOnboarding) && !isStaff;
        const userData = {
          userId: res.data.userId,
          username: res.data.username,
          email: res.data.email,
          role: res.data.role,
          avatarUrl: res.data.avatarUrl,
          needsOnboarding
        };
        setUser(userData);
      } catch (err) {
        //reset state when token is expired or missing
      } finally {
        setLoading(false);
      }
    };
    initAuth();

    //listen for expired session events
    const handleRefreshFailed = () => {
      setUser(null);
      setShowInterestsModal(false);
    };
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
      
      const isStaff = isStaffRole(response.data.role);
      const needsOnboarding = Boolean(response.data.needsOnboarding) && !isStaff;
      const userData = {
        userId: response.data.userId,
        username: response.data.username,
        email: response.data.email,
        role: response.data.role,
        avatarUrl: response.data.avatarUrl,
        needsOnboarding
      };
      setUser(userData);

      // Only popup once for fresh users upon registration and initial login
      const isFreshRegistration = sessionStorage.getItem('streamhub_fresh_registration') === 'true';
      const alreadyPrompted = sessionStorage.getItem(`onboarding_prompted_${userData.userId}`);
      if (needsOnboarding && (isFreshRegistration || !alreadyPrompted)) {
        setShowInterestsModal(true);
        sessionStorage.setItem(`onboarding_prompted_${userData.userId}`, 'true');
        sessionStorage.removeItem('streamhub_fresh_registration');
      }

      return userData;
    } catch (err) {
      console.error("Login failed", err);
      throw err;
    }
  };

  const completeOnboarding = () => {
    setShowInterestsModal(false);
    setUser(prev => prev ? { ...prev, needsOnboarding: false } : null);
    if (user?.userId) {
      sessionStorage.setItem(`onboarding_prompted_${user.userId}`, 'true');
    }
    sessionStorage.removeItem('streamhub_fresh_registration');
    window.dispatchEvent(new CustomEvent('streamhub-video-update'));
  };

  const register = async (userData) => {
    try {
      await axiosInstance.post('/auth/register', userData);
      sessionStorage.setItem('streamhub_fresh_registration', 'true');
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
      setShowInterestsModal(false);
    }
  };

  const updateUserAvatar = (newAvatarUrl) => {
    setUser(prev => prev ? { ...prev, avatarUrl: newAvatarUrl } : null);
  };

  if (loading) {
    return <div className="flex h-screen items-center justify-center">Loading...</div>;
  }

  return (
    <AuthContext.Provider value={{ 
      user, login, logout, register, updateUserAvatar,
      isAuthModalOpen, authModalMode, openAuthModal, closeAuthModal, setAuthModalMode,
      showInterestsModal, setShowInterestsModal, completeOnboarding
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

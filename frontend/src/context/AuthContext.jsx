import React, { createContext, useState, useEffect } from 'react';
import axiosInstance from '../api/axiosInstance';

// Create Context
export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);

  // Load user from local storage or token if we had a real check
  // For the frontend foundation demo, we'll start with logged out.
  // The user can be toggled manually for testing or it will eventually connect to a real login endpoint.
  useEffect(() => {
    // Example: You could parse window.__streamhub_jwt here if you decode it,
    // or call an /api/v1/auth/me endpoint.
    // For now, we leave it null.
  }, []);

  const login = async (username, password) => {
    try {
      const response = await axiosInstance.post('/auth/login', {
        username: username,
        password: password
      });
      const token = response.data.token;
      window.__streamhub_jwt = token;
      
      // We assume the response also returns the user details, or we just parse/set hardcoded for now
      setUser({
        username: response.data.username || username,
        role: response.data.role || 'ADMINISTRATOR' 
      });
    } catch (err) {
      console.error("Login failed", err);
      alert("Login failed! Ensure backend is running and credentials are correct.");
    }
  };

  const logout = () => {
    setUser(null);
    window.__streamhub_jwt = null;
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

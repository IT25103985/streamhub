import React, { createContext, useState, useEffect } from 'react';

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

  const login = (userData) => {
    setUser(userData);
  };

  const logout = () => {
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

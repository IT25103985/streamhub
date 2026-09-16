import React, { useState, useEffect, useRef, useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { getFullImageUrl } from '../utils/imageUtils';
import axiosInstance from '../api/axiosInstance';
import './ProfileDropdown.css';

//user menu dropdown with links to profile and logout
const ProfileDropdown = () => {
  const { user, logout } = useContext(AuthContext);
  const [isOpen, setIsOpen] = useState(false);
  const [userEmail, setUserEmail] = useState(user?.email || '');
  const dropdownRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (user?.email) {
      setUserEmail(user.email);
    } else if (user) {
      axiosInstance.get('/users/me/profile')
        .then(res => {
          if (res.data?.email) {
            setUserEmail(res.data.email);
          }
        })
        .catch(() => {});
    }
  }, [user]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  if (!user) return null;

  return (
    <div className="profile-dropdown-container" ref={dropdownRef}>
      <button 
        className="profile-btn"
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Profile Menu"
        style={user.avatarUrl ? { backgroundImage: `url(${getFullImageUrl(user.avatarUrl)})`, backgroundSize: 'cover', backgroundPosition: 'center', color: 'transparent' } : {}}
      >
        {!user.avatarUrl && user.username.charAt(0).toUpperCase()}
      </button>

      {isOpen && (
        <div className="profile-menu">
          <div className="profile-menu-header">
            <p className="profile-menu-name">{user.username}</p>
            <p className="profile-menu-email">{userEmail || user.email || ''}</p>
          </div>

          <div className="profile-menu-section">
            <Link to="/profile" className="dropdown-item" onClick={() => setIsOpen(false)}>View Profile</Link>
            <Link to="/my-playlists" className="dropdown-item" onClick={() => setIsOpen(false)}>My Playlists</Link>
            <Link to="/watch-history" className="dropdown-item" onClick={() => setIsOpen(false)}>Watch History</Link>
            <Link to="/my-reports" className="dropdown-item" onClick={() => setIsOpen(false)}>My Reports</Link>
            
            {(user.role === 'ADMINISTRATOR' || user.role === 'CONTENT_MODERATOR') && (
              <Link to="/admin" className="dropdown-item" onClick={() => setIsOpen(false)}>Admin Dashboard</Link>
            )}
          </div>
          
          <div className="profile-menu-divider">
            <Link to="/help" className="dropdown-item" onClick={() => setIsOpen(false)}>Help</Link>
            <Link to="/settings" className="dropdown-item" onClick={() => setIsOpen(false)}>Settings</Link>
          </div>
          
          <div className="profile-menu-divider">
            <button onClick={handleLogout} className="dropdown-item">
              Log out
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProfileDropdown;

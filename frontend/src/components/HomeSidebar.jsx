import React, { useState, useEffect, useContext } from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import axiosInstance from '../api/axiosInstance';
import { getFullImageUrl } from '../utils/imageUtils';
import './HomeSidebar.css';

//left side navigation rail for home feed and shortcuts
const HomeSidebar = ({ recommendedVideos = [], onSelectVideo }) => {
  const { user, logout, openAuthModal } = useContext(AuthContext);
  const [playlists, setPlaylists] = useState([]);
  const [expanded, setExpanded] = useState(false);
  const [followingData, setFollowingData] = useState(null);
  const [followingExpanded, setFollowingExpanded] = useState(false);
  const navigate = useNavigate();

  const fetchPlaylists = () => {
    axiosInstance.get('/playlists/public')
      .then(res => {
        if (Array.isArray(res.data)) {
          setPlaylists(res.data);
        } else {
          setPlaylists([]);
        }
      })
      .catch((err) => {
        console.error('Failed to fetch public playlists for sidebar:', err);
        setPlaylists([]);
      });
  };

  useEffect(() => {
    if (user) {
      axiosInstance.get('/follows/me/sidebar')
        .then(res => setFollowingData(res.data))
        .catch(err => console.error("Failed to fetch sidebar following", err));
    } else {
      setFollowingData(null);
    }
  }, [user]);

  useEffect(() => {
    fetchPlaylists();

    const handleUpdate = () => fetchPlaylists();
    window.addEventListener('streamhub-playlist-update', handleUpdate);
    window.addEventListener('focus', handleUpdate);

    return () => {
      window.removeEventListener('streamhub-playlist-update', handleUpdate);
      window.removeEventListener('focus', handleUpdate);
    };
  }, [user]);

  const displayList = Array.isArray(playlists) ? playlists.map(p => ({
    id: p?.playlistId || p?.id,
    name: p?.title || p?.name || 'Untitled',
    avatar: p?.thumbnailUrl ? getFullImageUrl(p.thumbnailUrl) : null,
    isLive: true,
    status: 'online',
    link: `/playlist/${p?.playlistId || p?.id}`
  })) : [];

  const visibleItems = Array.isArray(displayList) ? (expanded ? displayList : displayList.slice(0, 4)) : [];
  const visibleFollowing = followingData ? (followingExpanded ? followingData.topFollowed : followingData.topFollowed.slice(0, 3)) : [];

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  //sidebar navigation rail section
  return (
    <aside className="home-sidebar">
      <div className="home-sidebar-inner">
        <nav className="home-sidebar-nav">
          <NavLink 
            to="/" 
            end 
            className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
          >
            <div className="sidebar-nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/>
              </svg>
            </div>
            <span className="sidebar-nav-label">Browse</span>
          </NavLink>

          <NavLink 
            to="/watch-history" 
            className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
          >
            <div className="sidebar-nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <span className="sidebar-nav-label">Watch history</span>
          </NavLink>
        </nav>

        {user && followingData && followingData.totalFollowing > 0 && (
          <>
            <div className="sidebar-divider" />
            <div className="sidebar-section">
              <div className="sidebar-section-title">
                <span>Following</span>
              </div>
              <div className={`sidebar-playlist-list ${followingExpanded ? 'scrollable-list' : ''}`}>
                {visibleFollowing.map(f => (
                  <Link 
                    key={f.userId} 
                    to={`/profile/${f.userId}`} 
                    className="sidebar-playlist-item"
                  >
                    <div className="sidebar-playlist-avatar">
                      {f.avatarUrl ? (
                        <img src={getFullImageUrl(f.avatarUrl)} alt={f.displayName} />
                      ) : (
                        <span>{f.displayName ? f.displayName[0].toUpperCase() : 'U'}</span>
                      )}
                    </div>
                    <span className="sidebar-playlist-name">{f.displayName}</span>
                  </Link>
                ))}
              </div>
              {followingData.topFollowed.length > 3 && (
                <button 
                  className="sidebar-load-more-btn"
                  onClick={() => setFollowingExpanded(!followingExpanded)}
                >
                  <div className="load-more-icon-circle" style={{ background: 'var(--surface-3)', color: 'var(--text-dim)' }}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ transform: followingExpanded ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s ease' }}>
                      <polyline points="6 9 12 15 18 9"/>
                    </svg>
                  </div>
                  <span>{followingExpanded ? 'Show less' : 'See more'}</span>
                </button>
              )}
            </div>
          </>
        )}

        <div className="sidebar-divider" />

        <div className="sidebar-section">
          <div className="sidebar-section-title">
            <span>Playlist</span>
          </div>

          <div className={`sidebar-playlist-list ${expanded ? 'scrollable-list' : ''}`}>
            {displayList.length > 0 ? (
              visibleItems.map((item, idx) => (
                <Link 
                  key={item.id || idx} 
                  to={item.link || '#'} 
                  className="sidebar-playlist-item"
                  onClick={(e) => { if (!item.link) e.preventDefault(); }}
                >
                  <div className="sidebar-playlist-avatar">
                    {item.avatar ? (
                      <img src={item.avatar} alt={item.name} />
                    ) : (
                      <span>{item.name ? item.name[0].toUpperCase() : 'P'}</span>
                    )}
                  </div>
                  <span className="sidebar-playlist-name">{item.name}</span>
                </Link>
              ))
            ) : (
              <div style={{ padding: '6px 12px', fontSize: '12.5px', color: 'var(--text-dim)' }}>
                <p style={{ margin: '0 0 6px 0' }}>No public playlists yet</p>
                {user ? (
                  <Link to="/my-playlists" style={{ color: 'var(--accent, #6366f1)', textDecoration: 'none', fontSize: '12px', fontWeight: 500 }}>
                    + Create playlist
                  </Link>
                ) : (
                  <button 
                    className="btn btn-ghost" 
                    onClick={() => openAuthModal('login')}
                    style={{ 
                      width: '100%', 
                      fontSize: '12px', 
                      padding: '6px 10px', 
                      border: '1px solid var(--border)',
                      borderRadius: '6px',
                      color: 'var(--text)',
                      background: 'var(--surface-2)',
                      cursor: 'pointer'
                    }}
                  >
                    Sign In
                  </button>
                )}
              </div>
            )}
          </div>

          {displayList.length > 4 && (
            <button 
              className="sidebar-load-more-btn"
              onClick={() => setExpanded(!expanded)}
            >
              <div className="load-more-icon-circle">
                <svg 
                  width="13" 
                  height="13" 
                  viewBox="0 0 24 24" 
                  fill="none" 
                  stroke="currentColor" 
                  strokeWidth="2.5" 
                  strokeLinecap="round" 
                  strokeLinejoin="round"
                  style={{ transform: expanded ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s ease' }}
                >
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </div>
              <span>{expanded ? 'Show less' : 'Load more'}</span>
            </button>
          )}
        </div>

        <div className="sidebar-bottom">
          {user ? (
            <button className="sidebar-logout-btn" onClick={handleLogout}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
              <span>Log Out</span>
            </button>
          ) : (
            <button className="sidebar-logout-btn" onClick={() => openAuthModal('login')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                <polyline points="10 17 15 12 10 7"/>
                <line x1="15" y1="12" x2="3" y2="12"/>
              </svg>
              <span>Log In</span>
            </button>
          )}
        </div>
      </div>
    </aside>
  );
};

export default HomeSidebar;

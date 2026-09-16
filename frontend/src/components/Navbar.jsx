import { useContext, useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext, useRequireAuth } from '../context/AuthContext';
import axiosInstance from '../api/axiosInstance';
import ProfileDropdown from './ProfileDropdown';
import NotificationBell from '../features/notification/NotificationBell';
//primary top header with search bar notifications and user menu
const Navbar = () => {
  const { user, logout, openAuthModal } = useContext(AuthContext);
  const [navOpen, setNavOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [categories, setCategories] = useState([]);
  const navigate = useNavigate();
  const location = useLocation();
  const requireAuth = useRequireAuth();

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 8);
    };
    window.addEventListener('scroll', handleScroll);
    //navbar header and search section
  return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    //load navbar categories
    axiosInstance.get('/categories')
      .then(res => {
                setCategories(res.data.slice(0, 4)); //limit to top four categories
      })
      .catch(err => console.error("Error fetching categories for navbar", err));
  }, []);

  const [searchQuery, setSearchQuery] = useState('');

  const handleLogoClick = (e) => {
    e.preventDefault();
    setSearchQuery('');
    window.dispatchEvent(new CustomEvent('streamhub-reset-home'));
    window.scrollTo({ top: 0, behavior: 'smooth' });
    navigate('/');
  };

  //handles global search navigation on enter key
  const handleSearch = (e) => {
    if (e.key === 'Enter') {
      const q = e.target.value.trim();
      if (q) {
        navigate(`/search?q=${encodeURIComponent(q)}`);
      } else {
        navigate(`/`);
      }
    }
  };

  return (
    <header style={{ background: scrolled ? 'rgba(10,13,22,.92)' : 'rgba(10,13,22,.72)' }}>
      <Link to="/" onClick={handleLogoClick} className="logo">Stream<span>Hub</span></Link>
      <div className="header-search">
        <label className="search-pill">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
          <input 
            type="text" 
            placeholder="Search…" 
            aria-label="Search videos" 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={handleSearch} 
          />
        </label>
      </div>
      <div className="header-actions">
        
        <button onClick={() => requireAuth(() => navigate('/upload'))} className="btn btn-ghost upload-btn" aria-label="Upload Video">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          <span className="upload-text">Upload</span>
        </button>
        
        {user ? (
          <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
            <NotificationBell />
            <ProfileDropdown />
          </div>
        ) : (
          <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
            <button onClick={() => openAuthModal('login')} className="btn btn-primary" style={{ padding: '6px 14px', fontSize: '13px' }}>Log In</button>
          </div>
        )}
      </div>
    </header>
  );
};

export default Navbar;

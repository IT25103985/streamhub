import { useContext, useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import axiosInstance from '../api/axiosInstance';

const Navbar = () => {
  const { user, logout, login } = useContext(AuthContext); // login is just for demo if needed
  const [navOpen, setNavOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [categories, setCategories] = useState([]);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 8);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    // Fetch categories for the navbar
    axiosInstance.get('/categories')
      .then(res => {
        // Assume backend returns array of objects with id, name, slug or similar
        // Adjust based on actual DTO
        setCategories(res.data.slice(0, 4)); // max 4 for header
      })
      .catch(err => console.error("Error fetching categories for navbar", err));
  }, []);

  const handleSearch = (e) => {
    if (e.key === 'Enter') {
      const q = e.target.value.trim();
      if (q) {
        navigate(`/?q=${encodeURIComponent(q)}`);
      } else {
        navigate(`/`);
      }
    }
  };

  return (
    <header style={{ background: scrolled ? 'rgba(10,13,22,.92)' : 'rgba(10,13,22,.72)' }}>
      <Link to="/" className="logo">Stream<span>Hub</span></Link>
      <nav className={navOpen ? 'open' : ''}>
        <Link to="/" className={location.pathname === '/' ? 'active' : ''} onClick={() => setNavOpen(false)}>Home</Link>
        {categories.length > 0 ? (
           categories.map(cat => (
             <Link 
               key={cat.id} 
               to={`/categories/${cat.id}`} 
               className={location.pathname === `/categories/${cat.id}` ? 'active' : ''} 
               onClick={() => setNavOpen(false)}
             >
               {cat.name}
             </Link>
           ))
        ) : (
          <>
            {/* Fallback until API is ready */}
            <Link to="/categories/1" className={location.pathname === '/categories/1' ? 'active' : ''} onClick={() => setNavOpen(false)}>Gaming</Link>
            <Link to="/categories/2" className={location.pathname === '/categories/2' ? 'active' : ''} onClick={() => setNavOpen(false)}>Music</Link>
            <Link to="/categories/3" className={location.pathname === '/categories/3' ? 'active' : ''} onClick={() => setNavOpen(false)}>Movies</Link>
          </>
        )}
      </nav>
      <div className="header-actions">
        <label className="search-pill">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
          <input type="text" placeholder="Search the library…" aria-label="Search videos" onKeyDown={handleSearch} />
        </label>
        
        {user ? (
          <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
            {user.role === 'ADMIN' && (
              <Link to="/admin" className="btn btn-ghost" style={{ padding: '6px 12px', fontSize: '13px' }}>Admin</Link>
            )}
            <span style={{ fontSize: '14px', color: 'var(--text-dim)' }}>{user.username}</span>
            <button className="btn btn-ghost" onClick={logout}>Logout</button>
          </div>
        ) : (
          <button className="btn btn-primary" onClick={() => login({ username: 'DemoUser', role: 'ADMIN' })}>Login</button>
        )}
        
        <button className="nav-toggle" aria-label="Toggle menu" onClick={() => setNavOpen(!navOpen)}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M3 6h18M3 12h18M3 18h18"/></svg>
        </button>
      </div>
    </header>
  );
};

export default Navbar;

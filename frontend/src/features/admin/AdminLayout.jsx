import React, { useContext } from 'react';
import { Navigate, Outlet, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import AdminSidebar from './AdminSidebar';

const AdminNavbar = ({ user, logout }) => {
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };


  return (
    <header style={{ background: 'rgba(10,13,22,.92)', height: '50px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 32px', margin: 0, borderBottom: '1px solid var(--border)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <span className="logo" style={{ cursor: 'default', userSelect: 'none' }}>Stream<span>Hub</span></span>
        <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700, padding: '2px 8px', background: 'rgba(229, 9, 20, 0.15)', color: '#e50914', borderRadius: '4px', border: '1px solid rgba(229, 9, 20, 0.3)' }}>Admin</span>
      </div>
      <div className="header-actions" style={{ flex: 1, justifyContent: 'flex-end', display: 'flex', gap: '20px' }}>
        <label className="search-pill">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
          <input type="text" placeholder="Search admin panel…" aria-label="Search admin panel" />
        </label>
        
        <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
          <span style={{ fontSize: '14px', color: 'var(--text-dim)', fontWeight: 500 }}>
            {user?.username 
              ? (user.username.toLowerCase() === 'admin' 
                  ? 'Admin' 
                  : user.username.toLowerCase() === 'moderator' 
                    ? 'Moderator' 
                    : user.username.charAt(0).toUpperCase() + user.username.slice(1)) 
              : 'Admin'}
          </span>
          <button className="btn btn-ghost" onClick={handleLogout}>Logout</button>
        </div>
      </div>
    </header>
  );
};

//wrapper layout providing admin sidebar and authentication check
const AdminLayout = () => {
  const { user, loading, logout } = useContext(AuthContext);

  if (loading) {
    return <div className="admin-layout" style={{ justifyContent: 'center', alignItems: 'center' }}>Loading...</div>;
  }

  if (!user || (user.role !== 'CONTENT_MODERATOR' && user.role !== 'ADMINISTRATOR')) {
    return <Navigate to="/" replace />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', margin: '0 -0.5cm' }}>
      <AdminNavbar user={user} logout={logout} />
      <div className="admin-layout" style={{ flex: 1, overflow: 'hidden' }}>
        <AdminSidebar role={user.role} />
        <div className="admin-content" style={{ flex: 1, overflowY: 'auto' }}>
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default AdminLayout;

import React, { useState, useEffect } from 'react';
import adminUserService from './adminUserService';
import AdminDataTable from '../../../components/AdminDataTable';
import OptionsMenu from '../../../components/OptionsMenu';

const ICONS = {
  admin: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  ),
  mod: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
    </svg>
  ),
  user: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  ),
  activate: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
      <polyline points="22 4 12 14.01 9 11.01" />
    </svg>
  ),
  deactivate: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="12" r="10" />
      <line x1="10" y1="15" x2="10" y2="9" />
      <line x1="14" y1="15" x2="14" y2="9" />
    </svg>
  ),
  ban: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="12" r="10" />
      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
    </svg>
  )
};

//admin interface for promoting demoting banning or activating users
const AdminUsersPage = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const data = await adminUserService.getAllUsers();
      setUsers(data);
      setError(null);
    } catch (err) {
      console.error('Error fetching users:', err);
      setError('Failed to load users. ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  //updates user permission level in system
  const handleUpdateRole = async (userId, newRole) => {
    if (!window.confirm(`Are you sure you want to change role to ${newRole}?`)) {
      return;
    }
    try {
      await adminUserService.updateUserRole(userId, newRole);
      fetchUsers();
    } catch (err) {
      console.error('Error updating role:', err);
      alert('Failed to update role.');
    }
  };

  //toggles user account active or banned status
  const handleUpdateStatus = async (userId, newStatus) => {
    if (!window.confirm(`Are you sure you want to change status to ${newStatus}?`)) {
      return;
    }
    try {
      await adminUserService.updateUserStatus(userId, newStatus);
      fetchUsers();
    } catch (err) {
      console.error('Error updating status:', err);
      alert('Failed to update status.');
    }
  };

  const columns = [
    { key: 'username', label: 'Username' },
    { key: 'email', label: 'Email' },
    { 
      key: 'role', 
      label: 'Role',
      render: (item) => (
        <span style={{ 
          padding: '4px 8px', 
          borderRadius: '4px', 
          fontSize: '12px',
          fontWeight: 'bold',
          backgroundColor: item.role === 'ADMINISTRATOR' ? 'rgba(239, 68, 68, 0.2)' : 
                         item.role === 'CONTENT_MODERATOR' ? 'rgba(59, 130, 246, 0.2)' : 'rgba(107, 114, 128, 0.2)',
          color: item.role === 'ADMINISTRATOR' ? '#ef4444' : 
                 item.role === 'CONTENT_MODERATOR' ? '#3b82f6' : '#9ca3af'
        }}>
          {item.role === 'ADMINISTRATOR' ? 'Admin' : item.role === 'CONTENT_MODERATOR' ? 'Moderator' : 'User'}
        </span>
      )
    },
    { 
      key: 'status', 
      label: 'Status',
      render: (item) => (
        <span style={{ 
          padding: '4px 8px', 
          borderRadius: '4px', 
          fontSize: '12px',
          fontWeight: 'bold',
          backgroundColor: item.status === 'ACTIVE' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(245, 158, 11, 0.2)',
          color: item.status === 'ACTIVE' ? '#10b981' : '#f59e0b'
        }}>
          {item.status}
        </span>
      )
    },
    { 
      key: 'joinedDate', 
      label: 'Joined Date',
      render: (item) => new Date(item.joinedDate).toLocaleDateString()
    }
  ];

  const getUserActionItems = (item) => {
    const items = [];

    //role action buttons
    if (item.role !== 'ADMINISTRATOR') {
      items.push({
        label: 'Promote to Admin',
        icon: ICONS.admin,
        onClick: () => handleUpdateRole(item.userId, 'ADMINISTRATOR')
      });
    }
    if (item.role !== 'CONTENT_MODERATOR') {
      items.push({
        label: 'Make Moderator',
        icon: ICONS.mod,
        onClick: () => handleUpdateRole(item.userId, 'CONTENT_MODERATOR')
      });
    }
    if (item.role !== 'USER') {
      items.push({
        label: 'Demote to User',
        icon: ICONS.user,
        onClick: () => handleUpdateRole(item.userId, 'USER')
      });
    }

    //status action buttons
    if (item.status !== 'ACTIVE') {
      items.push({
        label: 'Activate Account',
        icon: ICONS.activate,
        style: { color: '#10b981' },
        onClick: () => handleUpdateStatus(item.userId, 'ACTIVE')
      });
    }
    if (item.status !== 'DEACTIVATED') {
      items.push({
        label: 'Deactivate Account',
        icon: ICONS.deactivate,
        style: { color: '#f59e0b' },
        onClick: () => handleUpdateStatus(item.userId, 'DEACTIVATED')
      });
    }
    if (item.status !== 'BANNED') {
      items.push({
        label: 'Ban Account',
        icon: ICONS.ban,
        className: 'danger',
        onClick: () => handleUpdateStatus(item.userId, 'BANNED')
      });
    }

    return items;
  };

  const actions = (item) => (
    <div className="admin-table-actions">
      <OptionsMenu items={getUserActionItems(item)} />
    </div>
  );

  //admin user management table section
  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <h2>Manage Users</h2>
      </div>
      <div className="admin-page-content">
        {error && <div className="error-message" style={{ marginBottom: '16px' }}>{error}</div>}
        <AdminDataTable 
          data={users} 
          columns={columns} 
          actions={actions}
          loading={loading}
        />
      </div>
    </div>
  );
};

export default AdminUsersPage;

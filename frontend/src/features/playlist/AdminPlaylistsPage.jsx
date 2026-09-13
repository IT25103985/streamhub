import React, { useState, useEffect } from 'react';
import { playlistService } from './playlistService';
import AdminDataTable from '../../components/AdminDataTable';

//admin management view for viewing and toggling visibility of all playlists
const AdminPlaylistsPage = () => {
  const [playlists, setPlaylists] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchPlaylists();
  }, []);

  //fetches full playlist list from admin endpoint
  const fetchPlaylists = async () => {
    try {
      setLoading(true);
      const data = await playlistService.getAllPlaylistsAdmin();
      setPlaylists(data);
      setError(null);
    } catch (err) {
      console.error('Error fetching playlists:', err);
      setError('Failed to load playlists.');
    } finally {
      setLoading(false);
    }
  };

  //toggles hidden status to moderate inappropriate playlists
  const handleToggleHide = async (id) => {
    try {
      await playlistService.togglePlaylistHiddenAdmin(id);
      fetchPlaylists();
    } catch (err) {
      console.error('Error toggling playlist visibility:', err);
      alert('Failed to toggle visibility.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this playlist? This action cannot be undone.')) {
      return;
    }
    try {
      await playlistService.deletePlaylistAdmin(id);
      fetchPlaylists();
    } catch (err) {
      console.error('Error deleting playlist:', err);
      alert('Failed to delete playlist.');
    }
  };

  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'ownerUsername', label: 'Owner' },
    { key: 'videoCount', label: 'Videos' },
    { 
      key: 'isHidden', 
      label: 'Status',
      render: (item) => item.isHidden ? <span style={{color: 'var(--accent-color)'}}>Hidden</span> : 'Visible'
    }
  ];

  const actions = (item) => (
    <div className="admin-table-actions">
      <button 
        className="btn-action"
        style={{ marginRight: '8px', backgroundColor: item.isHidden ? 'var(--primary-color)' : 'var(--text-faint)', color: 'var(--bg-color)', border: 'none', padding: '6px 12px', borderRadius: '4px', cursor: 'pointer' }}
        onClick={() => handleToggleHide(item.playlistId)}
      >
        {item.isHidden ? 'Unhide' : 'Hide'}
      </button>
      <button 
        className="btn-action"
        style={{ backgroundColor: '#ff4d4f', color: 'white', border: 'none', padding: '6px 12px', borderRadius: '4px', cursor: 'pointer' }}
        onClick={() => handleDelete(item.playlistId)}
      >
        Delete
      </button>
    </div>
  );

  //admin playlist table section
  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <h2>Manage Playlists</h2>
      </div>
      <div className="admin-page-content">
        {error && <div className="error-message" style={{ marginBottom: '16px' }}>{error}</div>}
        <AdminDataTable 
          data={playlists} 
          columns={columns} 
          actions={actions}
          loading={loading}
        />
      </div>
    </div>
  );
};

export default AdminPlaylistsPage;

import { useState, useEffect } from 'react';
import { socialService } from './socialService';
import AdminDataTable from '../../components/AdminDataTable';

//admin moderation table listing all platform comments with delete action
const AdminCommentsPage = () => {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchComments = async () => {
    try {
      setLoading(true);
      const data = await socialService.getAllCommentsAdmin();
      setComments(data);
      setError(null);
    } catch (err) {
      console.error('Error fetching comments:', err);
      setError('Failed to load comments.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComments();
  }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this comment? This action cannot be undone.')) {
      return;
    }
    try {
      await socialService.deleteCommentAdmin(id);
      fetchComments();
    } catch (err) {
      console.error('Error deleting comment:', err);
      alert('Failed to delete comment.');
    }
  };

  const columns = [
    { key: 'authorUsername', label: 'Author' },
    { key: 'videoTitle', label: 'Video' },
    { 
      key: 'text', 
      label: 'Comment',
      render: (item) => {
        const truncated = item.text.length > 50 ? item.text.substring(0, 50) + '...' : item.text;
        return <span title={item.text}>{truncated}</span>;
      }
    },
    { 
      key: 'createdAt', 
      label: 'Date',
      render: (item) => new Date(item.createdAt).toLocaleDateString()
    }
  ];

  const actions = (item) => (
    <div className="admin-table-actions">
      <button 
        className="btn-action"
        style={{ backgroundColor: '#ff4d4f', color: 'white', border: 'none', padding: '6px 12px', borderRadius: '4px', cursor: 'pointer' }}
        onClick={() => handleDelete(item.commentId)}
      >
        Delete
      </button>
    </div>
  );

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <h2>Manage Comments</h2>
      </div>
      <div className="admin-page-content">
        {error && <div className="error-message" style={{ marginBottom: '16px' }}>{error}</div>}
        <AdminDataTable 
          data={comments} 
          columns={columns} 
          actions={actions}
          loading={loading}
        />
      </div>
    </div>
  );
};

export default AdminCommentsPage;

import React, { useState, useEffect } from 'react';
import { getCategories, createCategory, updateCategory, deleteCategory } from './categoryService';
import AdminDataTable from '../../components/AdminDataTable';

//admin panel for creating and updating video categories
const CategoryManagement = () => {
  const [categories, setCategories] = useState([]);
  const [formData, setFormData] = useState({ name: '', description: '' });
  const [editingId, setEditingId] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async (search = '') => {
    try {
      setLoading(true);
      const data = await getCategories(search);
      setCategories(data);
      setError('');
    } catch (err) {
      setError('Failed to fetch categories');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchCategories(searchQuery);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        await updateCategory(editingId, formData);
      } else {
        await createCategory(formData);
      }
      setFormData({ name: '', description: '' });
      setEditingId(null);
      fetchCategories();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.response?.data?.name || (typeof err.response?.data === 'string' ? err.response?.data : null);
      setError(msg || 'Operation failed. Please ensure you are logged in with an Administrator account.');
    }
  };

  const handleEdit = (category) => {
    setFormData({ name: category.name, description: category.description || '' });
    setEditingId(category.id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleCancelEdit = () => {
    setFormData({ name: '', description: '' });
    setEditingId(null);
    setError('');
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this category?')) return;
    try {
      await deleteCategory(id);
      fetchCategories();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to delete category');
    }
  };

  const columns = [
    { key: 'name', label: 'Category Name', render: (row) => <strong>{row.name}</strong> },
    { key: 'description', label: 'Description', render: (row) => <span style={{ color: 'var(--text-dim)' }}>{row.description || '-'}</span> }
  ];

  const renderActions = (row) => (
    <>
      <button className="btn-action edit" onClick={() => handleEdit(row)}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
        Edit
      </button>
      <button className="btn-action delete" onClick={() => handleDelete(row.id)}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
        Delete
      </button>
    </>
  );

  return (
    <div>
      <div className="admin-page-header">
        <h1>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>
          Category Management
        </h1>
      </div>
      
      {error && (
        <div className="upload-error" style={{ marginBottom: '24px' }}>
          {error}
        </div>
      )}

      <div className="upload-container" style={{ maxWidth: '100%', padding: '32px', marginBottom: '40px' }}>
        <h2 style={{ fontSize: '18px', marginBottom: '24px', color: 'var(--text)' }}>{editingId ? 'Edit Category' : 'Create New Category'}</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Name *</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              required
              maxLength="100"
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              maxLength="500"
              rows="3"
            />
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button type="submit" className="btn btn-primary">
              {editingId ? 'Update Category' : 'Create Category'}
            </button>
            {editingId && (
              <button type="button" className="btn btn-ghost" onClick={handleCancelEdit}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h2 style={{ fontSize: '18px', margin: 0 }}>Existing Categories</h2>
        <form onSubmit={handleSearch} className="search-pill" style={{ margin: 0, width: '280px' }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <input
            type="text"
            placeholder="Search categories..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </form>
      </div>

      <AdminDataTable 
        columns={columns} 
        data={categories} 
        actions={renderActions} 
        loading={loading}
      />
    </div>
  );
};

export default CategoryManagement;

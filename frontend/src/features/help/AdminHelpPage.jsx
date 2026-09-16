import React, { useState, useEffect } from 'react';
import { helpApi } from '../../api/helpApi';
import './AdminHelpPage.css';

//admin help center management component for creating updating and deleting faq topics
const AdminHelpPage = () => {
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  //modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTopic, setEditingTopic] = useState(null);
  const [formData, setFormData] = useState({
    section: '',
    customSection: '',
    question: '',
    answer: '',
    displayOrder: 0
  });

  //delete confirmation modal state
  const [deletingTopic, setDeletingTopic] = useState(null);

  useEffect(() => {
    fetchTopics();
  }, []);

  const fetchTopics = async () => {
    setLoading(true);
    try {
      const data = await helpApi.getAllTopics();
      setTopics(data || []);
      setError('');
    } catch (err) {
      setError('Failed to fetch help topics');
    } finally {
      setLoading(false);
    }
  };

  //derive unique section list from existing topics
  const existingSections = Array.from(new Set(topics.map(t => t.section).filter(Boolean)));

  const handleOpenAddModal = () => {
    setEditingTopic(null);
    setFormData({
      section: existingSections[0] || 'Getting Started / Account',
      customSection: '',
      question: '',
      answer: '',
      displayOrder: 1
    });
    setError('');
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (topic) => {
    setEditingTopic(topic);
    setFormData({
      section: topic.section || '',
      customSection: '',
      question: topic.question || '',
      answer: topic.answer || '',
      displayOrder: topic.displayOrder || 0
    });
    setError('');
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingTopic(null);
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'displayOrder' ? parseInt(value || '0', 10) : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const finalSection = formData.section === '__NEW__' 
      ? formData.customSection.trim() 
      : formData.section.trim();

    if (!finalSection) {
      setError('Please provide a valid section name');
      return;
    }
    if (!formData.question.trim()) {
      setError('Question is required');
      return;
    }
    if (!formData.answer.trim()) {
      setError('Answer is required');
      return;
    }

    const payload = {
      section: finalSection,
      question: formData.question.trim(),
      answer: formData.answer.trim(),
      displayOrder: formData.displayOrder || 0
    };

    try {
      if (editingTopic) {
        await helpApi.updateTopic(editingTopic.id, payload);
      } else {
        await helpApi.createTopic(payload);
      }
      handleCloseModal();
      fetchTopics();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Operation failed';
      setError(msg);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deletingTopic) return;
    try {
      await helpApi.deleteTopic(deletingTopic.id);
      setDeletingTopic(null);
      fetchTopics();
    } catch (err) {
      setError('Failed to delete help topic');
    }
  };

  //filter topics based on search query
  const filteredTopics = topics.filter(t => {
    const q = searchQuery.toLowerCase();
    return (
      (t.section && t.section.toLowerCase().includes(q)) ||
      (t.question && t.question.toLowerCase().includes(q)) ||
      (t.answer && t.answer.toLowerCase().includes(q))
    );
  });

  //group filtered topics by section
  const groupedSectionsMap = new Map();
  filteredTopics.forEach(t => {
    const sec = t.section || 'General';
    if (!groupedSectionsMap.has(sec)) {
      groupedSectionsMap.set(sec, []);
    }
    groupedSectionsMap.get(sec).push(t);
  });

  return (
    <div className="admin-help-page">
      <div className="admin-help-header">
        <div>
          <h2>Help Center Management</h2>
          <p>Create, organize, and manage FAQ topics shown on the public Help page.</p>
        </div>
        <div className="admin-help-actions">
          <div className="admin-help-search">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
            <input 
              type="text" 
              placeholder="Search topics…" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <button className="btn-primary-add" onClick={handleOpenAddModal}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            Add Topic
          </button>
        </div>
      </div>

      {error && (
        <div style={{ padding: '12px 16px', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '8px', color: '#f87171', marginBottom: '20px', fontSize: '13.5px' }}>
          {error}
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px', color: 'var(--text-dim)' }}>
          Loading help topics...
        </div>
      ) : groupedSectionsMap.size === 0 ? (
        <div style={{ textAlign: 'center', padding: '50px', background: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)', color: 'var(--text-dim)' }}>
          No topics found. Click "Add Topic" to create one.
        </div>
      ) : (
        Array.from(groupedSectionsMap.entries()).map(([sectionName, sectionItems]) => (
          <div key={sectionName} className="admin-help-section-group">
            <div className="admin-help-section-title">
              <span>{sectionName}</span>
              <span className="admin-help-badge">{sectionItems.length} {sectionItems.length === 1 ? 'entry' : 'entries'}</span>
            </div>
            <table className="admin-help-table">
              <thead>
                <tr>
                  <th style={{ width: '60px' }}>Order</th>
                  <th style={{ width: '35%' }}>Question</th>
                  <th>Answer</th>
                  <th style={{ width: '150px', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {sectionItems.map((topic) => (
                  <tr key={topic.id}>
                    <td>
                      <span className="admin-help-order-badge">#{topic.displayOrder}</span>
                    </td>
                    <td>
                      <div className="admin-help-question-text">{topic.question}</div>
                    </td>
                    <td>
                      <div className="admin-help-answer-snippet">{topic.answer}</div>
                    </td>
                    <td>
                      <div className="admin-help-row-actions">
                        <button className="admin-help-btn-action" onClick={() => handleOpenEditModal(topic)}>
                          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                          Edit
                        </button>
                        <button className="admin-help-btn-action delete" onClick={() => setDeletingTopic(topic)}>
                          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))
      )}

      {/* Add / Edit modal */}
      {isModalOpen && (
        <div className="admin-help-modal-overlay" onClick={handleCloseModal}>
          <div className="admin-help-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-help-modal-header">
              <h3>{editingTopic ? 'Edit Help Topic' : 'Add New Help Topic'}</h3>
              <button className="admin-help-modal-close" onClick={handleCloseModal}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="admin-help-modal-body">
                <div className="admin-help-form-group">
                  <label>Section Category</label>
                  <select 
                    name="section" 
                    className="admin-help-select"
                    value={formData.section} 
                    onChange={handleFormChange}
                  >
                    {existingSections.map(sec => (
                      <option key={sec} value={sec}>{sec}</option>
                    ))}
                    <option value="__NEW__">+ Add new section...</option>
                  </select>
                </div>

                {formData.section === '__NEW__' && (
                  <div className="admin-help-form-group">
                    <label>New Section Name</label>
                    <input 
                      type="text" 
                      name="customSection" 
                      className="admin-help-input"
                      placeholder="e.g. Monetization & Subscriptions"
                      value={formData.customSection}
                      onChange={handleFormChange}
                      required
                    />
                  </div>
                )}

                <div className="admin-help-form-group">
                  <label>Question</label>
                  <input 
                    type="text" 
                    name="question" 
                    className="admin-help-input"
                    placeholder="e.g. How do I upload 4K videos?"
                    value={formData.question}
                    onChange={handleFormChange}
                    required
                  />
                </div>

                <div className="admin-help-form-group">
                  <label>Answer</label>
                  <textarea 
                    name="answer" 
                    className="admin-help-textarea"
                    placeholder="Enter explanatory answer (1-3 sentences)..."
                    value={formData.answer}
                    onChange={handleFormChange}
                    required
                  />
                </div>

                <div className="admin-help-form-group">
                  <label>Display Order</label>
                  <input 
                    type="number" 
                    name="displayOrder" 
                    className="admin-help-input"
                    min="0"
                    value={formData.displayOrder}
                    onChange={handleFormChange}
                    required
                  />
                </div>
              </div>
              <div className="admin-help-modal-footer">
                <button type="button" className="btn btn-ghost" onClick={handleCloseModal}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editingTopic ? 'Save Changes' : 'Create Topic'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirmation modal */}
      {deletingTopic && (
        <div className="admin-help-modal-overlay" onClick={() => setDeletingTopic(null)}>
          <div className="admin-help-modal" style={{ maxWidth: '440px' }} onClick={(e) => e.stopPropagation()}>
            <div className="admin-help-modal-header">
              <h3 style={{ color: '#ef4444' }}>Delete Help Topic</h3>
              <button className="admin-help-modal-close" onClick={() => setDeletingTopic(null)}>✕</button>
            </div>
            <div className="admin-help-modal-body">
              <p style={{ fontSize: '14px', color: 'var(--text-dim)', margin: 0 }}>
                Are you sure you want to permanently delete the topic <strong>"{deletingTopic.question}"</strong>?
              </p>
            </div>
            <div className="admin-help-modal-footer">
              <button type="button" className="btn btn-ghost" onClick={() => setDeletingTopic(null)}>Cancel</button>
              <button type="button" className="btn btn-danger" style={{ background: '#ef4444', color: '#fff', border: 'none', padding: '8px 16px', borderRadius: '8px', cursor: 'pointer' }} onClick={handleDeleteConfirm}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminHelpPage;

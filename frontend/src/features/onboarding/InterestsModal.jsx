import React, { useState, useEffect } from 'react';
import axiosInstance from '../../api/axiosInstance';
import { saveCategoryPreferences, skipOnboarding } from '../../api/onboardingApi';
import './InterestsModal.css';

//onboarding modal for picking initial category interests for fresh users
const InterestsModal = ({ onSuccess }) => {
  const [categories, setCategories] = useState([]);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    //fetch available categories that have at least one video
    const fetchCategories = async () => {
      try {
        setLoading(true);
        const res = await axiosInstance.get('/categories?hasVideos=true');
        const list = Array.isArray(res.data) ? res.data : res.data?.content || [];
        setCategories(list);
      } catch (err) {
        console.error('Failed to load categories:', err);
        setError('Failed to load categories. Please refresh.');
      } finally {
        setLoading(false);
      }
    };

    fetchCategories();
  }, []);

  const toggleCategory = (categoryId) => {
    setError(null);
    setSelectedCategoryIds((prev) =>
      prev.includes(categoryId)
        ? prev.filter((id) => id !== categoryId)
        : [...prev, categoryId]
    );
  };

  const handleSkip = async () => {
    try {
      setSubmitting(true);
      await skipOnboarding();
    } catch (err) {
      console.warn('Skip onboarding error', err);
    } finally {
      setSubmitting(false);
      if (onSuccess) {
        onSuccess();
      }
    }
  };

  const minRequired = Math.min(3, categories.length);
  const selectedCount = selectedCategoryIds.length;
  const isReady = minRequired > 0 ? selectedCount >= minRequired : true;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if ((minRequired > 0 && selectedCategoryIds.length < minRequired) || submitting) return;

    try {
      setSubmitting(true);
      setError(null);
      //save user selected onboarding categories
      await saveCategoryPreferences(selectedCategoryIds);
      
      //notify home feed to refresh personalized recommendations
      window.dispatchEvent(new CustomEvent('streamhub-video-update'));
      
      if (onSuccess) {
        onSuccess();
      }
    } catch (err) {
      console.error('Failed to save category preferences:', err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to save interests. Please try again.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="interests-modal-overlay">
      <div className="interests-modal-card" role="dialog" aria-modal="true" style={{ position: 'relative' }}>
        <button 
          onClick={handleSkip} 
          style={{ position: 'absolute', top: '16px', right: '16px', background: 'none', border: 'none', color: 'var(--text-dim, #94a3b8)', cursor: 'pointer', padding: '6px' }}
          aria-label="Skip"
          title="Skip"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>

        <div className="interests-modal-header">
          <span className="interests-badge">Welcome to StreamHub</span>
          <h2 className="interests-title">Pick Your Interests</h2>
          <p className="interests-subtitle">
            Choose what you enjoy watching so we can personalize your stream feed from day one.
          </p>
        </div>

        {error && <div className="interests-error">{error}</div>}

        <div className="interests-grid-wrapper">
          {loading ? (
            <div style={{ textAlign: 'center', padding: '30px 0', color: 'var(--text-dim)' }}>
              Loading categories...
            </div>
          ) : categories.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '30px 0', color: 'var(--text-dim)' }}>
              No categories with videos available yet.
            </div>
          ) : (
            <div className="interests-grid">
              {categories.map((cat) => {
                const catId = cat.id || cat.categoryId;
                const isSelected = selectedCategoryIds.includes(catId);
                return (
                  <button
                    key={catId}
                    type="button"
                    className={`interest-chip ${isSelected ? 'selected' : ''}`}
                    onClick={() => toggleCategory(catId)}
                  >
                    <span className="interest-chip-name">{cat.name}</span>
                    <div className="interest-check-icon">
                      {isSelected && (
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        <div className="interests-modal-footer">
          <div className="interests-progress-bar">
            <span>
              {categories.length === 0
                ? 'No categories available right now.'
                : isReady
                ? 'Great choices!'
                : `Pick at least ${minRequired - selectedCount} more ${minRequired - selectedCount === 1 ? 'category' : 'categories'}`}
            </span>
            {categories.length > 0 && (
              <span className="interests-count-badge">
                {selectedCount} / {minRequired} selected
              </span>
            )}
          </div>

          <button
            type="button"
            className="interests-submit-btn"
            disabled={!isReady || submitting || loading}
            onClick={handleSubmit}
          >
            {submitting ? (
              'Saving preferences...'
            ) : (
              <>
                Continue to StreamHub
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </>
            )}
          </button>

          <div style={{ textAlign: 'center', marginTop: '12px' }}>
            <button
              type="button"
              onClick={handleSkip}
              disabled={submitting}
              style={{ background: 'none', border: 'none', color: 'var(--text-dim, #94a3b8)', fontSize: '13px', cursor: 'pointer', textDecoration: 'underline' }}
            >
              Skip for now
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InterestsModal;

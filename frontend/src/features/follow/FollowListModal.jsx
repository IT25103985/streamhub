import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getFollowers, getFollowing, unfollowUser } from '../../api/followApi';
import { getFullImageUrl } from '../../utils/imageUtils';
import './FollowListModal.css';

/**
 * Parameterized modal for viewing followers or following list
 * Props:
 * - mode: 'followers' | 'following'
 * - userId: UUID of profile owner
 * - onClose: () => void
 * - onUnfollowSuccess: () => void (called when an unfollow occurs in 'following' mode)
 */
const FollowListModal = ({ mode = 'followers', userId, onClose, onUnfollowSuccess }) => {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [actionLoadingId, setActionLoadingId] = useState(null);

  const fetchList = useCallback(async (pageNumber = 0) => {
    if (!userId) return;
    try {
      setLoading(true);
      setError(null);
      const data = mode === 'followers'
        ? await getFollowers(userId, pageNumber, 10)
        : await getFollowing(userId, pageNumber, 10);

      setUsers(data?.content || []);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements || 0);
      setPage(data?.number || 0);
    } catch (err) {
      console.error(`Failed to fetch ${mode} list:`, err);
      setError(`Failed to load ${mode}. Please try again.`);
    } finally {
      setLoading(false);
    }
  }, [mode, userId]);

  useEffect(() => {
    fetchList(0);
  }, [fetchList]);

  const handleUserClick = (targetUserId) => {
    onClose();
    navigate(`/profile/${targetUserId}`);
  };

  const handleUnfollow = async (e, targetUserId) => {
    e.stopPropagation();
    if (actionLoadingId) return;

    setActionLoadingId(targetUserId);
    const previousUsers = [...users];

    // Optimistic removal
    setUsers(prev => prev.filter(u => u.userId !== targetUserId));
    setTotalElements(prev => Math.max(0, prev - 1));

    try {
      await unfollowUser(targetUserId);
      if (onUnfollowSuccess) {
        onUnfollowSuccess();
      }
    } catch (err) {
      // Rollback on failure
      setUsers(previousUsers);
      setTotalElements(previousUsers.length);
      alert('Failed to unfollow user. Please try again.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const isFollowersMode = mode === 'followers';
  const title = isFollowersMode ? 'Followers' : 'Following';

  return (
    <div className="follow-modal-overlay" onClick={onClose}>
      <div className="follow-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="follow-modal-header">
          <h2 className="follow-modal-title">
            {title} {totalElements > 0 && <span style={{ color: 'var(--text-dim)', fontSize: '0.9rem', fontWeight: 500 }}>({totalElements})</span>}
          </h2>
          <button
            type="button"
            className="follow-modal-close-btn"
            onClick={onClose}
            aria-label="Close modal"
          >
            ✕
          </button>
        </div>

        <div className="follow-modal-body">
          {loading ? (
            <div className="follow-modal-loading">
              <div className="skeleton-pulse" style={{ width: '40px', height: '40px', borderRadius: '50%' }} />
            </div>
          ) : error ? (
            <div className="follow-modal-empty" style={{ color: '#ef4444' }}>
              <p>{error}</p>
              <button
                className="follow-page-btn"
                onClick={() => fetchList(page)}
              >
                Try Again
              </button>
            </div>
          ) : users.length === 0 ? (
            <div className="follow-modal-empty">
              <p>
                {isFollowersMode
                  ? 'No followers yet.'
                  : 'Not following anyone yet.'}
              </p>
            </div>
          ) : (
            users.map((item) => {
              const avatar = getFullImageUrl(item.avatarUrl);
              const initials = (item.displayName || item.username || '?').charAt(0).toUpperCase();

              return (
                <div key={item.userId} className="follow-user-row">
                  <div
                    className="follow-user-info-link"
                    onClick={() => handleUserClick(item.userId)}
                  >
                    {avatar ? (
                      <img
                        src={avatar}
                        alt={item.displayName || item.username}
                        className="follow-user-avatar"
                      />
                    ) : (
                      <div className="follow-user-avatar-placeholder">
                        {initials}
                      </div>
                    )}
                    <div className="follow-user-text">
                      <span className="follow-user-display-name">
                        {item.displayName || item.username}
                      </span>
                      <span className="follow-user-username">
                        @{item.username}
                      </span>
                    </div>
                  </div>

                  {!isFollowersMode && (
                    <button
                      type="button"
                      className="follow-unfollow-btn"
                      onClick={(e) => handleUnfollow(e, item.userId)}
                      disabled={actionLoadingId === item.userId}
                    >
                      {actionLoadingId === item.userId ? '...' : 'Unfollow'}
                    </button>
                  )}
                </div>
              );
            })
          )}
        </div>

        {totalPages > 1 && (
          <div className="follow-modal-footer">
            <span className="follow-pagination-info">
              Page {page + 1} of {totalPages}
            </span>
            <div className="follow-pagination-controls">
              <button
                type="button"
                className="follow-page-btn"
                onClick={() => fetchList(page - 1)}
                disabled={page === 0 || loading}
              >
                Previous
              </button>
              <button
                type="button"
                className="follow-page-btn"
                onClick={() => fetchList(page + 1)}
                disabled={page >= totalPages - 1 || loading}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default FollowListModal;

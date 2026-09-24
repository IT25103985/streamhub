import React, { useState, useEffect, useContext, useRef } from 'react';
import { AuthContext } from '../context/AuthContext';
import { followUser, unfollowUser, getFollowCounts } from '../api/followApi';
import websocketClient from '../api/websocketClient';
import './FollowButton.css';

/**
 * Shared FollowButton component
 * Props:
 * - targetUserId: UUID of user to follow/unfollow
 * - initialIsFollowing: (optional) initial boolean state to skip initial fetch
 * - onFollowChange: (optional) callback (isFollowing, targetUserId) => void
 * - size: 'sm' | 'md' | 'lg' (default: 'md')
 * - className: (optional) custom css class
 */
const FollowButton = ({
  targetUserId,
  initialIsFollowing = null,
  onFollowChange,
  size = 'md',
  className = ''
}) => {
  const { user } = useContext(AuthContext);
  const [isFollowing, setIsFollowing] = useState(
    typeof initialIsFollowing === 'boolean' ? initialIsFollowing : false
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const subscriptionRef = useRef(null);

  // Sync initialIsFollowing if prop updates
  useEffect(() => {
    if (typeof initialIsFollowing === 'boolean') {
      setIsFollowing(initialIsFollowing);
    }
  }, [initialIsFollowing]);

  // Never render for guest or self
  const isGuest = !user;
  const isSelf = user && targetUserId && (user.userId === targetUserId || user.id === targetUserId);

  // Fetch initial status if not provided via props
  useEffect(() => {
    if (isGuest || isSelf || !targetUserId) return;
    if (typeof initialIsFollowing === 'boolean') return;

    let isMounted = true;
    const fetchStatus = async () => {
      try {
        const data = await getFollowCounts(targetUserId);
        if (isMounted && data) {
          setIsFollowing(Boolean(data.isFollowing));
        }
      } catch (err) {
        // silent fail on background fetch
      }
    };

    fetchStatus();
    return () => {
      isMounted = false;
    };
  }, [targetUserId, isGuest, isSelf, initialIsFollowing]);

  // STOMP WebSocket live count and status subscription
  useEffect(() => {
    if (isGuest || isSelf || !targetUserId) return;

    // Subscribe to live follow updates on /user/queue/follow-counts
    const sub = websocketClient.subscribe('/user/queue/follow-counts', (payload) => {
      if (payload && payload.userId === targetUserId && typeof payload.isFollowing === 'boolean') {
        setIsFollowing(payload.isFollowing);
      }
    });

    subscriptionRef.current = sub;

    return () => {
      if (subscriptionRef.current && typeof subscriptionRef.current.unsubscribe === 'function') {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
    };
  }, [targetUserId, isGuest, isSelf]);

  if (isGuest || isSelf || !targetUserId) {
    return null;
  }

  const handleToggle = async (e) => {
    e.stopPropagation();
    if (loading) return;

    const previousState = isFollowing;
    const nextState = !previousState;

    // Optimistic UI update
    setIsFollowing(nextState);
    setError(null);
    setLoading(true);

    if (onFollowChange) {
      onFollowChange(nextState, targetUserId);
    }

    try {
      if (nextState) {
        await followUser(targetUserId);
      } else {
        await unfollowUser(targetUserId);
      }
    } catch (err) {
      // Roll back state on failure
      setIsFollowing(previousState);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to update follow';
      setError(typeof msg === 'string' ? msg : 'Action failed');

      if (onFollowChange) {
        onFollowChange(previousState, targetUserId);
      }

      // Clear error after 4 seconds
      setTimeout(() => setError(null), 4000);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`follow-button-wrapper ${className}`}>
      <button
        type="button"
        className={`follow-btn follow-btn-${size} ${isFollowing ? 'is-following' : 'not-following'}`}
        onClick={handleToggle}
        disabled={loading}
        title={isFollowing ? 'Following' : 'Follow'}
      >
        {loading ? (
          <span className="follow-btn-spinner" />
        ) : isFollowing ? (
          <span className="follow-btn-text">
            <span>Following</span>
          </span>
        ) : (
          <span className="follow-btn-text">Follow</span>
        )}
      </button>

      {error && <div className="follow-btn-error">{error}</div>}
    </div>
  );
};

export default FollowButton;

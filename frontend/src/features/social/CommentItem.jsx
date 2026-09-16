import { useState, useContext } from 'react';
import { AuthContext, useRequireAuth } from '../../context/AuthContext';
import { Link } from 'react-router-dom';
import { getFullImageUrl } from '../../utils/imageUtils';
import OptionsMenu from '../../components/OptionsMenu';
import CommentForm from './CommentForm';
import { socialService } from './socialService';

//renders single comment with author avatar timestamp and reply thread
const CommentItem = ({ 
  comment, 
  videoId,
  onCommentEdited, 
  onCommentDeleted, 
  onReplyAdded,
  onReport
}) => {
  const { user } = useContext(AuthContext);
  const requireAuth = useRequireAuth();
  
  const [isEditing, setIsEditing] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [editText, setEditText] = useState(comment.text);
  
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const [isReplying, setIsReplying] = useState(false);
  const [showReplies, setShowReplies] = useState(false);

  const isAuthor = user && (user.userId === comment.authorId || user.username === comment.authorUsername);
  const hasReplies = comment.replies && comment.replies.length > 0;

  const menuItems = [];

  if (!isAuthor) {
    menuItems.push({ label: 'Report', onClick: () => requireAuth(() => onReport(comment.commentId)) });
  }

  if (isAuthor) {
    menuItems.push({ label: 'Edit', onClick: () => setIsEditing(true) });
    menuItems.push({ label: 'Delete', onClick: () => setIsDeleting(true) });
  }

  const handleEditSave = async () => {
    if (!editText.trim()) return;
    setEditLoading(true);
    try {
      const updated = await socialService.updateComment(comment.commentId, { text: editText });
      onCommentEdited(updated);
      setIsEditing(false);
    } catch (err) {
      console.error('Failed to update comment', err);
    } finally {
      setEditLoading(false);
    }
  };

  const handleDeleteConfirm = async () => {
    setDeleteLoading(true);
    try {
      await socialService.deleteComment(comment.commentId);
      onCommentDeleted(comment.commentId, comment.parentCommentId);
    } catch (err) {
      console.error('Failed to delete comment', err);
    } finally {
      setDeleteLoading(false);
    }
  };

  //comment thread card section
  return (
    <div className="comment-thread">
      <div className={`comment-item ${isDeleting ? 'comment-item-deleting' : ''}`}>
        <div className="comment-header" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
          <Link to={`/profile/${comment.authorId}`} style={{ display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none', color: 'inherit' }}>
            {comment.authorAvatarUrl ? (
              <img 
                src={getFullImageUrl(comment.authorAvatarUrl)} 
                alt={`${comment.authorName || comment.authorUsername}'s avatar`} 
                className="comment-avatar"
              />
            ) : (
              <span className="comment-avatar-fallback">
                {(comment.authorName || comment.authorUsername).charAt(0).toUpperCase()}
              </span>
            )}
            <span className="comment-author" style={{ "&:hover": { textDecoration: 'underline' } }}>{comment.authorName || comment.authorUsername}</span>
          </Link>
          <span className="comment-date">
            {new Date(comment.createdAt).toLocaleDateString(undefined, {
              year: 'numeric',
              month: 'short',
              day: 'numeric'
            })}
          </span>
          <div style={{ marginLeft: 'auto' }}>
            <OptionsMenu items={menuItems} />
          </div>
        </div>

        
        {isDeleting ? (
          <div className="comment-delete-confirm">
            <p>Are you sure you want to delete this comment?</p>
            <div className="comment-actions-inline">
              <button 
                onClick={handleDeleteConfirm} 
                disabled={deleteLoading}
                className="btn-danger-inline"
              >
                {deleteLoading ? 'Deleting...' : 'Delete'}
              </button>
              <button 
                onClick={() => setIsDeleting(false)} 
                disabled={deleteLoading}
                className="btn-cancel-inline"
              >
                Cancel
              </button>
            </div>
          </div>
        ) : isEditing ? (
          <div className="comment-edit-ui">
            <textarea
              className="comment-edit-textarea"
              value={editText}
              onChange={(e) => setEditText(e.target.value)}
              disabled={editLoading}
            />
            <div className="comment-actions-inline">
              <button 
                onClick={handleEditSave} 
                disabled={editLoading || !editText.trim()}
                className="btn-primary-inline"
              >
                {editLoading ? 'Saving...' : 'Save'}
              </button>
              <button 
                onClick={() => {
                  setEditText(comment.text);
                  setIsEditing(false);
                }} 
                disabled={editLoading}
                className="btn-cancel-inline"
              >
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <div className="comment-text">{comment.text}</div>
        )}

        
        {!isDeleting && !isEditing && (
          <div className="comment-actions-bar">
            <button 
              className="btn-reply-text" 
              onClick={() => requireAuth(() => setIsReplying(!isReplying))}
            >
              Reply
            </button>
          </div>
        )}

        
        {isReplying && (
          <div className="comment-reply-form">
            <CommentForm 
              videoId={videoId} 
              parentCommentId={comment.parentCommentId || comment.commentId}
              onCommentAdded={(newReply) => {
                onReplyAdded(newReply);
                setIsReplying(false);
                setShowReplies(true);
              }} 
            />
          </div>
        )}
      </div>

      
      {hasReplies && (
        <div className="comment-replies-section">
          <button 
            className="btn-toggle-replies" 
            onClick={() => setShowReplies(!showReplies)}
          >
            {showReplies ? 'Hide replies' : `View ${comment.replies.length} replies`}
          </button>

          {showReplies && (
            <div className="comment-replies-list">
              {comment.replies.map(reply => (
                <CommentItem 
                  key={reply.commentId} 
                  comment={reply} 
                  videoId={videoId}
                  onCommentEdited={onCommentEdited}
                  onCommentDeleted={onCommentDeleted}
                  onReplyAdded={onReplyAdded}
                  onReport={onReport}
                />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CommentItem;

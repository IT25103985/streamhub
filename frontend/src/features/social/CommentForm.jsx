import { useState } from 'react';
import axiosInstance from '../../api/axiosInstance';
import { useRequireAuth } from '../../context/AuthContext';
import './Comments.css';

//input box for submitting new top level or reply comments
const CommentForm = ({ videoId, parentCommentId, onCommentAdded }) => {
  const [text, setText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const requireAuth = useRequireAuth();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!text.trim()) return;

    requireAuth(() => {
      setSubmitting(true);
      const payload = { text: text.trim() };
      if (parentCommentId) {
        payload.parentCommentId = parentCommentId;
      }
      
      axiosInstance.post(`/videos/${videoId}/comments`, payload)
        .then(res => {
          if (onCommentAdded) {
            onCommentAdded(res.data);
          }
          setText('');
        })
        .catch(err => {
          console.error('Failed to post comment', err);
        })
        .finally(() => {
          setSubmitting(false);
        });
    });
  };

  return (
    <form className="comment-form" onSubmit={handleSubmit}>
      <textarea
        className="comment-textarea"
        placeholder="Add a comment..."
        value={text}
        onChange={(e) => setText(e.target.value)}
        rows="3"
        maxLength="2000"
      />
      <div className="comment-actions">
        <button type="submit" className="btn btn-primary" disabled={submitting || !text.trim()}>
          {submitting ? 'Posting...' : 'Post Comment'}
        </button>
      </div>
    </form>
  );
};

export default CommentForm;

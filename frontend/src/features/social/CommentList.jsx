import { useEffect, useState } from 'react';
import axiosInstance from '../../api/axiosInstance';
import CommentForm from './CommentForm';
import CommentSkeleton from '../../components/skeletons/CommentSkeleton';
import ReportModal from '../report/ReportModal';
import './Comments.css';

import CommentItem from './CommentItem';

//loads and displays paginated top level comments with infinite scroll
const CommentList = ({ videoId }) => {
  const [reportCommentId, setReportCommentId] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    if (!videoId) return;

    const fetchComments = async () => {
      setLoading(true);
      try {
        const res = await axiosInstance.get(`/comments?videoId=${videoId}&page=0&size=10`);
        const data = res.data.content || res.data;
        setComments(data);
        setHasMore(res.data.last !== undefined ? !res.data.last : data.length === 10);
      } catch (err) {
        console.error('Failed to fetch comments', err);
      } finally {
        setLoading(false);
      }
    };

    fetchComments();
  }, [videoId]);

  const loadMore = () => {
    const nextPage = page + 1;
    axiosInstance.get(`/comments?videoId=${videoId}&page=${nextPage}&size=10`)
      .then(res => {
        const data = res.data.content || res.data;
        setComments(prev => [...prev, ...data]);
        setHasMore(res.data.last !== undefined ? !res.data.last : data.length === 10);
        setPage(nextPage);
      })
      .catch(err => {
        console.error('Failed to fetch more comments', err);
      });
  };

  const handleCommentAdded = (newComment) => {
    setComments(prev => [newComment, ...prev]);
  };

  const handleCommentEdited = (updatedComment) => {
    setComments(prev => {
      //update nested comment tree or single level list
      return prev.map(c => {
        if (c.commentId === updatedComment.commentId) {
          return { ...c, ...updatedComment };
        }
        if (c.replies) {
          return {
            ...c,
            replies: c.replies.map(r => r.commentId === updatedComment.commentId ? { ...r, ...updatedComment } : r)
          };
        }
        return c;
      });
    });
  };

  const handleCommentDeleted = (commentId, parentCommentId) => {
    setComments(prev => {
      if (!parentCommentId) {
        return prev.filter(c => c.commentId !== commentId);
      }
      return prev.map(c => {
        if (c.commentId === parentCommentId && c.replies) {
          return {
            ...c,
            replies: c.replies.filter(r => r.commentId !== commentId)
          };
        }
        return c;
      });
    });
  };

  const handleReplyAdded = (newReply) => {
    setComments(prev => prev.map(c => {
      if (c.commentId === newReply.parentCommentId) {
        return {
          ...c,
          replies: [...(c.replies || []), newReply]
        };
      }
      return c;
    }));
  };

  //comment list stream section
  return (
    <div className="comment-section">
      <h3 className="comment-section-title">Comments</h3>
      <CommentForm videoId={videoId} onCommentAdded={handleCommentAdded} />
      
      {loading && comments.length === 0 ? (
        <div className="comment-list" style={{ marginTop: '16px' }}>
          <CommentSkeleton />
          <CommentSkeleton />
          <CommentSkeleton />
        </div>
      ) : (
        <>
          {!loading && comments.length === 0 && (
            <div className="comment-empty">No comments yet. Be the first to share your thoughts!</div>
          )}

          <div className="comment-list">
            {comments.map(comment => (
              <CommentItem 
                key={comment.commentId} 
                comment={comment} 
                videoId={videoId}
                onCommentEdited={handleCommentEdited}
                onCommentDeleted={handleCommentDeleted}
                onReplyAdded={handleReplyAdded}
                onReport={(id) => setReportCommentId(id)}
              />
            ))}
            {loading && comments.length > 0 && (
              <>
                <CommentSkeleton />
                <CommentSkeleton />
              </>
            )}
          </div>
        </>
      )}

      {hasMore && !loading && (
        <button className="btn-load-more-comments" onClick={loadMore}>
          Load more comments
        </button>
      )}

      {reportCommentId && (
        <ReportModal
          reportableType="COMMENT"
          reportableId={reportCommentId}
          onClose={() => setReportCommentId(null)}
          onSuccess={() => setReportCommentId(null)}
        />
      )}
    </div>
  );
};

export default CommentList;

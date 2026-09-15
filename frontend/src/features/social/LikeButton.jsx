import React, { useState, useEffect } from 'react';
import axiosInstance from '../../api/axiosInstance';
import { useRequireAuth } from '../../context/AuthContext';
import SkeletonBase from '../../components/skeletons/SkeletonBase';
import './LikeButton.css';

//like and dislike reaction toggle bar with live counter updates
const LikeButton = ({ videoId }) => {
  const [likeCount, setLikeCount] = useState(0);
  const [dislikeCount, setDislikeCount] = useState(0);
  const [isLiked, setIsLiked] = useState(false);
  const [isDisliked, setIsDisliked] = useState(false);
  const [loading, setLoading] = useState(true);
  const requireAuth = useRequireAuth();

  useEffect(() => {
    if (!videoId) return;

    const fetchLikeStatus = async () => {
      setLoading(true);
      try {
        const res = await axiosInstance.get(`/videos/${videoId}/like-status`);
        setLikeCount(res.data.likeCount || 0);
        setDislikeCount(res.data.dislikeCount || 0);
        setIsLiked(res.data.userLiked || false);
        setIsDisliked(res.data.userDisliked || false);
      } catch (err) {
        console.error('Failed to fetch like status', err);
      } finally {
        setLoading(false);
      }
    };
    
    fetchLikeStatus();
  }, [videoId]);

  const handleLikeClick = () => {
    requireAuth(() => {
      //optimistic update before api response
      const previousLikeCount = likeCount;
      const previousDislikeCount = dislikeCount;
      const previousLiked = isLiked;
      const previousDisliked = isDisliked;
      
      const newIsLiked = !isLiked;
      setIsLiked(newIsLiked);
      setLikeCount(prev => newIsLiked ? prev + 1 : prev - 1);
      
      //clear opposite reaction state when liking
      if (newIsLiked && isDisliked) {
        setIsDisliked(false);
        setDislikeCount(prev => prev - 1);
      }

      axiosInstance.post(`/videos/${videoId}/like`)
        .then(res => {
            //sync state with server response
            setLikeCount(res.data.likeCount);
            setIsLiked(res.data.userLiked);
            setDislikeCount(res.data.dislikeCount);
            setIsDisliked(res.data.userDisliked);
        })
        .catch(err => {
          console.error('Failed to toggle like', err);
          //revert local state on network failure
          setLikeCount(previousLikeCount);
          setIsLiked(previousLiked);
          setDislikeCount(previousDislikeCount);
          setIsDisliked(previousDisliked);
        });
    });
  };

  const handleDislikeClick = () => {
    requireAuth(() => {
      //optimistic update before api response
      const previousLikeCount = likeCount;
      const previousDislikeCount = dislikeCount;
      const previousLiked = isLiked;
      const previousDisliked = isDisliked;
      
      const newIsDisliked = !isDisliked;
      setIsDisliked(newIsDisliked);
      setDislikeCount(prev => newIsDisliked ? prev + 1 : prev - 1);
      
      //clear opposite reaction state when disliking
      if (newIsDisliked && isLiked) {
        setIsLiked(false);
        setLikeCount(prev => prev - 1);
      }

      axiosInstance.post(`/videos/${videoId}/dislike`)
        .then(res => {
            //sync state with server response
            setLikeCount(res.data.likeCount);
            setIsLiked(res.data.userLiked);
            setDislikeCount(res.data.dislikeCount);
            setIsDisliked(res.data.userDisliked);
        })
        .catch(err => {
          console.error('Failed to toggle dislike', err);
          //revert local state on network failure
          setLikeCount(previousLikeCount);
          setIsLiked(previousLiked);
          setDislikeCount(previousDislikeCount);
          setIsDisliked(previousDisliked);
        });
    });
  };

  if (loading) {
    return <SkeletonBase width="140px" height="36px" borderRadius="18px" />;
  }

  const formatCount = (count) => {
    return new Intl.NumberFormat('en-US', { notation: "compact", compactDisplay: "short" }).format(count);
  };

  //like dislike reaction button section
  return (
    <>
      <button 
        className={`btn-like ${isLiked ? 'active' : ''}`} 
        onClick={handleLikeClick}
        aria-label={isLiked ? "Unlike" : "Like this video"}
      >
        <svg 
          width="20" 
          height="20" 
          viewBox="0 0 24 24" 
          fill={isLiked ? "currentColor" : "none"} 
          stroke="currentColor" 
          strokeWidth="2" 
          strokeLinecap="round" 
          strokeLinejoin="round"
        >
          <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>
        </svg>
        <span>{formatCount(likeCount)}</span>
      </button>
      <div className="yt-action-divider"></div>
      <button 
        className={`btn-dislike ${isDisliked ? 'active' : ''}`} 
        onClick={handleDislikeClick}
        aria-label={isDisliked ? "Remove dislike" : "Dislike this video"}
      >
        <svg 
          width="20" 
          height="20" 
          viewBox="0 0 24 24" 
          fill={isDisliked ? "currentColor" : "none"} 
          stroke="currentColor" 
          strokeWidth="2" 
          strokeLinecap="round" 
          strokeLinejoin="round"
        >
          <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3zm7-13h2.67A2.31 2.31 0 0 1 22 4v7a2.31 2.31 0 0 1-2.33 2H17"></path>
        </svg>
      </button>
    </>
  );
};

export default LikeButton;

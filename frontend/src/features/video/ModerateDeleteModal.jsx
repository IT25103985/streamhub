import React, { useState } from 'react';
import axiosInstance from '../../api/axiosInstance';
import { getFullImageUrl } from '../../utils/imageUtils';
import './ModerateDeleteModal.css';

const PRESET_REASONS = [
  'Violates Community Guidelines',
  'Copyright or Intellectual Property Infringement',
  'Inappropriate or Explicit Content',
  'Harassment, Bullying, or Hate Speech',
  'Spam, Scam, or Misleading Metadata',
  'Dangerous or Harmful Activities',
  'Other / Custom Reason'
];

//modal prompt for moderators to input reason when deleting content
const ModerateDeleteModal = ({ video, onClose, onSuccess }) => {
  const [selectedReason, setSelectedReason] = useState(PRESET_REASONS[0]);
  const [customDetails, setCustomDetails] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (!video) return null;

  const videoId = video.videoId || video.id;
  const title = video.title || 'Untitled Video';
  const creator = video.uploaderName || video.uploaderUsername || video.ownerUsername || 'Unknown';

  const handleConfirm = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');

    const finalReason = selectedReason === 'Other / Custom Reason'
      ? (customDetails.trim() || 'Violation of platform community guidelines')
      : (customDetails.trim() ? `${selectedReason} - ${customDetails.trim()}` : selectedReason);

    try {
      await axiosInstance.delete(`/videos/${videoId}/moderate`, {
        params: { reason: finalReason }
      });

      window.dispatchEvent(new CustomEvent('streamhub-video-update', { detail: { videoId } }));
      if (onSuccess) onSuccess(videoId);
      if (onClose) onClose();
    } catch (err) {
      console.error('Failed to moderate delete video:', err);
      setError(err.response?.data?.message || 'Failed to remove video. Please verify your moderator privileges.');
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mod-delete-overlay" onClick={onClose}>
      <div className="mod-delete-modal" onClick={(e) => e.stopPropagation()}>
        <div className="mod-delete-header">
          <div className="mod-delete-title-wrap">
            <div className="mod-delete-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
            </div>
            <h3>Remove Video</h3>
          </div>
          <button className="mod-delete-close" onClick={onClose} aria-label="Close modal">×</button>
        </div>

        <form onSubmit={handleConfirm} className="mod-delete-body">
          
          <div className="mod-delete-target">
            <div className="mod-delete-thumb">
              {video.thumbnailUrl ? (
                <img src={getFullImageUrl(video.thumbnailUrl)} alt={title} />
              ) : (
                <div className="mod-delete-no-thumb">No Preview</div>
              )}
            </div>
            <div className="mod-delete-target-info">
              <h4 className="mod-delete-video-title">{title}</h4>
              <p className="mod-delete-video-creator">Uploaded by: <strong>{creator}</strong></p>
            </div>
          </div>

          <div className="mod-delete-notice">
            The video will be permanently removed, and a notification citing the reason below will be delivered to <strong>{creator}</strong>.
          </div>

          {error && <div className="mod-delete-error">{error}</div>}

          
          <div className="mod-delete-form-group">
            <label htmlFor="mod-reason-select">Select Reason for Removal *</label>
            <select
              id="mod-reason-select"
              value={selectedReason}
              onChange={(e) => setSelectedReason(e.target.value)}
              disabled={isSubmitting}
              className="mod-delete-select"
            >
              {PRESET_REASONS.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
          </div>

          
          <div className="mod-delete-form-group">
            <label htmlFor="mod-details-text">
              {selectedReason === 'Other / Custom Reason' ? 'Explanation / Notes *' : 'Additional Notes (Optional)'}
            </label>
            <textarea
              id="mod-details-text"
              rows={3}
              placeholder={selectedReason === 'Other / Custom Reason' ? 'Describe the reason for removing this video...' : 'Add any specific context for the user notification...'}
              value={customDetails}
              onChange={(e) => setCustomDetails(e.target.value)}
              disabled={isSubmitting}
              className="mod-delete-textarea"
              required={selectedReason === 'Other / Custom Reason'}
            />
          </div>

          <div className="mod-delete-actions">
            <button
              type="button"
              className="mod-btn mod-btn-cancel"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="mod-btn mod-btn-confirm"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Removing Video...' : 'Remove Video & Notify'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ModerateDeleteModal;

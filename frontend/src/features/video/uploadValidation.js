export const MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
export const MAX_THUMBNAIL_SIZE = 2 * 1024 * 1024; // 2MB
export const ALLOWED_THUMBNAIL_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

/**
 * Validates video upload form fields and files according to backend validation rules.
 * @param {Object} formData
 * @param {string} formData.title
 * @param {string} formData.description
 * @param {string} formData.categoryId
 * @param {File|null} formData.file
 * @param {File|null} formData.thumbnail
 * @returns {Object} Key-value errors map (empty if valid)
 */
export const validateUploadForm = ({ title, description, categoryId, file, thumbnail }) => {
  const errors = {};

  if (!title || !title.trim()) {
    errors.title = 'Title is required';
  }

  if (!description || !description.trim()) {
    errors.description = 'Description is required';
  }

  if (!categoryId || !String(categoryId).trim()) {
    errors.categoryId = 'Category is required';
  }

  if (!file || file.size === 0) {
    errors.file = 'Cannot upload empty file or thumbnail';
  } else if (file.size > MAX_VIDEO_SIZE) {
    errors.file = 'Video file size exceeds 100MB limit';
  } else if (!file.type || !file.type.startsWith('video/')) {
    errors.file = 'Invalid file type. Only video files are allowed';
  }

  if (!thumbnail || thumbnail.size === 0) {
    errors.thumbnail = 'Cannot upload empty file or thumbnail';
  } else if (thumbnail.size > MAX_THUMBNAIL_SIZE) {
    errors.thumbnail = 'Thumbnail file size exceeds 2MB limit';
  } else if (!thumbnail.type || !ALLOWED_THUMBNAIL_TYPES.includes(thumbnail.type)) {
    errors.thumbnail = 'Invalid thumbnail type. Only jpg, png, and webp are allowed';
  }

  return errors;
};

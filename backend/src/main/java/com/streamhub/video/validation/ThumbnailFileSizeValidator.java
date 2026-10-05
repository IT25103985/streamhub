package com.streamhub.video.validation;

import org.springframework.web.multipart.MultipartFile;

//validates that the thumbnail file size does not exceed the 2MB limit
public class ThumbnailFileSizeValidator extends UploadValidator {

    public static final long MAX_THUMBNAIL_SIZE = 2 * 1024 * 1024; // 2MB

    @Override
    public void validate(MultipartFile file, MultipartFile thumbnail) {
        if (thumbnail.getSize() > MAX_THUMBNAIL_SIZE) {
            throw new IllegalArgumentException("Thumbnail file size exceeds 2MB limit");
        }
        checkNext(file, thumbnail);
    }
}

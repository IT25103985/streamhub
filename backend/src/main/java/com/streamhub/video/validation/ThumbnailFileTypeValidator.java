package com.streamhub.video.validation;

import org.springframework.web.multipart.MultipartFile;

//validates that the uploaded thumbnail is an allowed image MIME type
public class ThumbnailFileTypeValidator extends UploadValidator {

    @Override
    public void validate(MultipartFile file, MultipartFile thumbnail) {
        String thumbContentType = thumbnail.getContentType();
        if (thumbContentType == null || (!thumbContentType.equals("image/jpeg") && !thumbContentType.equals("image/png") && !thumbContentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Invalid thumbnail type. Only jpg, png, and webp are allowed");
        }
        checkNext(file, thumbnail);
    }
}

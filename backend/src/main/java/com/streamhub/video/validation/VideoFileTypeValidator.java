package com.streamhub.video.validation;

import org.springframework.web.multipart.MultipartFile;

//validates that the uploaded file is an allowed video MIME type
public class VideoFileTypeValidator extends UploadValidator {

    @Override
    public void validate(MultipartFile file, MultipartFile thumbnail) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("Invalid file type. Only video files are allowed");
        }
        checkNext(file, thumbnail);
    }
}

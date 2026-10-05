package com.streamhub.video.validation;

import org.springframework.web.multipart.MultipartFile;

//validates that neither the video file nor the thumbnail is empty
public class EmptyUploadValidator extends UploadValidator {

    @Override
    public void validate(MultipartFile file, MultipartFile thumbnail) {
        if (file.isEmpty() || thumbnail.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file or thumbnail");
        }
        checkNext(file, thumbnail);
    }
}

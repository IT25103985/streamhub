package com.streamhub.video.validation;

import org.springframework.web.multipart.MultipartFile;

//validates that the video file size does not exceed the 100MB limit
public class VideoFileSizeValidator extends UploadValidator {

    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    @Override
    public void validate(MultipartFile file, MultipartFile thumbnail) {
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("Video file size exceeds 100MB limit");
        }
        checkNext(file, thumbnail);
    }
}

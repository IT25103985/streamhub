package com.streamhub.video.validation;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

//assembles and executes the sequential validation chain for video uploads
@Component
public class UploadValidatorChain {

    private final UploadValidator head;

    public UploadValidatorChain() {
        EmptyUploadValidator emptyUploadValidator = new EmptyUploadValidator();
        VideoFileSizeValidator videoFileSizeValidator = new VideoFileSizeValidator();
        ThumbnailFileSizeValidator thumbnailFileSizeValidator = new ThumbnailFileSizeValidator();
        VideoFileTypeValidator videoFileTypeValidator = new VideoFileTypeValidator();
        ThumbnailFileTypeValidator thumbnailFileTypeValidator = new ThumbnailFileTypeValidator();

        emptyUploadValidator
            .setNext(videoFileSizeValidator)
            .setNext(thumbnailFileSizeValidator)
            .setNext(videoFileTypeValidator)
            .setNext(thumbnailFileTypeValidator);

        this.head = emptyUploadValidator;
    }

    //executes the full chain of responsibility validation
    public void validate(MultipartFile file, MultipartFile thumbnail) {
        if (head != null) {
            head.validate(file, thumbnail);
        }
    }
}

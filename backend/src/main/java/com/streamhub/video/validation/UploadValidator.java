package com.streamhub.video.validation;

import org.springframework.web.multipart.MultipartFile;

//abstract handler for multipart video upload validation chain
public abstract class UploadValidator {

    protected UploadValidator next;

    //links the next validator in the processing sequence
    public UploadValidator setNext(UploadValidator next) {
        this.next = next;
        return next;
    }

    //executes specific validation rule on incoming video and thumbnail files
    public abstract void validate(MultipartFile file, MultipartFile thumbnail);

    //passes validation to the next handler if configured
    protected void checkNext(MultipartFile file, MultipartFile thumbnail) {
        if (next != null) {
            next.validate(file, thumbnail);
        }
    }
}

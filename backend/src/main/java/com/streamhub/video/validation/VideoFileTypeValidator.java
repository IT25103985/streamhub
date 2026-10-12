package com.streamhub.video.validation;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

//validates that the uploaded file is an allowed video MIME type and has valid video magic bytes
public class VideoFileTypeValidator extends UploadValidator {

    @Override
    public void validate(MultipartFile file, MultipartFile thumbnail) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("Invalid file type. Only video files are allowed");
        }

        //inspect magic bytes to prevent disguised non-video files
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 8 || !isValidVideoHeader(header, read)) {
                throw new IllegalArgumentException("Invalid file type. Only video files are allowed");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file type. Only video files are allowed");
        }

        checkNext(file, thumbnail);
    }

    private boolean isValidVideoHeader(byte[] header, int length) {
        // MP4 / M4V / MOV ISO Base Media format: bytes 4-7 are 'ftyp'
        if (length >= 8 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
            return true;
        }
        // WebM / MKV: 0x1A 0x45 0xDF 0xA3
        if (length >= 4 && (header[0] & 0xFF) == 0x1A && (header[1] & 0xFF) == 0x45 
                && (header[2] & 0xFF) == 0xDF && (header[3] & 0xFF) == 0xA3) {
            return true;
        }
        // AVI: RIFF....AVI 
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'A' && header[9] == 'V' && header[10] == 'I' && header[11] == ' ') {
            return true;
        }
        // FLV: FLV\x01
        if (length >= 4 && header[0] == 'F' && header[1] == 'L' && header[2] == 'V') {
            return true;
        }
        return false;
    }
}

package com.streamhub.video.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class MediaDurationUtil {

    /**
     * Extracts duration in seconds from an MP4 / ISO-BMFF video file.
     * Returns null if duration cannot be extracted or file is not valid.
     */
    public static Integer extractMp4Duration(Path filePath) {
        if (filePath == null) return null;
        File file = filePath.toFile();
        if (!file.exists() || !file.isFile() || file.length() < 16) {
            return null;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {

            long fileSize = channel.size();
            long position = 0;
            ByteBuffer headerBuffer = ByteBuffer.allocate(8);

            while (position + 8 <= fileSize) {
                channel.position(position);
                headerBuffer.clear();
                int read = channel.read(headerBuffer);
                if (read < 8) break;
                headerBuffer.flip();

                long boxSize = headerBuffer.getInt() & 0xFFFFFFFFL;
                byte[] typeBytes = new byte[4];
                headerBuffer.get(typeBytes);
                String boxType = new String(typeBytes, StandardCharsets.US_ASCII);

                long boxHeaderSize = 8;
                if (boxSize == 1) { // 64-bit large box size
                    ByteBuffer largeSizeBuffer = ByteBuffer.allocate(8);
                    if (channel.read(largeSizeBuffer) < 8) break;
                    largeSizeBuffer.flip();
                    boxSize = largeSizeBuffer.getLong();
                    boxHeaderSize = 16;
                } else if (boxSize == 0) { // extends to end of file
                    boxSize = fileSize - position;
                }

                if (boxSize < boxHeaderSize || position + boxSize > fileSize + 1024) {
                    break;
                }

                if ("moov".equalsIgnoreCase(boxType)) {
                    Integer dur = parseMoovBox(channel, position + boxHeaderSize, Math.min(position + boxSize, fileSize));
                    if (dur != null) {
                        return dur;
                    }
                }

                position += boxSize;
            }
        } catch (Exception e) {
            System.err.println("Failed to parse MP4 duration for " + filePath + ": " + e.getMessage());
        }
        return null;
    }

    private static Integer parseMoovBox(FileChannel channel, long moovStart, long moovEnd) throws IOException {
        long position = moovStart;
        ByteBuffer headerBuffer = ByteBuffer.allocate(8);

        while (position + 8 <= moovEnd) {
            channel.position(position);
            headerBuffer.clear();
            int read = channel.read(headerBuffer);
            if (read < 8) break;
            headerBuffer.flip();

            long boxSize = headerBuffer.getInt() & 0xFFFFFFFFL;
            byte[] typeBytes = new byte[4];
            headerBuffer.get(typeBytes);
            String boxType = new String(typeBytes, StandardCharsets.US_ASCII);

            long boxHeaderSize = 8;
            if (boxSize == 1) {
                ByteBuffer largeSizeBuffer = ByteBuffer.allocate(8);
                if (channel.read(largeSizeBuffer) < 8) break;
                largeSizeBuffer.flip();
                boxSize = largeSizeBuffer.getLong();
                boxHeaderSize = 16;
            } else if (boxSize == 0) {
                boxSize = moovEnd - position;
            }

            if (boxSize < boxHeaderSize) {
                break;
            }

            if ("mvhd".equalsIgnoreCase(boxType)) {
                long payloadSize = boxSize - boxHeaderSize;
                int readSize = (int) Math.min(payloadSize, 64);
                ByteBuffer mvhdBuf = ByteBuffer.allocate(readSize);
                channel.position(position + boxHeaderSize);
                channel.read(mvhdBuf);
                mvhdBuf.flip();

                if (mvhdBuf.remaining() >= 4) {
                    int version = mvhdBuf.get() & 0xFF;
                    // skip 3 flags bytes
                    mvhdBuf.get();
                    mvhdBuf.get();
                    mvhdBuf.get();

                    long timescale = 0;
                    long duration = 0;

                    if (version == 1) {
                        // 8 bytes creation + 8 bytes modification = 16 bytes
                        if (mvhdBuf.remaining() >= 16 + 4 + 8) {
                            mvhdBuf.position(mvhdBuf.position() + 16);
                            timescale = mvhdBuf.getInt() & 0xFFFFFFFFL;
                            duration = mvhdBuf.getLong();
                        }
                    } else {
                        // 4 bytes creation + 4 bytes modification = 8 bytes
                        if (mvhdBuf.remaining() >= 8 + 4 + 4) {
                            mvhdBuf.position(mvhdBuf.position() + 8);
                            timescale = mvhdBuf.getInt() & 0xFFFFFFFFL;
                            duration = mvhdBuf.getInt() & 0xFFFFFFFFL;
                        }
                    }

                    if (timescale > 0 && duration >= 0) {
                        return (int) Math.round((double) duration / (double) timescale);
                    }
                }
            }

            position += boxSize;
        }
        return null;
    }
}

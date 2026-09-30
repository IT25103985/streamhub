package com.streamhub.video;

import com.streamhub.video.util.MediaDurationUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class MediaDurationUtilTest {

    @Test
    public void testExtractMp4Duration_NullOrNonExistent(@TempDir Path tempDir) {
        assertNull(MediaDurationUtil.extractMp4Duration(null));
        assertNull(MediaDurationUtil.extractMp4Duration(tempDir.resolve("non-existent.mp4")));
    }

    @Test
    public void testExtractMp4Duration_SyntheticMp4V0(@TempDir Path tempDir) throws Exception {
        Path dummyMp4 = tempDir.resolve("sample_v0.mp4");
        
        // Build synthetic MP4 with ftyp and moov -> mvhd (version 0)
        // timescale = 1000, duration = 125000 (125 seconds)
        try (FileOutputStream fos = new FileOutputStream(dummyMp4.toFile())) {
            // 1. ftyp box (16 bytes)
            ByteBuffer ftyp = ByteBuffer.allocate(16);
            ftyp.putInt(16); // size
            ftyp.put("ftyp".getBytes(StandardCharsets.US_ASCII));
            ftyp.put("isom".getBytes(StandardCharsets.US_ASCII));
            ftyp.putInt(512);
            fos.write(ftyp.array());

            // 2. mvhd box (v0: 8 header + 4 version/flags + 4 creation + 4 mod + 4 timescale + 4 duration = 28 payload + 8 header = 36 bytes or more)
            int mvhdTotalSize = 8 + 4 + 4 + 4 + 4 + 4 + 16; // 44 bytes
            ByteBuffer mvhd = ByteBuffer.allocate(mvhdTotalSize);
            mvhd.putInt(mvhdTotalSize);
            mvhd.put("mvhd".getBytes(StandardCharsets.US_ASCII));
            mvhd.put((byte) 0); // version 0
            mvhd.put(new byte[]{0, 0, 0}); // flags
            mvhd.putInt(0); // creation_time
            mvhd.putInt(0); // modification_time
            mvhd.putInt(1000); // timescale = 1000
            mvhd.putInt(125000); // duration = 125,000 units => 125 seconds
            mvhd.put(new byte[16]); // other fields

            // 3. moov box wrapping mvhd
            int moovTotalSize = 8 + mvhdTotalSize;
            ByteBuffer moovHeader = ByteBuffer.allocate(8);
            moovHeader.putInt(moovTotalSize);
            moovHeader.put("moov".getBytes(StandardCharsets.US_ASCII));

            fos.write(moovHeader.array());
            fos.write(mvhd.array());
        }

        Integer duration = MediaDurationUtil.extractMp4Duration(dummyMp4);
        assertNotNull(duration);
        assertEquals(125, duration);
    }

    @Test
    public void testExtractMp4Duration_SyntheticMp4V1(@TempDir Path tempDir) throws Exception {
        Path dummyMp4 = tempDir.resolve("sample_v1.mp4");
        
        // Build synthetic MP4 with ftyp and moov -> mvhd (version 1)
        // timescale = 600, duration = 360000 (600 seconds = 10 mins)
        try (FileOutputStream fos = new FileOutputStream(dummyMp4.toFile())) {
            // 1. ftyp box
            ByteBuffer ftyp = ByteBuffer.allocate(16);
            ftyp.putInt(16);
            ftyp.put("ftyp".getBytes(StandardCharsets.US_ASCII));
            ftyp.put("mp42".getBytes(StandardCharsets.US_ASCII));
            ftyp.putInt(0);
            fos.write(ftyp.array());

            // 2. mvhd box (v1: 8 header + 4 version/flags + 8 creation + 8 mod + 4 timescale + 8 duration = 40 payload + 8 header = 48 bytes)
            int mvhdTotalSize = 8 + 4 + 8 + 8 + 4 + 8 + 16;
            ByteBuffer mvhd = ByteBuffer.allocate(mvhdTotalSize);
            mvhd.putInt(mvhdTotalSize);
            mvhd.put("mvhd".getBytes(StandardCharsets.US_ASCII));
            mvhd.put((byte) 1); // version 1
            mvhd.put(new byte[]{0, 0, 0}); // flags
            mvhd.putLong(0L); // creation_time
            mvhd.putLong(0L); // modification_time
            mvhd.putInt(600); // timescale = 600
            mvhd.putLong(360000L); // duration = 360,000 units => 600 seconds
            mvhd.put(new byte[16]);

            // 3. moov box wrapping mvhd
            int moovTotalSize = 8 + mvhdTotalSize;
            ByteBuffer moovHeader = ByteBuffer.allocate(8);
            moovHeader.putInt(moovTotalSize);
            moovHeader.put("moov".getBytes(StandardCharsets.US_ASCII));

            fos.write(moovHeader.array());
            fos.write(mvhd.array());
        }

        Integer duration = MediaDurationUtil.extractMp4Duration(dummyMp4);
        assertNotNull(duration);
        assertEquals(600, duration);
    }
}

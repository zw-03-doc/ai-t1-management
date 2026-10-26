package org.dromara.fitness.service.support;

import org.dromara.common.core.exception.ServiceException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Bounded ISO-BMFF movie-header inspection. No process execution or media download. */
public final class FitnessTrainingVideoRules {
    private FitnessTrainingVideoRules() { }

    public static void validateShortMp4(Path path) {
        try (RandomAccessFile input = new RandomAccessFile(path.toFile(), "r")) {
            long[] movie = findBox(input, 0, input.length(), "moov");
            if (movie == null) throw invalid();
            long[] header = findBox(input, movie[0], movie[1], "mvhd");
            if (header == null || header[1] - header[0] < 20) throw invalid();
            input.seek(header[0]);
            int version = input.readUnsignedByte();
            input.skipBytes(3);
            if (version == 0) input.skipBytes(8);
            else if (version == 1 && header[1] - header[0] >= 32) input.skipBytes(16);
            else throw invalid();
            long timescale = Integer.toUnsignedLong(input.readInt());
            long duration = version == 0 ? Integer.toUnsignedLong(input.readInt()) : input.readLong();
            FitnessTrainingRules.require(timescale > 0 && duration > 0 && duration <= timescale * 60L,
                "视频须为不超过 60 秒的完整 MP4 短片");
        } catch (IOException ex) {
            throw invalid();
        }
    }

    private static long[] findBox(RandomAccessFile input, long start, long end, String wanted) throws IOException {
        int boxes = 0;
        while (start < end) {
            if (++boxes > 4096 || end - start < 8) throw invalid();
            input.seek(start);
            long size = Integer.toUnsignedLong(input.readInt());
            byte[] type = new byte[4];
            input.readFully(type);
            int prefix = 8;
            if (size == 1) { if (end - start < 16) throw invalid(); size = input.readLong(); prefix = 16; }
            else if (size == 0) size = end - start;
            if (size < prefix || size > end - start) throw invalid();
            if (wanted.equals(new String(type, StandardCharsets.US_ASCII))) return new long[]{start + prefix, start + size};
            start += size;
        }
        return null;
    }

    private static ServiceException invalid() { return new ServiceException("MP4 视频缺少有效时长信息，请重新导出后上传"); }
}

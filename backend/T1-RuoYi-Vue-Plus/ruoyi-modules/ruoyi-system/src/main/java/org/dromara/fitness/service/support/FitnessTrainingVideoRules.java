package org.dromara.fitness.service.support;

import org.dromara.common.core.exception.ServiceException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Bounded ISO-BMFF movie-header inspection. No process execution or media download. */
public final class FitnessTrainingVideoRules {
    private FitnessTrainingVideoRules() { }

    public static void validateMp4(Path path) {
        try (RandomAccessFile input = new RandomAccessFile(path.toFile(), "r")) {
            long[] movie = findBox(input, 0, input.length(), "moov");
            if (movie == null) throw invalid();
            long[] header = findBox(input, movie[0], movie[1], "mvhd");
            if (header == null || header[1] - header[0] < 20) throw invalid();
            input.seek(header[0]);
            int version = input.readUnsignedByte();
            if (version != 0 && (version != 1 || header[1] - header[0] < 32)) throw invalid();
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

    private static ServiceException invalid() { return new ServiceException("MP4 视频文件格式无效，请重新导出后上传"); }
}

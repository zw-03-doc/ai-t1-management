package org.dromara.fitness.service.support;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.dromara.common.core.exception.ServiceException;

final class FitnessImageDecoder {
    private FitnessImageDecoder() { }

    static BufferedImage decode(InputStream bytes, int maxSide) throws IOException {
        try (var stream = new MemoryCacheImageInputStream(bytes)) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new ServiceException("请选择有效的 JPEG 或 PNG 图片");
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!"JPEG".equalsIgnoreCase(format) && !"PNG".equalsIgnoreCase(format)) {
                    throw new ServiceException("仅支持 JPEG 和 PNG 图片");
                }
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 16384 || height > 16384 || (long) width * height > 32_000_000L) {
                    throw new ServiceException("图片尺寸过大，请压缩后重试");
                }
                int stride = (Math.max(width, height) + maxSide - 1) / maxSide;
                var options = reader.getDefaultReadParam();
                options.setSourceSubsampling(stride, stride, 0, 0);
                BufferedImage image = reader.read(0, options);
                BufferedImage clean = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = clean.createGraphics();
                try {
                    graphics.drawImage(image, 0, 0, Color.WHITE, null);
                } finally {
                    graphics.dispose();
                }
                return clean;
            } finally {
                reader.dispose();
            }
        }
    }
}

package org.dromara.fitness.service.support;

import org.dromara.common.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import cn.hutool.crypto.digest.DigestUtil;

@Service
public class FitnessCatalogImages {
    public static final String PREFIX = "/fitness/catalog/images/";
    private final Path root;

    public FitnessCatalogImages(@Value("${fitness.media.path:./data/training-media}") String mediaPath) {
        this.root = Path.of(mediaPath).toAbsolutePath().normalize().resolve("catalog");
    }

    public String save(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5L * 1024 * 1024) throw new ServiceException("请选择 5 MB 以内的 JPEG 或 PNG 图片");
        try (var input = file.getInputStream(); var output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(FitnessImageDecoder.decode(input, 3072), "JPEG", output)) throw new IOException();
            byte[] bytes = output.toByteArray();
            String name = DigestUtil.sha256Hex(bytes) + ".jpg";
            Files.createDirectories(root);
            Path target = root.resolve(name);
            if (!Files.exists(target)) {
                Path temp = Files.createTempFile(root, ".upload-", ".tmp");
                try {
                    Files.write(temp, bytes);
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                } finally { Files.deleteIfExists(temp); }
            }
            return PREFIX + name;
        } catch (IOException ex) {
            throw new ServiceException("图片无法保存，请重试");
        }
    }

    public Resource read(String name) {
        if (!name.matches("[0-9a-f]{64}\\.jpg") || !Files.isRegularFile(root.resolve(name))) return null;
        return new FileSystemResource(root.resolve(name));
    }
}

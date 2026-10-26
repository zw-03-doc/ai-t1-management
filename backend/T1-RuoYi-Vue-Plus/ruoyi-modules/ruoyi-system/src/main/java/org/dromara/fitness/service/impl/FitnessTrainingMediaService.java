package org.dromara.fitness.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.Media;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.Plan;
import org.dromara.fitness.mapper.FitnessTrainingMapper;
import org.dromara.fitness.service.support.FitnessTrainingVideoRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static org.dromara.fitness.service.support.FitnessTrainingRules.require;

@Service
@RequiredArgsConstructor
public class FitnessTrainingMediaService {
    private final FitnessTrainingService trainingService;
    private final FitnessTrainingMapper mapper;

    @Value("${fitness.media.path:./data/training-media}")
    private String mediaPath;

    @Transactional(rollbackFor = Exception.class)
    public Media upload(Long planId, MultipartFile file) {
        Plan plan = trainingService.mediaWriteAccess(planId);
        require(file != null && !file.isEmpty() && file.getSize() <= 30L * 1024 * 1024, "附件不能为空且不得超过 30MB");
        Path target = null;
        try (InputStream stream = file.getInputStream()) {
            String type = detectType(stream.readNBytes(16));
            require(type != null, "仅支持 JPEG、PNG、WebP 图片和 MP4 视频");
            require(type.equals("video/mp4") || file.getSize() <= 10L * 1024 * 1024, "图片不得超过 10MB");
            String suffix = switch (type) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".mp4";
            };
            Media media = new Media();
            media.setAssetId(UUID.randomUUID().toString());
            media.setTenantId(plan.getTenantId());
            media.setPlanId(planId);
            media.setMediaType(type);
            media.setSize(file.getSize());
            media.setFileName(media.getAssetId() + suffix);
            media.setUrl("/fitness/training/media/" + media.getAssetId());
            Path root = Path.of(mediaPath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            target = root.resolve(media.getFileName()).normalize();
            require(target.startsWith(root), "附件路径无效");
            try (InputStream content = file.getInputStream()) {
                Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (type.equals("video/mp4")) FitnessTrainingVideoRules.validateShortMp4(target);
            mapper.insertMedia(media);
            trainingService.recordMediaUpload(plan, media);
            return media;
        } catch (IOException ex) {
            cleanup(target);
            throw new ServiceException("附件保存失败，请稍后重试");
        } catch (RuntimeException ex) {
            cleanup(target);
            throw ex;
        }
    }

    public ResponseEntity<Resource> read(String assetId) {
        require(assetId.matches("[0-9a-f-]{36}"), "附件编号无效");
        Media media = trainingService.mediaReadAccess(assetId);
        Path root = Path.of(mediaPath).toAbsolutePath().normalize();
        Path file = root.resolve(media.getFileName()).normalize();
        require(file.startsWith(root) && Files.isRegularFile(file), "附件文件不存在");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(media.getMediaType()))
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.getFileName() + "\"")
            .header("X-Content-Type-Options", "nosniff")
            .body(new FileSystemResource(file));
    }

    public static String detectType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71
            && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return "image/png";
        if (bytes.length >= 12 && ascii(bytes, 0, 4).equals("RIFF") && ascii(bytes, 8, 4).equals("WEBP")) return "image/webp";
        if (bytes.length >= 12 && ascii(bytes, 4, 4).equals("ftyp")
            && java.util.Set.of("isom", "iso2", "mp41", "mp42", "avc1", "M4V ").contains(ascii(bytes, 8, 4))) return "video/mp4";
        return null;
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    private static void cleanup(Path file) {
        if (file == null) return;
        try { Files.deleteIfExists(file); } catch (IOException ignored) { /* An unreferenced file remains inaccessible. */ }
    }
}

package org.dromara.fitness.service.support;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.model.FitnessLoginUser.CoachLoginUser;
import org.dromara.fitness.domain.model.FitnessLoginUser.MemberLoginUser;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.mapper.FitnessTrainingMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FitnessMemberAvatarService {
    private static final String PREFIX = "/fitness/member/avatars/";
    private static final Pattern FILE_NAME = Pattern.compile("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.jpg");
    private static final int MAX_SIDE = 512;
    private final FitnessMemberMapper members;
    private final FitnessTrainingMapper relationships;
    private final Path root;

    public FitnessMemberAvatarService(FitnessMemberMapper members, FitnessTrainingMapper relationships,
            @Value("${fitness.media.path:./data/training-media}") String mediaPath) {
        this.members = members;
        this.relationships = relationships;
        this.root = Path.of(mediaPath).toAbsolutePath().normalize().resolve("avatars");
    }

    public String upload(MemberLoginUser login, MultipartFile file) {
        Long id = login.getMemberId();
        FitMember member = member(id, login.getTenantId());
        if (!"normal".equals(member.getMemberStatus())) throw new ServiceException("会员状态不可用");
        if (file == null || file.isEmpty() || file.getSize() > 5L * 1024 * 1024) {
            throw new ServiceException("头像请选择不超过 5MB 的 JPEG 或 PNG 图片");
        }
        Path dir = root.resolve(id.toString());
        String name = UUID.randomUUID() + ".jpg";
        Path target = dir.resolve(name);
        try (InputStream bytes = file.getInputStream()) {
            BufferedImage clean = FitnessImageDecoder.decode(bytes, MAX_SIDE);
            Files.createDirectories(dir);
            try (var output = Files.newOutputStream(target)) {
                if (!ImageIO.write(clean, "JPEG", output)) throw new IOException("JPEG writer unavailable");
            }
            // Member mutations are serialized by FitnessAccountWebConfig. Keep the saved and newest draft only.
            try (var files = Files.newDirectoryStream(dir, "*.jpg")) {
                for (Path path : files) {
                    String oldName = path.getFileName().toString();
                    if (FILE_NAME.matcher(oldName).matches() && !name.equals(oldName)
                        && !url(id, oldName).equals(member.getAvatarUrl())) Files.deleteIfExists(path);
                }
            }
            return url(id, name);
        } catch (IOException exception) {
            remove(target);
            throw new ServiceException("头像文件无法读取或保存，请更换图片后重试");
        }
    }

    public void validateSelection(Long memberId, String currentUrl, String submittedUrl) {
        if (submittedUrl == null || submittedUrl.isEmpty() || Objects.equals(currentUrl, submittedUrl)) return;
        Path file = ownedPath(memberId, submittedUrl);
        if (file == null || !Files.isRegularFile(file)) throw new ServiceException("头像已失效，请重新选择并上传");
    }

    /** Called while the verified identity claim holds both member rows, only if the destination lacks an avatar. */
    public String copyForClaim(Long sourceId, String sourceUrl, Long targetId) {
        if (sourceUrl == null || !sourceUrl.startsWith(PREFIX)) return sourceUrl;
        Path source = ownedPath(sourceId, sourceUrl);
        if (source == null || !Files.isRegularFile(source)) return null;
        String name = UUID.randomUUID() + ".jpg";
        Path target = root.resolve(targetId.toString()).resolve(name);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target);
        } catch (IOException ex) {
            remove(target);
            throw new ServiceException("头像迁移失败，请稍后重试");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) remove(source);
                    if (status == STATUS_ROLLED_BACK) remove(target);
                }
            });
        }
        return url(targetId, name);
    }

    public Resource read(Long id, String name) {
        if (!FILE_NAME.matcher(name).matches()) throw missing();
        LoginUser login = LoginHelper.getLoginUser();
        if (login == null || login.getTenantId() == null) throw missing();
        FitMember owner = member(id, login.getTenantId());
        if (!url(id, name).equals(owner.getAvatarUrl())) throw missing();
        if (login instanceof MemberLoginUser self) {
            if (!id.equals(self.getMemberId())) throw missing();
        } else if (login instanceof CoachLoginUser coach) {
            if (relationships.countCoachMember(login.getTenantId(), coach.getCoachId(), id) == 0) throw missing();
        } else if (!StpUtil.hasPermission("fitness:member:query")) {
            throw missing();
        }
        Path file = root.resolve(id.toString()).resolve(name);
        if (!Files.isRegularFile(file)) throw missing();
        return new FileSystemResource(file);
    }

    private FitMember member(Long id, String tenantId) {
        FitMember member = members.selectById(id);
        if (member == null || tenantId == null || !tenantId.equals(member.getTenantId())) throw missing();
        return member;
    }

    private Path ownedPath(Long id, String requested) {
        String prefix = url(id, "");
        if (!requested.startsWith(prefix)) return null;
        String name = requested.substring(prefix.length());
        return FILE_NAME.matcher(name).matches() ? root.resolve(id.toString()).resolve(name) : null;
    }

    private static String url(Long id, String name) {
        return PREFIX + id + "/" + name;
    }

    private static ServiceException missing() {
        return new ServiceException("头像不存在或无权查看", 404);
    }

    private static void remove(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("无法清理会员头像文件 {}", path.getFileName(), ex);
        }
    }
}

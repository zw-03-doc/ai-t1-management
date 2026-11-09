package org.dromara.fitness.service.support;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.mapper.FitnessVenueMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FitnessVenueService {
    private static final String BRAND = TenantConstants.DEFAULT_TENANT_ID;
    public static final String IMAGE_PATH = "/fitness/mini/portal/venue/images/";
    private final FitnessVenueMapper mapper;

    public record Photo(String id, String url) {}
    public record Gallery(int version, List<Photo> photos) {}

    @Transactional(readOnly = true)
    public Gallery gallery() {
        int version = mapper.version(BRAND);
        return new Gallery(version, mapper.ids(BRAND).stream().map(id -> new Photo(id,
            id.matches("live-[123]") ? "/app/static/images/" + id + ".jpg" : IMAGE_PATH + id)).toList());
    }

    public byte[] image(String id) {
        var stored = mapper.image(BRAND, id);
        return stored == null ? null : stored.jpeg();
    }

    @Transactional(rollbackFor = Exception.class)
    public Gallery upload(int version, String replaceId, MultipartFile file) {
        checkAdminTenant();
        if (file == null || file.isEmpty() || file.getSize() > 10L * 1024 * 1024) {
            throw new ServiceException("请选择 10 MB 以内的 JPEG 或 PNG 图片");
        }
        byte[] jpeg;
        try (var input = file.getInputStream(); var out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(FitnessImageDecoder.decode(input, 2048), "JPEG", out)) throw new IOException();
            jpeg = out.toByteArray();
        } catch (IOException ex) {
            throw new ServiceException("图片无法读取，请更换文件");
        }
        claim(version);
        String id = UUID.randomUUID().toString();
        if (replaceId == null) mapper.insert(BRAND, id, mapper.nextPosition(BRAND), jpeg);
        else if (mapper.replace(BRAND, replaceId, id, jpeg) != 1) throw new ServiceException("图片不存在");
        return gallery();
    }

    @Transactional(rollbackFor = Exception.class)
    public Gallery order(int version, List<String> requested) {
        checkAdminTenant();
        claim(version);
        List<String> current = mapper.ids(BRAND);
        if (requested == null || current.size() != requested.size() || !new HashSet<>(current).equals(new HashSet<>(requested))) {
            throw new ServiceException("图片列表已变更，请刷新");
        }
        for (int i = 0; i < requested.size(); i++) mapper.move(BRAND, requested.get(i), i + 1);
        return gallery();
    }

    @Transactional(rollbackFor = Exception.class)
    public Gallery delete(int version, String id) {
        checkAdminTenant();
        claim(version);
        if (mapper.remove(BRAND, id) != 1) throw new ServiceException("图片不存在");
        return gallery();
    }

    private void claim(int version) {
        if (mapper.claim(BRAND, version) != 1) throw new ServiceException("场馆图片已变更，请刷新后重试", 409);
    }

    // This album is published on the T1 brand homepage.
    private static void checkAdminTenant() {
        if (TenantHelper.isEnable() && !BRAND.equals(TenantHelper.getTenantId())) {
            throw new ServiceException("当前账号无权管理品牌首页", 403);
        }
    }
}

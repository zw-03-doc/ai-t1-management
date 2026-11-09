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
    public record Gallery(String storeId, String storeName, int version, List<Photo> photos) {}

    public List<FitnessVenueMapper.Store> stores() {
        checkAdminTenant();
        return mapper.stores(BRAND);
    }

    private Long resolveStore(Long requested) {
        return requested == null ? mapper.defaultStore(BRAND) : requested;
    }

    @Transactional(readOnly = true)
    public Gallery gallery(Long requested, boolean admin) {
        if (admin) checkAdminTenant();
        Long id = resolveStore(requested);
        var store = id == null ? null : mapper.store(BRAND, id);
        if (store == null || (!admin && !"0".equals(store.status()))) {
            if (requested == null) return new Gallery(null, null, 0, List.of());
            throw new ServiceException("门店不存在或未开放", 404);
        }
        return album(store, requested != null);
    }

    private Gallery album(FitnessVenueMapper.Store store, boolean scoped) {
        long id = store.storeId();
        Integer version = mapper.version(BRAND, id);
        String suffix = scoped ? "?storeId=" + id : "";
        return new Gallery(String.valueOf(id), store.storeName(), version == null ? 0 : version,
            mapper.ids(BRAND, id).stream().map(photo -> new Photo(photo, photo.matches("live-[123]")
                ? "/app/static/images/" + photo + ".jpg" : IMAGE_PATH + photo + suffix)).toList());
    }

    @Transactional(readOnly = true)
    public byte[] image(Long requested, String photoId, boolean admin) {
        if (admin) checkAdminTenant();
        Long id = resolveStore(requested);
        var store = id == null ? null : mapper.store(BRAND, id);
        if (store == null || (!admin && !"0".equals(store.status()))) return null;
        var image = mapper.image(BRAND, id, photoId);
        return image == null ? null : image.jpeg();
    }

    @Transactional(rollbackFor = Exception.class)
    public Gallery upload(Long requested, int version, String replaceId, MultipartFile file) {
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
        var store = claim(requested, version);
        long storeId = store.storeId();
        String id = UUID.randomUUID().toString();
        if (replaceId == null) mapper.insert(BRAND, storeId, id, mapper.nextPosition(BRAND, storeId), jpeg);
        else if (mapper.replace(BRAND, storeId, replaceId, id, jpeg) != 1) throw new ServiceException("图片不存在");
        return album(store, requested != null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Gallery order(Long requestedStore, int version, List<String> requested) {
        checkAdminTenant();
        var store = claim(requestedStore, version);
        List<String> current = mapper.ids(BRAND, store.storeId());
        if (requested == null || current.size() != requested.size() || !new HashSet<>(current).equals(new HashSet<>(requested))) {
            throw new ServiceException("图片列表已变更，请刷新");
        }
        for (int i = 0; i < requested.size(); i++) mapper.move(BRAND, store.storeId(), requested.get(i), i + 1);
        return album(store, requestedStore != null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Gallery delete(Long requested, int version, String id) {
        checkAdminTenant();
        var store = claim(requested, version);
        if (mapper.remove(BRAND, store.storeId(), id) != 1) throw new ServiceException("图片不存在");
        return album(store, requested != null);
    }

    private FitnessVenueMapper.Store claim(Long requested, int version) {
        Long id = resolveStore(requested);
        var store = id == null ? null : mapper.lockStore(BRAND, id);
        if (store == null) throw new ServiceException("门店不存在", 404);
        mapper.ensureGallery(BRAND, id);
        if (mapper.claim(BRAND, id, version) != 1) throw new ServiceException("场馆图片已变更，请刷新后重试", 409);
        return store;
    }

    private static void checkAdminTenant() {
        if (TenantHelper.isEnable() && !BRAND.equals(TenantHelper.getTenantId())) {
            throw new ServiceException("当前账号无权管理品牌首页", 403);
        }
    }
}

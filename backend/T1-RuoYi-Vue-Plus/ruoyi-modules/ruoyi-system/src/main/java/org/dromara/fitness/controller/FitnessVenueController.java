package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.fitness.service.support.FitnessVenueService;
import org.dromara.fitness.mapper.FitnessVenueMapper.Store;
import org.dromara.fitness.service.support.FitnessVenueService.Gallery;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fitness")
public class FitnessVenueController {
    private final FitnessVenueService service;
    public record Order(int version, List<String> ids) {}

    @SaIgnore
    @GetMapping("/mini/portal/venue")
    public R<Gallery> gallery(@RequestParam(required = false) Long storeId, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return R.ok(service.gallery(storeId, false));
    }

    @SaIgnore
    @GetMapping("/mini/portal/venue/images/{id:[a-f0-9-]+}")
    public ResponseEntity<byte[]> image(@PathVariable String id, @RequestParam(required = false) Long storeId, HttpServletResponse response) {
        return jpeg(service.image(storeId, id, false), response, CacheControl.noCache());
    }

    @SaCheckPermission("fitness:venue:query")
    @GetMapping("/portal/venue/images/{id:[a-f0-9-]+}")
    public ResponseEntity<byte[]> adminImage(@PathVariable String id, @RequestParam Long storeId, HttpServletResponse response) {
        return jpeg(service.image(storeId, id, true), response, CacheControl.noCache().cachePrivate());
    }

    private ResponseEntity<byte[]> jpeg(byte[] bytes, HttpServletResponse response, CacheControl cache) {
        if (bytes == null) return ResponseEntity.notFound().build();
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).header("X-Content-Type-Options", "nosniff")
            .cacheControl(cache).body(bytes);
    }

    @SaCheckPermission("fitness:venue:query")
    @GetMapping("/portal/venue")
    public R<Gallery> admin(@RequestParam(required = false) Long storeId, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return R.ok(service.gallery(storeId, true));
    }

    @SaCheckPermission("fitness:venue:query")
    @GetMapping("/portal/venue/stores")
    public R<List<Store>> stores() { return R.ok(service.stores()); }

    @Log(title = "上传场馆图片", businessType = BusinessType.UPDATE)
    @SaCheckPermission("fitness:venue:edit")
    @PostMapping(value = "/portal/venue/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Gallery> upload(@RequestParam(required = false) Long storeId, @RequestParam int version, @RequestParam(required = false) String replaceId, @RequestPart MultipartFile file) {
        return R.ok(service.upload(storeId, version, replaceId, file));
    }

    @Log(title = "场馆图片排序", businessType = BusinessType.UPDATE)
    @SaCheckPermission("fitness:venue:edit")
    @PutMapping("/portal/venue/order")
    public R<Gallery> order(@RequestParam(required = false) Long storeId, @RequestBody Order order) { return R.ok(service.order(storeId, order.version(), order.ids())); }

    @Log(title = "删除场馆图片", businessType = BusinessType.DELETE)
    @SaCheckPermission("fitness:venue:edit")
    @DeleteMapping("/portal/venue/images/{id}")
    public R<Gallery> remove(@PathVariable String id, @RequestParam(required = false) Long storeId, @RequestParam int version) { return R.ok(service.delete(storeId, version, id)); }
}

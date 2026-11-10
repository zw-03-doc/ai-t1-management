package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.fitness.service.support.FitnessCatalogImages;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fitness/catalog")
public class FitnessCatalogImageController {
    private final FitnessCatalogImages images;
    public record ImageUrl(String url) {}

    @PostMapping(value = "/{scope:activity|coach|course}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Log(title = "上传展示图片", businessType = BusinessType.INSERT)
    public R<ImageUrl> upload(@PathVariable String scope, @RequestPart MultipartFile file) {
        StpUtil.checkPermissionOr("fitness:" + scope + ":add", "fitness:" + scope + ":edit");
        return R.ok(new ImageUrl(images.save(file)));
    }

    @SaIgnore
    @GetMapping("/images/{name}")
    public ResponseEntity<Resource> image(@PathVariable String name, HttpServletResponse response) {
        Resource image = images.read(name);
        if (image == null) return ResponseEntity.notFound().build();
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
            .header("X-Content-Type-Options", "nosniff").body(image);
    }
}

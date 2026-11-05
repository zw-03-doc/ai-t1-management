package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.fitness.service.support.FitnessMemberAvatarService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@SaCheckLogin
@RestController
@RequiredArgsConstructor
public class FitnessMemberAvatarController {
    private final FitnessMemberAvatarService avatars;

    public record UploadedAvatar(String avatarUrl) {}

    @PostMapping("/fitness/mini/mine/avatar")
    public R<UploadedAvatar> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(new UploadedAvatar(avatars.upload(FitnessSecurityUtils.getMemberLoginUser(), file)));
    }

    @GetMapping("/fitness/member/avatars/{memberId}/{fileName}")
    public ResponseEntity<Resource> image(@PathVariable Long memberId, @PathVariable String fileName, HttpServletResponse response) {
        Resource image = avatars.read(memberId, fileName);
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff").body(image);
    }
}

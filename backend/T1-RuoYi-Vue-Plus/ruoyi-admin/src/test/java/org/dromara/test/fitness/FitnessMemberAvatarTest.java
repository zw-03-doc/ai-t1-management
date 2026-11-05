package org.dromara.test.fitness;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.model.FitnessLoginUser.CoachLoginUser;
import org.dromara.fitness.domain.model.FitnessLoginUser.MemberLoginUser;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.mapper.FitnessTrainingMapper;
import org.dromara.fitness.service.support.FitnessMemberAvatarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessMemberAvatarTest {
    @TempDir Path directory;
    private FitnessMemberAvatarService service;
    private FitnessTrainingMapper relationships;
    private final MemberLoginUser user = new MemberLoginUser();
    private final FitMember profile = new FitMember();

    @BeforeEach void setUp() {
        var members = mock(FitnessMemberMapper.class);
        relationships = mock(FitnessTrainingMapper.class);
        service = new FitnessMemberAvatarService(members, relationships, directory.toString());
        user.setTenantId("gym");
        user.setMemberId(41L);
        profile.setMemberId(41L);
        profile.setTenantId("gym");
        profile.setMemberStatus("normal");
        when(members.selectById(41L)).thenReturn(profile);
    }

    private MockMultipartFile photo() throws IOException {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1024, 512, BufferedImage.TYPE_INT_RGB), "PNG", bytes);
        return new MockMultipartFile("file", "../../profile.exe", "text/plain", bytes.toByteArray());
    }

    private Path disk(String url) {
        return directory.resolve("avatars").resolve(url.substring("/fitness/member/avatars/".length()));
    }

    @Test void decodesRealPhotoAndKeepsOnlyNormalizedThumbnail() throws Exception {
        String url = service.upload(user, photo());
        assertTrue(url.startsWith("/fitness/member/avatars/41/"));
        var image = ImageIO.read(disk(url).toFile());
        assertEquals(512, image.getWidth());
        assertEquals(256, image.getHeight());
        assertArrayEquals(new byte[]{(byte)0xff, (byte)0xd8}, Arrays.copyOf(Files.readAllBytes(disk(url)), 2));
        assertNull(profile.getAvatarUrl(), "Choosing a photo must not change the saved profile");
    }

    @Test void rejectsFakeAndOversizeFilesBeforePublishing() {
        var html = new MockMultipartFile("file", "photo.png", "image/png", "<script/>".getBytes());
        assertThrows(ServiceException.class, () -> service.upload(user, html));
        var oversized = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);
        assertThrows(ServiceException.class, () -> service.upload(user, oversized));
        assertFalse(Files.exists(directory.resolve("avatars")));
    }

    @Test void stagedPhotoIsLimitedToItsOwnerAndNewestDraftSurvives() throws Exception {
        String saved = service.upload(user, photo());
        profile.setAvatarUrl(saved);
        String abandoned = service.upload(user, photo());
        String draft = service.upload(user, photo());
        assertTrue(Files.exists(disk(saved)));
        assertFalse(Files.exists(disk(abandoned)));
        service.validateSelection(41L, saved, draft);
        assertThrows(ServiceException.class, () -> service.validateSelection(42L, null, draft));
        assertThrows(ServiceException.class, () -> service.validateSelection(41L, saved, abandoned));
        assertThrows(ServiceException.class, () -> service.validateSelection(41L, saved, "https://example.test/new.jpg"));
        assertThrows(ServiceException.class, () -> service.validateSelection(41L, saved, "/fitness/member/avatars/41/../42/abc.jpg"));
        service.validateSelection(41L, "https://example.test/legacy.jpg", "https://example.test/legacy.jpg");
    }

    @Test void onlyCurrentPhotoCanBeReadAndPermissionIsCheckedForEachReader() throws Exception {
        String url = service.upload(user, photo());
        String name = disk(url).getFileName().toString();
        try (var login = mockStatic(LoginHelper.class); var token = mockStatic(StpUtil.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(user);
            assertThrows(ServiceException.class, () -> service.read(41L, name));
            profile.setAvatarUrl(url);
            assertTrue(service.read(41L, name).exists());
            user.setMemberId(42L);
            assertThrows(ServiceException.class, () -> service.read(41L, name));
            var coach = new CoachLoginUser();
            coach.setCoachId(50L);
            coach.setTenantId("gym");
            login.when(LoginHelper::getLoginUser).thenReturn(coach);
            assertThrows(ServiceException.class, () -> service.read(41L, name));
            when(relationships.countCoachMember("gym", 50L, 41L)).thenReturn(1);
            assertTrue(service.read(41L, name).exists());
            var admin = new LoginUser();
            admin.setTenantId("gym");
            login.when(LoginHelper::getLoginUser).thenReturn(admin);
            assertThrows(ServiceException.class, () -> service.read(41L, name));
            token.when(() -> StpUtil.hasPermission("fitness:member:query")).thenReturn(true);
            assertTrue(service.read(41L, name).exists());
            admin.setTenantId("other");
            assertThrows(ServiceException.class, () -> service.read(41L, name));
        }
    }

    @Test void committedClaimKeepsOnlyReownedFile() throws Exception {
        String source = service.upload(user, photo());
        String target = claim(source, TransactionSynchronization.STATUS_COMMITTED);
        assertTrue(target.startsWith("/fitness/member/avatars/83/"));
        assertTrue(Files.exists(disk(target)));
        assertFalse(Files.exists(disk(source)));
        service.validateSelection(83L, null, target);
    }

    @Test void rolledBackClaimKeepsSourceAndRemovesUnpublishedCopy() throws Exception {
        String source = service.upload(user, photo());
        String target = claim(source, TransactionSynchronization.STATUS_ROLLED_BACK);
        assertTrue(Files.exists(disk(source)));
        assertFalse(Files.exists(disk(target)));
    }

    private String claim(String source, int transactionStatus) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            String result = service.copyForClaim(41L, source, 83L);
            TransactionSynchronizationManager.getSynchronizations().forEach(hook -> hook.afterCompletion(transactionStatus));
            return result;
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}

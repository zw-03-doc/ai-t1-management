package org.dromara.test;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.*;
import org.dromara.fitness.mapper.FitnessTrainingMapper;
import org.dromara.fitness.service.impl.FitnessTrainingMediaService;
import org.dromara.fitness.service.impl.FitnessTrainingService;
import org.dromara.fitness.service.support.FitnessTrainingVideoRules;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("training")
class FitnessTrainingMediaTest {
    @TempDir Path directory;

    @Test
    void uploadChecksPlanPermissionThenWritesMetadataAndAudit() throws Exception {
        FitnessTrainingService training = mock(FitnessTrainingService.class);
        FitnessTrainingMapper mapper = mock(FitnessTrainingMapper.class);
        FitnessTrainingMediaService service = new FitnessTrainingMediaService(training, mapper);
        ReflectionTestUtils.setField(service, "mediaPath", directory.toString());
        Plan plan = new Plan();
        plan.setPlanId(1L);
        plan.setTenantId("tenant-a");
        when(training.mediaWriteAccess(1L)).thenReturn(plan);
        MockMultipartFile image = new MockMultipartFile("file", "../../image.jpg", "text/html",
            new byte[]{(byte) 255, (byte) 216, (byte) 255, 0});
        Media saved = service.upload(1L, image);
        assertEquals("image/jpeg", saved.getMediaType());
        assertTrue(saved.getUrl().startsWith("/fitness/training/media/"));
        assertTrue(Files.exists(directory.resolve(saved.getFileName())));
        verify(mapper).insertMedia(saved);
        verify(training).recordMediaUpload(plan, saved);
        assertFalse(saved.getFileName().contains(".."));
    }

    @Test
    void deniedUploadNeverWritesAFileAndDeniedReadNeverReturnsAResource() throws Exception {
        FitnessTrainingService training = mock(FitnessTrainingService.class);
        FitnessTrainingMapper mapper = mock(FitnessTrainingMapper.class);
        FitnessTrainingMediaService service = new FitnessTrainingMediaService(training, mapper);
        ReflectionTestUtils.setField(service, "mediaPath", directory.toString());
        when(training.mediaWriteAccess(1L)).thenThrow(new ServiceException("无权访问"));
        assertThrows(ServiceException.class, () -> service.upload(1L,
            new MockMultipartFile("file", new byte[]{1, 2, 3})));
        verifyNoInteractions(mapper);
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
        String id = "00000000-0000-0000-0000-000000000001";
        when(training.mediaReadAccess(id)).thenThrow(new ServiceException("无权访问"));
        assertThrows(ServiceException.class, () -> service.read(id));
    }

    @Test
    void mp4DurationRejectsLongOrMalformedMovies() throws Exception {
        Path shortMovie = Files.write(directory.resolve("short.mp4"), movie(60));
        assertDoesNotThrow(() -> FitnessTrainingVideoRules.validateShortMp4(shortMovie));
        Path longMovie = Files.write(directory.resolve("long.mp4"), movie(61));
        assertThrows(ServiceException.class, () -> FitnessTrainingVideoRules.validateShortMp4(longMovie));
        Path invalid = Files.write(directory.resolve("invalid.mp4"), new byte[]{0, 0, 0, 1, 'm', 'o', 'o', 'v'});
        assertThrows(ServiceException.class, () -> FitnessTrainingVideoRules.validateShortMp4(invalid));
        Path noDuration = Files.write(directory.resolve("empty.mp4"), movie(0));
        assertThrows(ServiceException.class, () -> FitnessTrainingVideoRules.validateShortMp4(noDuration));
    }

    @Test
    void oversizedDurationRollsBackFileBeforeMetadataWrite() throws Exception {
        FitnessTrainingService training = mock(FitnessTrainingService.class);
        FitnessTrainingMapper mapper = mock(FitnessTrainingMapper.class);
        FitnessTrainingMediaService service = new FitnessTrainingMediaService(training, mapper);
        ReflectionTestUtils.setField(service, "mediaPath", directory.toString());
        Plan plan = new Plan();
        plan.setPlanId(1L);
        plan.setTenantId("tenant-a");
        when(training.mediaWriteAccess(1L)).thenReturn(plan);
        MockMultipartFile video = new MockMultipartFile("file", "clip.mp4", "video/mp4", movie(61));
        assertThrows(ServiceException.class, () -> service.upload(1L, video));
        verify(mapper, never()).insertMedia(any());
        verify(training, never()).recordMediaUpload(any(), any());
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }

    private byte[] movie(int seconds) {
        ByteBuffer bytes = ByteBuffer.allocate(52);
        bytes.putInt(16).put(new byte[]{'f','t','y','p','i','s','o','m'}).putInt(0);
        bytes.putInt(36).put(new byte[]{'m','o','o','v'});
        bytes.putInt(28).put(new byte[]{'m','v','h','d'});
        bytes.putInt(0).putInt(0).putInt(0).putInt(1000).putInt(seconds * 1000);
        return bytes.array();
    }
}

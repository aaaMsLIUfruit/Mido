package com.mido.backend.storage;

import com.mido.backend.config.FileStorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp"
    );

    private final FileStorageProperties properties;
    private Path rootLocation;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws IOException {
        this.rootLocation = Paths.get(properties.getRootDir()).toAbsolutePath().normalize();
        Files.createDirectories(rootLocation);
    }

    public String storeImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        validateContentType(file);

        LocalDateTime now = LocalDateTime.now();
        String relativeDir = String.format("%04d/%02d", now.getYear(), now.getMonthValue());
        Path targetDir = rootLocation.resolve(relativeDir);
        try {
            Files.createDirectories(targetDir);
            String extension = resolveExtension(file);
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (StringUtils.hasText(extension)) {
                filename += "." + extension;
            }
            Path targetFile = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetFile);
            String urlPath = relativeDir.replace("\\", "/") + "/" + filename;
            String baseUrl = properties.getBaseUrl().endsWith("/")
                    ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1)
                    : properties.getBaseUrl();
            return baseUrl + "/" + urlPath;
        } catch (IOException ex) {
            throw new RuntimeException("保存文件失败", ex);
        }
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("仅支持上传图片类型文件");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        String ext = StringUtils.getFilenameExtension(filename);
        return ext != null ? ext.toLowerCase(Locale.ROOT) : null;
    }
}


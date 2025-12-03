package com.mido.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.upload")
public class FileStorageProperties {

    /**
     * 服务器本地存储根目录，例如 uploads 或 D:/data/uploads
     */
    @NotBlank(message = "上传根目录不能为空")
    private String rootDir = "uploads";

    /**
     * 对外暴露的访问前缀，例如 http://localhost:8080/uploads
     */
    @NotBlank(message = "上传访问地址前缀不能为空")
    private String baseUrl = "http://localhost:8080/uploads";

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}


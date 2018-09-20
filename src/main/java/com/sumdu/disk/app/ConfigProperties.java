package com.sumdu.disk.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config properties for the bot.
 */
@ConfigurationProperties(prefix = "bot")
public class ConfigProperties {

    private String token;
    private String username;
    private String localFilePath;
    private String uploadFolderId;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public String getUploadFolderId() {
        return uploadFolderId;
    }

    public void setUploadFolderId(String uploadFolderId) {
        this.uploadFolderId = uploadFolderId;
    }
}

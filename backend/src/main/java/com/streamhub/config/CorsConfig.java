package com.streamhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
//cors configuration allowing cross origin requests from frontend app
public class CorsConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path thumbnailPath = Paths.get(uploadDir, "thumbnails").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/thumbnails/**")
                .addResourceLocations("file:" + thumbnailPath.toString() + "/");
                
        Path playlistCoverPath = Paths.get(uploadDir, "playlist-covers").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/playlist-covers/**")
                .addResourceLocations("file:" + playlistCoverPath.toString() + "/");
    }
}
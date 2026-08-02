package com.prompttraining.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（V3.2）
 * 将 /uploads/** 映射到本地上传目录，供头像等文件访问
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 上传根目录（相对项目根目录） */
    public static final String UPLOAD_DIR = "uploads";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./" + UPLOAD_DIR + "/");
    }
}

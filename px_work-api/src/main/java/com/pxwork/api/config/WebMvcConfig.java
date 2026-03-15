package com.pxwork.api.config;

import com.pxwork.api.interceptor.FrontInterceptor;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * <p>
 * WebMvc 配置类
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:D:/px/backend/uploads}")
    private String uploadDir;

    @Bean
    public FrontInterceptor frontInterceptor() {
        return new FrontInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    StpUtil.checkLogin();
                }))
                .addPathPatterns(
                        "/backend/**",
                        "/admin-user/**",
                        "/admin-role/**",
                        "/department/**",
                        "/resource/**",
                        "/resource-category/**",
                        "/upload/**",
                        "/course/**")
                .excludePathPatterns(
                        "/backend/login",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error");

        registry.addInterceptor(frontInterceptor())
                .addPathPatterns("/frontend/**")
                .excludePathPatterns(
                        "/frontend/login",
                        "/frontend/register",
                        "/frontend/captchaImage",
                        "/frontend/course/list", // 假设公开课程列表不需要登录
                        "/frontend/course/detail/**" // 假设公开课程详情不需要登录
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = "file:" + uploadDir + File.separator;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}

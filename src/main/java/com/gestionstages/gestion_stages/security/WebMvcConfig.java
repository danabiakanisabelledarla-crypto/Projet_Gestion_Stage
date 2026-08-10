package com.gestionstages.gestion_stages.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TwoFactorInterceptor twoFactorInterceptor;

    public WebMvcConfig(TwoFactorInterceptor twoFactorInterceptor) {
        this.twoFactorInterceptor = twoFactorInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(twoFactorInterceptor)
                .addPathPatterns("/admin/**", "/responsable/**", "/encadreur/**",
                        "/stagiaire/**", "/profil/**", "/documents/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/uploads/**",
                        "/2fa/**", "/logout");
    }
}

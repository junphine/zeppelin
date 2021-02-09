package com.xioaka.easy.flow.sdk;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <b>类 名 称</b> :  CorsConfig<br/>
 * <b>类 描 述</b> :  Cors解决跨域请求问题<br/>
 * <b>修改备注</b> :
 * @author z_dk
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET","HEAD","POST","PUT","DELETE","OPTIONS")
                // 是否允许发送Cookie
                .allowCredentials(true)
                // 本次预检请求的有效期，单位为秒
                .maxAge(3600)
                .allowedHeaders("*");
    }
    
}

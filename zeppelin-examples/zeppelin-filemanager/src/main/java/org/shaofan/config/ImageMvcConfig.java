package org.shaofan.config;

import java.io.File;
import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class ImageMvcConfig extends WebMvcConfigurerAdapter {
 
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	File images = new File("./images");  
    	if(!images.exists()) {
    		images.mkdir();
    	}
        try {
			registry.addResourceHandler("/images/**")
			        .addResourceLocations("file:///"+images.getCanonicalPath()+"/");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}

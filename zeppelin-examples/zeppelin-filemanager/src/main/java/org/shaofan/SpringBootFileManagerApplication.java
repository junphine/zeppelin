package org.shaofan;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;


/**
 * @author shaofan
 */
@SpringBootApplication
public class SpringBootFileManagerApplication extends SpringBootServletInitializer {

	//SecurityWebInitializer wi;
	
    public static void main(String[] args) {
        SpringApplication.run(SpringBootFileManagerApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application){
    	
    	return application.sources(SpringBootFileManagerApplication.class);
    }
}

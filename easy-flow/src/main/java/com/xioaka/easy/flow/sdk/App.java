package com.xioaka.easy.flow.sdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author liuchengbiao
 * @date 2020-05-22 21:09
 */
@SpringBootApplication(scanBasePackages = {"com.xioaka.easy.flow"})
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

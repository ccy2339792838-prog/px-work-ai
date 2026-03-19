package com.pxwork.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.pxwork"})
@MapperScan("com.pxwork.**.mapper")
public class PxWorkApplication {

    public static void main(String[] args) {
        SpringApplication.run(PxWorkApplication.class, args);
        System.out.println("====== PxWork 后端启动成功！ ======");
    }
}
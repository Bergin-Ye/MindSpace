package com.mindspace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mindspace.mapper")
public class MindSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindSpaceApplication.class, args);
    }

}

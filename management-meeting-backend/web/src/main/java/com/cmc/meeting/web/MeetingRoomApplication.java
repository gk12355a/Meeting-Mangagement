package com.cmc.meeting.web;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan; // <-- Chỉ import 1 lần
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching

// 1. Quét @Service, @Component, @Repository (sẽ tìm thấy UserRepositoryAdapter)
@ComponentScan(basePackages = "com.cmc.meeting") 

// 2. Chỉ cho Spring biết nơi tìm @Entity (trong module infrastructure)
@EntityScan(basePackages = "com.cmc.meeting.infrastructure.persistence.jpa.entity") 

// 3. Kích hoạt & chỉ cho Spring nơi tìm JpaRepository (SpringDataUserRepository)
@EnableJpaRepositories(basePackages = "com.cmc.meeting.infrastructure.persistence.jpa.repository")

// 4. (Bonus) Chỉ cho Spring nơi tìm MyBatis Mappers
@MapperScan(basePackages = "com.cmc.meeting.infrastructure.persistence.mybatis.mapper")

// (KHÔNG CẦN @ComponentScan thứ hai nữa, vì "com.cmc.meeting" đã bao gồm cả 3)

public class MeetingRoomApplication {

    public static void main(String[] args) {
        
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        SpringApplication.run(MeetingRoomApplication.class, args);
    }
}
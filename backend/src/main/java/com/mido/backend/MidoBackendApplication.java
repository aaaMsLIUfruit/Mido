package com.mido.backend;

import com.mido.backend.config.FileStorageProperties;
import com.mido.backend.security.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan({"com.mido.backend.user.mapper", "com.mido.backend.note.mapper", "com.mido.backend.chat.mapper"})
@EnableConfigurationProperties({
		JwtProperties.class,
		FileStorageProperties.class,
		com.mido.backend.config.CorsProperties.class,
		com.mido.backend.config.AiProperties.class
})
public class MidoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MidoBackendApplication.class, args);
	}

}

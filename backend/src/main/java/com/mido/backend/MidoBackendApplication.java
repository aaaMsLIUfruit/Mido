package com.mido.backend;

import com.mido.backend.config.FileStorageProperties;
import com.mido.backend.security.JwtProperties;
import java.sql.Connection;
import javax.sql.DataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan({"com.mido.backend.user.mapper", "com.mido.backend.note.mapper"})
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

	@Bean
	CommandLineRunner testDB(DataSource dataSource) {
		return args -> {
			try (Connection conn = dataSource.getConnection()) {
				System.out.println("数据库连接成功：" + conn);
			} catch (Exception e) {
				System.err.println("数据库连接失败！");
				e.printStackTrace();
			}
		};
	}
}

package com.tecdes.smart.app_smart_40;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // produtores read-only do SSE rodam em @Scheduled
@EnableAsync      // broadcast do SseNotifier roda fora da thread do @Scheduled
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

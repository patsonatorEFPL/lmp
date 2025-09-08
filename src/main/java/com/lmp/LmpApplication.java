package com.lmp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.lmp.config.MailAddressConfig;

@SpringBootApplication
@EnableConfigurationProperties({MailAddressConfig.class})
@EnableScheduling // Pour l'auto-répondeur noreply
public class LmpApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmpApplication.class, args);
	}

}

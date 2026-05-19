package com.lmp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.lmp.notification.config.MailAddressConfig;
import com.lmp.shared.pricing.RegionalPricingProperties;
import com.lmp.support.config.SupportProperties;

@SpringBootApplication
@EnableConfigurationProperties({MailAddressConfig.class, RegionalPricingProperties.class, SupportProperties.class})
@EnableScheduling // Pour l'auto-répondeur noreply + purge scheduler
@EnableAsync // Pour le bus d'événements vers l’ERP (traitement asynchrone)
@EnableCaching // Caffeine pour agrégats ERP + GeoIP
public class LmpApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmpApplication.class, args);
	}

}

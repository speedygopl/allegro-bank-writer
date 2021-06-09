package org.piotr.allegrotestapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

@SpringBootApplication
public class AllegroTestAppApplication {
	static ConfigurableApplicationContext ctx;

		public static void main(String[] args) throws IOException {
		ctx = SpringApplication.run(AllegroTestAppApplication.class, args);

	}

	public static void serverShutdown() {
		ctx.close();
	}


}

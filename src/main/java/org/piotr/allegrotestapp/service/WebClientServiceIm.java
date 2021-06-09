package org.piotr.allegrotestapp.service;

import org.piotr.allegrotestapp.enums.AllegroEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

@Service
public class WebClientServiceIm implements WebClientService {

    @Autowired
    AllegroJwtToken allegroJwtToken;

    @Override
    public WebClient plainGetRequest() {
        return WebClient.builder()
                .baseUrl("http://localhost:8080")
                .defaultHeader("ACCEPT", AllegroEnum.ACCEPT.acceptHeader)
                .build();
    }

    @Override
    public WebClient httpRequestWithToken() throws IOException {
        return WebClient.builder()
                .baseUrl("https://api.allegro.pl")
                .defaultHeader("ACCEPT", AllegroEnum.ACCEPT.acceptHeader)
                .defaultHeader("Authorization", "Bearer " + allegroJwtToken.getAllegroJwtToken().getAccess_token())
                .defaultHeader("Content-Type", AllegroEnum.ACCEPT.acceptHeader)
                .build();
    }
    // tworzy requesta dla większych niż domyślne ilości danych
    @Override
    public WebClient httpRequestWithTokenMaxLoad() throws IOException {
        return   WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build())
                .baseUrl("https://api.allegro.pl")
                .defaultHeader("ACCEPT", AllegroEnum.ACCEPT.acceptHeader)
                .defaultHeader("Authorization", "Bearer " + allegroJwtToken.getAllegroJwtToken().getAccess_token())
                .defaultHeader("Content-Type", AllegroEnum.ACCEPT.acceptHeader)
                .build();
    }


}

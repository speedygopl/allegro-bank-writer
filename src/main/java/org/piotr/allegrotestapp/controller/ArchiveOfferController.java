package org.piotr.allegrotestapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.piotr.allegrotestapp.service.WebClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ArchiveOfferController {

    @Autowired
    WebClientService webClientService;

    @GetMapping("/archive")
    public JsonNode getArchiveOffer() throws IOException {
        JsonNode events = webClientService.httpRequestWithToken()
                .get()
                .uri("/sale/offer-events&limit=20&type=OFFER_ARCHIVED")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return events;
    }

}

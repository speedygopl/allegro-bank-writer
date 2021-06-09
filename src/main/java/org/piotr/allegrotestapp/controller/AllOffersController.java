package org.piotr.allegrotestapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.piotr.allegrotestapp.service.WebClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
public class AllOffersController {

    @Autowired
    WebClientService webClientService;

    public List<JsonNode> getOtherOffers() throws IOException {
        List<String> categoryList = Files.readAllLines(Path.of("category.txt"));
        List<JsonNode> offersList = new ArrayList<>();
        for (String s : categoryList) {
            JsonNode event = webClientService.httpRequestWithTokenMaxLoad()
                    .get()
                    .uri("/sale/offers?publication.status=ACTIVE&limit=999&category.id=" + s)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            offersList.add(event);
        }
        return offersList;
    }

    public List<String> getOtherOffersCD(List<JsonNode> offersList) {
        List<String> list = new ArrayList<>();
        for (JsonNode s : offersList) {
            int i = 0;
            while (s.get("offers").get(i) != null) {
                String id = s.get("offers").get(i).get("id").toString();
                String name = s.get("offers").get(i).get("name").toString();
                list.add(id + " " + name);
                i++;
            }
        }
        return list;
    }

    @GetMapping("/runotheroffers")
    public List<String> runAll() throws IOException {
        List<JsonNode> otherOffers = getOtherOffers();
        return getOtherOffersCD(otherOffers);
    }
}

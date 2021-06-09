package org.piotr.allegrotestapp.controller;


import com.fasterxml.jackson.databind.JsonNode;
import org.piotr.allegrotestapp.service.WebClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@RestController
public class OrderController {

    @Autowired
    WebClientService webClientService;



    @GetMapping("/orderevents")
    public JsonNode getOrderEvents(String fromId) throws IOException {
        JsonNode orderEvents = webClientService.httpRequestWithTokenMaxLoad()
                .get()
                .uri("/order/events?limit=1000&type=READY_FOR_PROCESSING&from=" + fromId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return orderEvents;
    }


    @GetMapping("/listoforderevents")
    public List<JsonNode> makeListOfOrderEvents(JsonNode orderEvents) throws IOException {
        List<JsonNode> orderEventsList = new ArrayList<>();
        int i = 0;
        while (orderEvents.get("events").get(i) != null) {
            orderEventsList.add(orderEvents.get("events").get(i));
            System.out.println(orderEventsList.get(i).get("order").get("buyer").get("login"));
            i++;
        }
        return orderEventsList;
    }

    public String saveLastOrderId(List<JsonNode> orderEventsList) throws FileNotFoundException {
        String lastOrderId = orderEventsList.get(orderEventsList.size()-1).get("id").toString().replaceAll("\"", "");
        PrintWriter pw = new PrintWriter(new FileOutputStream("lastOrder.txt", false));
        PrintWriter pw1 = new PrintWriter(new FileOutputStream("lastOrderList.txt", true));
        pw.print(lastOrderId);
        pw1.println(lastOrderId);
        pw.close();
        pw1.close();
        return lastOrderId;
    }

    @GetMapping("/listofoffers")
    public List<String> makeListOfOffers(List<JsonNode> orderEventsList) throws IOException {
        List<String> offerList = new ArrayList<>();
        for (JsonNode jn : orderEventsList) {
            int i = 0;
            while (jn.get("order").get("lineItems").get(i) != null) {
                offerList.add(jn.get("order").get("lineItems").get(i).get("offer").get("name").toString().replaceAll("\"", "") + "  " + jn.get("order").get("lineItems").get(i).get("quantity").toString().replaceAll("\"", ""));
                i++;
            }
        }
        return offerList;
    }

    @GetMapping("/listoftires")
    public List<String> makeListOfTires(List<String> offerList) throws IOException {
        return offerList.stream()
                .map(x -> x.toLowerCase())
                .filter(x -> x.contains("opona"))
                .collect(Collectors.toList());
    }

    @GetMapping("/runlistoftires")
    public List<String> getListOfTires() throws IOException {
        try {
            List<String> lastOrder = Files.readAllLines(Path.of("lastOrder.txt"));
            JsonNode orderEvents = getOrderEvents(lastOrder.get(0));
            List<JsonNode> orderEventsList = makeListOfOrderEvents(orderEvents);
            List<String> listOfOffers = makeListOfOffers(orderEventsList);
            List<String> listOfTires = makeListOfTires(listOfOffers);
            saveLastOrderId(orderEventsList);
            return listOfTires;
        } catch(Exception ex){
            List<String> errorList = new ArrayList<>();
            errorList.add(ex.getMessage());
            return errorList;
        }
    }

// METODY NIE WCHODZĄCE W SKŁAD PROGRAMU, NIEAKTYWNE
    @GetMapping("/buyercancelled")
    public JsonNode getBuyerCanceled(String fromId) throws IOException {
        JsonNode buyerCancelled = webClientService.httpRequestWithTokenMaxLoad()
                .get()
                .uri("/order/events?limit=1000&type=BUYER_CANCELLED&from=" + fromId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return buyerCancelled;

    }

    @GetMapping("/checkout")
    public JsonNode getCheckout() throws IOException {
        JsonNode checkout = webClientService.httpRequestWithTokenMaxLoad()
                .get()
                .uri("/order/checkout-forms?limit=100&fulfillment.status=NEW")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return checkout;
    }

    @GetMapping("/orderevents1")
    public JsonNode getOrderEvents1() throws IOException {
        JsonNode orderEvents = webClientService.httpRequestWithTokenMaxLoad()
                .get()
                .uri("/order/events")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return orderEvents;
    }



}

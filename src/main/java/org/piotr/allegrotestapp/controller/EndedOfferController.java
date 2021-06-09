package org.piotr.allegrotestapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.piotr.allegrotestapp.service.WebClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//klasa odpowiedzialna za zarządzanie zakończonymi aukcjami
@RestController
public class EndedOfferController {

    @Autowired
    WebClientService webClientService;


    // zdarzenia (zakończone oferty) od ostatniego zdarzenia tego typu
    @GetMapping("/offerendedevents")
    public JsonNode getOfferEndedEvents() throws IOException {
        List<String> lastEndedOffer = Files.readAllLines(Path.of("lastEndedOffer.txt"));
        JsonNode events = webClientService.httpRequestWithTokenMaxLoad()
                .get()
                .uri("/sale/offer-events?from=" + lastEndedOffer.get(0) + "&limit=500&type=OFFER_ENDED")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        System.out.println("-----------------------");
        System.out.println("getOfferEndedEvents");
        return events;
    }

    //tworzenie listy id aukcji które zostały zakończone od ostatniego zdarzenia tego typu
    //zapisuje do pliku id ostatniego zdarzenia typu zakończenie oferty
    public List<String> getOfferEndedIdList(JsonNode events) throws IOException {
        List<String> offerEndedList = new ArrayList<>();
        List<String> offerEndedIdList = new ArrayList<>();
        if (events.get("offerEvents").get(0) == null) {
            System.out.println("getOfferEndedIdList = null");
            return new ArrayList<>();
        }
        int i = 0;
        while (events.get("offerEvents").get(i) != null) {
            offerEndedList.add(events.get("offerEvents").get(i).get("id").toString().replaceAll("\"", ""));
            offerEndedList.add(events.get("offerEvents").get(i).get("offer").get("id").toString().replaceAll("\"", ""));
            i++;
        }
        PrintWriter pw = new PrintWriter(new FileOutputStream("lastEndedOffer.txt"));
        PrintWriter pw1 = new PrintWriter(new FileOutputStream("lastEndedOfferList.txt", true));
        pw.println(offerEndedList.get(offerEndedList.size() - 2));
        pw1.println(offerEndedList.get(offerEndedList.size() - 2));
        pw1.println(" ");
        pw.close();
        pw1.close();
        int x = 0;
        for (int j = 1; x + j < offerEndedList.size(); j++) {
            offerEndedIdList.add(offerEndedList.get(x + j));
            x++;
        }
        System.out.println("getOfferEndedIdList");
        return offerEndedIdList;

    }

    //tworzenie listy ID takich, których jeszcze nie było w bazie danych ofert zakończonych
    public List<Long> listOfUniqueIdEndedOffers(List<String> newEnded) throws IOException {
        List<Long> resultEnded = new ArrayList<>();
        for (String s : newEnded) {
            Stream<String> stream = Files.lines(Paths.get("endedAuctions2.txt"), Charset.forName("UTF-8"));
            long count = stream.filter(lines -> lines.contains(s))
                    .count();
            if (count == 0) {
                resultEnded.add(Long.valueOf(s));
            }
        }
        if (resultEnded.size() == 0) {
            System.out.println("listOfUniqueIdEndedOffers = null");
        } else {
            System.out.println("listOfUniqueIdEndedOffers");
        }
        return resultEnded;
    }


    // tworzy listę poszczególnych zakończonych aukcji po id aukcji,
    // pobiera szczegóły aukcji i zapisuje do listy
    @GetMapping("/endedauctiondetaillist")
    public List<String> getEndedAuctionsDetails(List<Long> endedAuctionsIdList) throws IOException {
        List<String> endedAuctionsDetails = new ArrayList<>();
        for (int i = 0; i < endedAuctionsIdList.size(); i++) {
            JsonNode ended = webClientService.httpRequestWithTokenMaxLoad()
                    .get()
                    .uri("/sale/offers/" + endedAuctionsIdList.get(i).toString())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            endedAuctionsDetails.add(ended.toString());
        }
        System.out.println("getEndedAuctionsDetails");
        return endedAuctionsDetails;
    }

    //łączy dwie listy, stare zakończone i nowe
    //jeżeli lista nowych jest pusta to zwraca stare zakończone
    //jeżeli lista nowych nie jest pusta to łączy dwie listy,
    // i zapisuje wynik do pliku
    //nową listę zakończonych przerabia na charset windows-1250
    @GetMapping("/saveendedauctiondetails")
    public List<String> mergeEndedOfferDetailLists(List<String> newEndedOfferDetailList) throws IOException {
        List<String> oldEndedOfferDetailList = Files.readAllLines(Path.of("endedAuctions2.txt"), Charset.forName("UTF-8"));
        oldEndedOfferDetailList.addAll(newEndedOfferDetailList);
        OutputStream os = new FileOutputStream("endedAuctions2.txt", false);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, Charset.forName("UTF-8")));
        for (String s : oldEndedOfferDetailList)
            pw.println(s);
        pw.close();
        System.out.println("mergeEndedOfferDetailLists");
        return newEndedOfferDetailList;
    }

    //metoda uruchamiająca proces
    public List<String> runSaveEnded() throws IOException {
        JsonNode offerEndedEvents = getOfferEndedEvents();
        List<String> offerEndedIdList = getOfferEndedIdList(offerEndedEvents);
        List<Long> unique = listOfUniqueIdEndedOffers(offerEndedIdList);
        List<String> endedAuctionsDetails = getEndedAuctionsDetails(unique);
        List<String> strings = mergeEndedOfferDetailLists(endedAuctionsDetails);
        System.out.println("runSaveEnded");
        return strings;
    }
// --------------------------------------------------------------------------------------
    // metody spoza programu

    // pokazuje oferty zakończone w ciągu ostatnich 24 godzin
    @GetMapping("/lastended")
    public JsonNode lastEnded() throws IOException {
        JsonNode events = webClientService.httpRequestWithTokenMaxLoad()
                .get()
                .uri("/sale/offer-events?limit=500&type=OFFER_ENDED")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return events;
    }


    public JsonNode getActiveOffers(String offset) throws IOException {
        JsonNode jsonActiveOffers = webClientService.httpRequestWithTokenMaxLoad()
                .get()
                .uri("/sale/offers?publication.status=ACTIVE&limit=1000&offset=" + offset)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return jsonActiveOffers;
    }

    public Map<String, String> getMapOfAuctionidAndSignatures(JsonNode jsonActiveOffers) {
        Map<String, String> mapOfAuctionidAndSygnatures = new HashMap<>();
        int i = 0;
        while (jsonActiveOffers.get("offers").get(i) != null) {
            mapOfAuctionidAndSygnatures.put(jsonActiveOffers.get("offers").get(i).get("name").toString().replaceAll("\"", ""),
                    jsonActiveOffers.get("offers").get(i).get("external").get("id") == null ? null : jsonActiveOffers.get("offers").get(i).get("external").get("id").toString().replaceAll("\"", ""));
            i++;
        }
        return mapOfAuctionidAndSygnatures;
    }

    public List<String> getFiltered(Map<String, String> mapOfAuctionidAndSygnatures) {
        List<String> filteredList = new ArrayList<>();
        mapOfAuctionidAndSygnatures.values().removeIf(Objects::nonNull);
        for (String s : mapOfAuctionidAndSygnatures.keySet()) {
            filteredList.add(s);
        }
        return filteredList;
    }

    @GetMapping("/nosyg")
    public List<String> noSygnatureList() throws IOException {
        JsonNode jsonActiveOffers1 = getActiveOffers("1");
        JsonNode jsonActiveOffers2 = getActiveOffers("1001");
        JsonNode jsonActiveOffers3 = getActiveOffers("2001");
        JsonNode jsonActiveOffers4 = getActiveOffers("3001");
        Map<String, String> mapOfAuctionidAndSygnatures1 = getMapOfAuctionidAndSignatures(jsonActiveOffers1);
        Map<String, String> mapOfAuctionidAndSygnatures2 = getMapOfAuctionidAndSignatures(jsonActiveOffers2);
        Map<String, String> mapOfAuctionidAndSygnatures3 = getMapOfAuctionidAndSignatures(jsonActiveOffers3);
        Map<String, String> mapOfAuctionidAndSygnatures4 = getMapOfAuctionidAndSignatures(jsonActiveOffers4);
        List<String> filteredList1 = getFiltered(mapOfAuctionidAndSygnatures1);
        List<String> filteredList2 = getFiltered(mapOfAuctionidAndSygnatures2);
        List<String> filteredList3 = getFiltered(mapOfAuctionidAndSygnatures3);
        List<String> filteredList4 = getFiltered(mapOfAuctionidAndSygnatures4);
        List<String> allFiltered = new ArrayList<>();
        allFiltered.addAll(filteredList1);
        allFiltered.addAll(filteredList2);
        allFiltered.addAll(filteredList3);
        allFiltered.addAll(filteredList4);

        return allFiltered;
    }

    @GetMapping("/id")
    public JsonNode getAuctionDetails(String id) throws IOException {
        return webClientService.httpRequestWithToken()
                .get()
                .uri("/sale/offers/" + id)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

}

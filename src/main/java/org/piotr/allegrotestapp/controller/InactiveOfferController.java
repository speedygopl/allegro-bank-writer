package org.piotr.allegrotestapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.piotr.allegrotestapp.service.WebClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class InactiveOfferController {

    @Autowired
    WebClientService webClientService;

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ExceptionController exceptionController;

    @Autowired
    ImageController imageController;

    boolean store;

    //----------------------------search for auction in archive------------------------------------------------
    // metoda wyszukuje podany ciąg znaków w bazie aukcji,
    // zwraca listę (ID, NAME, EXTERNAL ID) aukcji, które zawierają ten ciąg znaków
    @GetMapping("/searchforauction")
    public List<String> searchForAuction(String searchString) throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get("endedAuctions2.txt"), Charset.forName("UTF-8"));
        List<String> results = allLines.stream()
                .map(x -> x.toLowerCase())
                .filter(x -> x.contains(searchString.toLowerCase()))
                .collect(Collectors.toList());
        List<JsonNode> resultsInJsonnode = changeListOfStringsToListOfJsonnode(results);
        for(JsonNode jn : resultsInJsonnode){
            System.out.println(jn);
        }
        return getIdNameExternalidFromListOfJsonnode(resultsInJsonnode);
    }

    // zamienia listę stringów na listę obiektów JSON
    public List<JsonNode> changeListOfStringsToListOfJsonnode(List<String> listOfString) throws JsonProcessingException {
        List<JsonNode> results = new ArrayList<>();
        for (String s : listOfString) {
            try {
                results.add(objectMapper.readTree(s));
            } catch(Exception ex){
                System.out.println(ex);
            }
        }
        return results;
    }

    // tworzy (z listy obiektów JSON) listę stringów składającą się z ID oraz NAME oferty
    public List<String> getIdNameExternalidFromListOfJsonnode(List<JsonNode> listOfJsonnode) {
        List<String> results = new ArrayList<>();
        for (JsonNode jn : listOfJsonnode) {
            try {
                results.add(jn.get("id").toString().replaceAll("\"", "")
                        + ";".replaceAll("\"", "")
                        + "      "
                        + jn.get("name").toString().replaceAll("\"", "")
                        + ";     "
                        + jn.get("external").get("id").toString().replaceAll("\"", ""));
            }catch(Exception ex) {
                System.out.println(ex);
            }
        }
        return results;
    }

    //--------------------------------------make inactive offer from archive-------------------------------
    @GetMapping("/findbyexternalid")
    public JsonNode getEndedOfferDetailsByExternalId(String searchString) throws IOException {
        List<String> alllines = Files.readAllLines(Paths.get("endedAuctions2.txt"), Charset.forName("UTF-8"));
        Optional<String> firstResult = alllines.stream()
                .filter(x -> x.contains(searchString))
                .findFirst();
        if (firstResult.isEmpty()) {
            return showEmptyJson();
        } else {
            String result = firstResult.get();
            return objectMapper.readTree(result);
        }
    }


    public JsonNode showEmptyJson() throws IOException {
        File emptyJson = new File("emptyJson.txt");
        return objectMapper.readTree(emptyJson);
    }


    @GetMapping("/changejsontomakeinactiveoffer")
    public JsonNode changeJsonToMakeInactiveOffer(String externalId) throws IOException {
        JsonNode json = getEndedOfferDetailsByExternalId(externalId);
        if (json.equals(showEmptyJson())) {
            return showEmptyJson();
        } else {
            ObjectNode object = (ObjectNode) json;
            object.remove("id");
            object.remove("publication");
            object.remove("createdAt");
            object.remove("updatedAt");
            object.remove("validation");
            return json;
        }
    }

    @GetMapping("/makeinactiveoffer")
    public JsonNode makeInactiveOffer(String searchString) throws IOException {
        JsonNode json = changeJsonToMakeInactiveOffer(searchString);
        if (json.equals(showEmptyJson())) {
            return showEmptyJson();
        } else {
            JsonNode block = webClientService.httpRequestWithToken()
                    .post()
                    .uri("/sale/offers")
                    .body(BodyInserters.fromValue(json.toString()))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return block;
        }
    }

    //-------------------------------------double existing inactive offer-----------------------------


    //zwraca id najnowszej aukcji po sygnaturze produktu,
    //jednak json zawiera mało szczegółów i wystawia się z tego okrojony draft bez zdjęcia
    @GetMapping("/inactiveofferbyexternalid")
    public JsonNode getInactiveOfferByExternalId(String externalId) throws IOException {
        JsonNode result = webClientService.httpRequestWithToken()
                .get()
                .uri("/sale/offers?external.id=" + externalId + "&publication.status=INACTIVE&publication.status=ENDED")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return result.get("offers").get(0);
    }

    //zwraca szczegóły oferty po id aukcji
    @GetMapping("/inactiveofferbyid")
    public JsonNode getInactiveOfferById(String id) throws IOException {
        JsonNode result = webClientService.httpRequestWithToken()
                .get()
                .uri("/sale/offers/" + id)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return result;
    }

    //zmienia ofertę aby można było stworzyć z niej draft, usuwa pewne pola
    public JsonNode changeInactiveOfferToDoubleIt(JsonNode inactiveOffer) throws IOException {
        ObjectNode object = (ObjectNode) inactiveOffer;
        object.remove("id");
        object.remove("publication");
        object.remove("createdAt");
        object.remove("updatedAt");
        object.remove("validation");
        return inactiveOffer;
    }


    //powiela draft wcześniej przygotowanej oferty
    public JsonNode doubleInactiveOffer(JsonNode inactiveOffer) throws IOException {
        JsonNode result = webClientService.httpRequestWithToken()
                .post()
                .uri("/sale/offers")
                .body(BodyInserters.fromValue(inactiveOffer.toString()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return result;
    }

    //metoda wykonująca , wykonuje wszystkie kroki po kolei, jeżeli nie znaleziono wyników
    //to zwróci BŁĄD w stringu.
    @GetMapping("/rundouble")
    public String runDouble(String id) throws IOException {
        try {
            JsonNode inactiveOffer = getInactiveOfferById(id);
            JsonNode inactiveOfferCorrected = changeInactiveOfferToDoubleIt(inactiveOffer);
            doubleInactiveOffer(inactiveOfferCorrected);
            return "AUKCJA ZOSTAŁA POWIELONA";
        } catch (Exception ex) {
            return "BŁĄD" + " " + ex.getMessage();
        }

    }

//    ----------------------------------- store inactive offers ------------------------------

    //zwraca najnowsze oferty szkice, określamy limit, zaczyna od najnowszej oferty (offset=0)
    @GetMapping("/inactiveoffers")
    public JsonNode getInactiveOffers(String limit) throws IOException {
        JsonNode newestOffersInactive = webClientService.httpRequestWithTokenMaxLoad()
                .get()
                .uri("/sale/offers?limit=" + limit + "&offset=0&publication.status=INACTIVE")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return newestOffersInactive;
    }

    //zwraca listę numerów ID najnowszych ofert szkiców
    public List<String> getIdListOfInactiveOffers(JsonNode offersInactive) {
        List<String> idListOfInactiveOffers = new ArrayList<>();
        int i = 0;
        while (offersInactive.get("offers").get(i) != null) {
            idListOfInactiveOffers.add(offersInactive.get("offers").get(i).get("id").toString().replaceAll("\"", ""));
            i++;
        }
        return idListOfInactiveOffers;
    }

    //zwraca listę numerów ID ofert szkiców ale tylko tych, które jeszcze nie były save'owane
    //zaczynając od ofery , na której skończyliśmy poprzednio save'owanie
    public List<String> getNewestIdListOfInactiveOffers(List<String> idListOfInactiveOffers) throws IOException {
        List<String> lastInactiveOffer = Files.readAllLines(Path.of("lastInactiveOffer.txt"));
        List<String> newestIdListOfInactiveOffers = new ArrayList<>();
        Integer point = null;
        for (int i = 0; i < idListOfInactiveOffers.size(); i++) {
            if (lastInactiveOffer.get(0).equals(idListOfInactiveOffers.get(i))) {
                point = i;
            }
        }
        for (int i = 0; i < point; i++) {
            newestIdListOfInactiveOffers.add(idListOfInactiveOffers.get(i));
        }
        if(point != 0 && point != null && store) {
            PrintWriter pw = new PrintWriter(new FileOutputStream("lastInactiveOffer.txt", false));
            pw.println(newestIdListOfInactiveOffers.get(0));
            pw.close();
        }
        return newestIdListOfInactiveOffers;
    }


    //pobiera szczegóły najnowszych ofert szkiców
    public List<String> getDetailsOfInactiveOffers(List<String> idList) throws IOException {
        List<String> offersDetailsList = new ArrayList<>();
        for (String s : idList) {
            String offerDetails = webClientService.httpRequestWithTokenMaxLoad()
                    .get()
                    .uri("/sale/offers/" + s)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            offersDetailsList.add(offerDetails);
        }
        return offersDetailsList;
    }


    //zapisuje szczegóły poszczególnych ofert szkiców do 2 plików, drugi plik potrzebny
    //do metody ściągającej zdjęcia szkiców
    public void saveResultsToFile(List<String> offersDetailsList) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new FileOutputStream("endedAuctions2.txt", true));
        for (String s : offersDetailsList) {
            pw.println(s);
        }
        pw.close();
        PrintWriter pw1 = new PrintWriter(new FileOutputStream("inactiveAuctions2.txt", false));
        for (String s1 : offersDetailsList) {
            pw1.println(s1);
        }
        pw1.close();
    }

    //metoda agregacyjna , uruchamiająca cały proces zapisu szczegółów szkiców do pliku
    @GetMapping("/runstoreinactive")
    public String runStoreInactive() throws IOException {
        store = true;
        try {
            JsonNode offers = getInactiveOffers("100");
            List<String> idList = getIdListOfInactiveOffers(offers);
            List<String> newestIdList = getNewestIdListOfInactiveOffers(idList);
            List<String> offersDetailsList = getDetailsOfInactiveOffers(newestIdList);
            saveResultsToFile(offersDetailsList);
            imageController.runDownloadInactiveImages();
            return "POWIODŁO SIĘ";
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }


    @GetMapping("/runshowinactive")
    public List<String> runShowInactive() throws IOException {
        store = false;
        try {
            JsonNode offers = getInactiveOffers("100");
            List<String> idList = getIdListOfInactiveOffers(offers);
            List<String> newestIdList = getNewestIdListOfInactiveOffers(idList);
            return getDetailsOfInactiveOffers(newestIdList);
        } catch (Exception ex) {
            List<String> list = new ArrayList<>();
            list.add(ex.getMessage());
            return list;
        }
    }

}

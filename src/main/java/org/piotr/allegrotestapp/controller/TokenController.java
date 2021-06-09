package org.piotr.allegrotestapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.piotr.allegrotestapp.AllegroTestAppApplication;
import org.piotr.allegrotestapp.model.Token;
import org.piotr.allegrotestapp.service.UuidService;
import org.piotr.allegrotestapp.service.WebClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


//klasa odpowiedzialna za odpisywanie banków
@RestController
public class TokenController {

    @Autowired
    private RestTemplate template;
    @Autowired
    WebClientService webClientService;


    List<String> allLines = new ArrayList<>();
    String id = "";
    String auctionStatus = "";
    Map<String, Integer> inputMap = new HashMap<>();
    Integer updatedQuantityInStock;
    List<String> resultsSuccess = new ArrayList<>();
    List<String> resultsError = new ArrayList<>();
    List<String> resultsClosed = new ArrayList<>();
    List<String> resultsAll = new ArrayList<>();
    Integer quantityBeforeChange;
    PrintStream console = System.out;

    public List<String> readAllLinesFromFile() throws IOException {
        allLines = Files.readAllLines(Paths.get("c:\\AllegroApi\\plik.txt"), Charset.forName("windows-1250"));
        for (String s : allLines) {
            System.out.println(s);
        }
        return allLines;
    }

    public Map<String, Integer> getInputMap() throws IOException {
        allLines = readAllLinesFromFile();
        Pattern p = Pattern.compile("([ +][0-9]+[|])(.{30})( +)([|])( +)([0-9]+)");
        Matcher m;
        for (int i = 0; i < allLines.size(); i++) {
            m = p.matcher(allLines.get(i));
            if (m.find()) {
                inputMap.put(m.group(2).trim(), Integer.valueOf(m.group(6)));
            }
        }
        return inputMap;
    }


    @GetMapping("/refresh")
    public List<String> useRefreshToken() throws IOException {
        //utworzenie mappera json
        ObjectMapper objectMapper = new ObjectMapper();
        //utworzenie obiektu tokena
        Token token = objectMapper.readValue(new File("token.txt"), Token.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "basic YmU0N2ZjNjUwZWRhNGE0MTgzOWFhYzRlZDAzNmRjNzM6b29GZHpuR2p6RURKcGo0MFhvU1RlVEloa2tqTHZyQmVBREFuZkFBUUQ4ZHNrYVd2MElMdEZ4ZVdSb0xCRkNxcg==");
        String body = "";
        HttpEntity<String> requestEntity = new HttpEntity<String>(body, headers);
        ResponseEntity<String> responseEntity = template.exchange("https://allegro.pl/auth/oauth/token?grant_type=refresh_token&refresh_token=" + token.getRefresh_token() + "&redirect_uri=http://localhost:8080", HttpMethod.POST, requestEntity, String.class);
        String response = responseEntity.getBody();
        //utworzenie streamu dla System.Out.Println do zapisywania w pliku
        //utworzenie zmiennej do wyświetlania na konsoli
        PrintStream file = new PrintStream(new File("token.txt"));
        //stream odpowiedzi serwera do pliku
        System.setOut(file);
        System.out.println(response);
        //przestawienie streamu na konsolę i wyświetlenie informacji o obiekcie token
        System.setOut(console);
        token = objectMapper.readValue(new File("token.txt"), Token.class);
        return token.toList();
    }

    public JsonNode changeJson(JsonNode inputJson, Integer quantityToChange) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode changedJson = objectMapper.readValue(inputJson.toString(), JsonNode.class);
        Integer available = changedJson.get("stock").get("available").asInt();
        quantityBeforeChange = available;
        updatedQuantityInStock = available - quantityToChange;
        if (updatedQuantityInStock <= 0) {
            System.out.println("zamykanie aukcji");
            closeAuction();
        } else {
            ((ObjectNode) changedJson.get("stock")).put("available", updatedQuantityInStock);
        }
        return changedJson;
    }

    public JsonNode changeJsonForAuctionClose() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonAuctionCloseChanged = objectMapper.readValue(new File("auctionclosed.txt"), JsonNode.class);
        System.out.println(jsonAuctionCloseChanged);
        ((ObjectNode) jsonAuctionCloseChanged.get("offerCriteria").get(0).get("offers").get(0)).put("id", id);
        System.out.println(jsonAuctionCloseChanged);
        return jsonAuctionCloseChanged;
    }


    public void closeAuction() throws IOException {
        UuidService uuidService = new UuidService();
        String uuid = uuidService.uuid.toString();
        System.out.println(uuid);
        String jsonAuctionCloseChanged = changeJsonForAuctionClose().toString();
        webClientService.httpRequestWithToken()
                .put()
                .uri("/sale/offer-publication-commands/" + uuid)
                .body(BodyInserters.fromValue(jsonAuctionCloseChanged))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    @GetMapping
    public JsonNode getOfferById(String externalId) throws IOException, InterruptedException {
        JsonNode jsonByExternalId = webClientService.httpRequestWithToken()
                .get()
                .uri("/sale/offers?external.id=" + externalId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        System.out.println(jsonByExternalId);
        if (jsonByExternalId.get("offers").get(0) == null) {
            auctionStatus = "BRAK SYGNATURY";
        } else {
            System.out.println("jestem tutaj");
            Integer auctionStatusId = getAuctionStatusId(jsonByExternalId);
            System.out.println("i tu");
            auctionStatus = jsonByExternalId.get("offers").get(auctionStatusId).get("publication").get("status").toString().replaceAll("\"", "");
            System.out.println("i tutu");
            if (auctionStatus.equals("ACTIVE")) {
                id = jsonByExternalId.get("offers").get(auctionStatusId).get("id").toString().replaceAll("\"", "");
                JsonNode jsonById = webClientService.httpRequestWithToken()
                        .get()
                        .uri("/sale/offers/" + id)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
                return jsonById;
            }
        }
        return null;
    }

    @GetMapping("/dojob")
    public List<String> doJob() throws IOException {
        if (!resultsAll.isEmpty()) {
            List<String> list = new ArrayList<>();
            list.add("NIE MOŻNA DWUKROTNIE ODPISAĆ BANKÓW");
            return list;
        } else {
            for (Map.Entry<String, Integer> entry : getInputMap().entrySet()) {
                try {
                    JsonNode inputJson = getOfferById(entry.getKey());
                    JsonNode outputJson = changeJson(inputJson, entry.getValue());
                    if (updatedQuantityInStock <= 0) {
                        resultsClosed.add("offer " + entry.getKey() + " auction CLOSED!!!" + " (quantity after change = " + updatedQuantityInStock + ")");
                    } else {
                        changeOfferWithOutputJson(outputJson);
                        resultsSuccess.add("offer " + entry.getKey() + " changed SUCCESSFULLY !!!" + " (quantity : before change = " + quantityBeforeChange + " , after change = " + updatedQuantityInStock + ")");
                    }
                } catch (Exception exc) {
                    resultsError.add("offer " + entry.getKey() + " - NOT CHANGED!!!" + " (quantity to change = " + entry.getValue() + "); " + " Auction Status : " + auctionStatus);
                }
            }
            return mergeResultsLists();
        }
    }

    @RequestMapping
    public boolean changeOfferWithOutputJson(JsonNode outputJson) throws IOException {
        JsonNode putJson = webClientService.httpRequestWithToken()
                .put()
                .uri("/sale/offers/" + id)
                .body(BodyInserters.fromValue(outputJson.toString()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return true;
    }

    //tworzenie listy łączącej wszystkie 3 listy z rezultatami
    public List<String> mergeResultsLists() throws FileNotFoundException {
        resultsAll.add("RESULTS QUANTITY CHANGED !!!");
        resultsAll.addAll(resultsSuccess);
        resultsAll.add("---------------------------------------------------------------------------");
        resultsAll.add("RESULTS AUCTIONS CLOSED !!!");
        resultsAll.addAll(resultsClosed);
        resultsAll.add("---------------------------------------------------------------------------");
        resultsAll.add("RESULTS WITH ERRORS !!!");
        resultsAll.addAll(resultsError);
        writeResultsToFile();
        return resultsAll;
    }

    // metoda wyłączająca serwer
    @GetMapping("/shutdown")
    public void shutdown() throws FileNotFoundException {
        System.setOut(console);
        PrintWriter writer = new PrintWriter("c:\\AllegroApi\\plik.txt");
        writer.print("");
        writer.close();
        AllegroTestAppApplication.serverShutdown();
    }

    //metoda wykonywana po wejściu na localhost:8080 - przekierowuje na index.html
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView index() {
        return new ModelAndView("index");
    }

    public void writeResultsToFile() throws FileNotFoundException {
        PrintStream file = new PrintStream(new File("c:\\AllegroApi\\outputFile.txt"));
        System.setOut(file);
        for (String s : resultsAll) {
            System.out.println(s);
        }
    }

    // zwraca numer statusu aukcji,najpierw przeszukiwane są aukcje w poszukiwaniu ofert aktywnych,
    //jeśli nie znajdzie to szuka zakończone, a jeśli tych nie ma to szuka szkicy,
    //może być kolekcja tej samej aukcji o różnym statusie np może być szkic aukcji i aukcja aktywna
    // razem pod jednym symbolem
    public Integer getAuctionStatusId(JsonNode jsonByExternalId) {
        Integer i = 0;
        while (jsonByExternalId.get("offers").get(i) != null) {
            System.out.println("petla active");
            if (jsonByExternalId.get("offers").get(i).get("publication").get("status").toString().replaceAll("\"", "").equals("ACTIVE")) {
                System.out.println("active " + i);
                return i;
            }
            i++;
        }
        i = 0;
        while (jsonByExternalId.get("offers").get(i) != null) {
            System.out.println("petla ended");
            if (jsonByExternalId.get("offers").get(i).get("publication").get("status").toString().replaceAll("\"", "").equals("ENDED")) {
                System.out.println("ended " + i);
                return i;
            }
            i++;
        }
        i = 0;
        while (jsonByExternalId.get("offers").get(i) != null) {
            System.out.println("petla inactive");
            if (jsonByExternalId.get("offers").get(i).get("publication").get("status").toString().replaceAll("\"", "").equals("INACTIVE")) {
                System.out.println("inactive " + i);
                return i;
            }
            i++;
        }
        return 0;
    }
}

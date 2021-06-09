package org.piotr.allegrotestapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.piotr.allegrotestapp.service.WebClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

@RestController
public class ImageController {

    @Autowired
    WebClientService webClientService;

    @Autowired
    EndedOfferController endedOfferController;

    @Autowired
    TokenController tokenController;


    //konwertuje listę aukcji w stringach na listę auckji w formacie JsonNode
    @GetMapping("/tojson")
    public List<JsonNode> convertStringListOfEndedAuctionsToJsonList(List<String> strings) throws IOException {
        List<JsonNode> json = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (String s : strings) {
            JsonNode objectNode = objectMapper.readTree(s);
            json.add(objectNode);
        }
        System.out.println("convertStringListOfEndedAuctionsToJsonList");
        return json;
    }

    //wyodrębnia adresy internetowe zdjęć i zapisuje je w mapie, gdzie kluczem
    // jest id aukcji a wartością adres internetowy
    @GetMapping("/imageadresses")
    public Map<String, String> extractImageAdresses(List<String> strings) throws IOException {
        List<JsonNode> json = convertStringListOfEndedAuctionsToJsonList(strings);
        Map<String, String> stringsMap = new HashMap<>();
        for (int i = 0; i < json.size(); i++) {
            try {
                stringsMap.put(json.get(i).get("id").toString().replaceAll("\"", ""), json.get(i).get("images").get(0).get("url").toString().replaceAll("\"", ""));
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
        System.out.println("extractImageAdresses");
        return stringsMap;
    }

    //zapisuje zdjęcia na dysk do c:\images
    @GetMapping("/downloadimages")
    public void downloadImages(List<String> list) throws IOException {
        Map<String, String> strings = extractImageAdresses(list);
        // This will get input data from the server
        InputStream inputStream = null;
        // This will read the data from the server;
        OutputStream outputStream = null;
        for (int i = 0; i < strings.size(); i++) {
            Object key = strings.keySet().toArray()[i];
            try {
                // This will open a socket from client to server
                URL url = new URL(strings.get(key));
                // This user agent is for if the server wants real humans to visit
                String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
                // This socket type will allow to set user_agent
                URLConnection con = url.openConnection();
                // Setting the user agent
                con.setRequestProperty("User-Agent", USER_AGENT);
                // Requesting input data from server
                inputStream = con.getInputStream();
                // make a file with path to it
                File file = new File("/images/" + key.toString() + ".jpg");
                // Open local file writer
                outputStream = new FileOutputStream(file);
                // Limiting byte written to file per loop
                byte[] buffer = new byte[2048];
                // Increments file size
                int length;
                // Looping until server finishes
                while ((length = inputStream.read(buffer)) != -1) {
                    // Writing data
                    outputStream.write(buffer, 0, length);
                }
            } catch (Exception ex) {
                System.out.println(ex);

            }
        }
        // closing used resources
        // The computer will not be able to use the image
        // This is a must
        outputStream.close();
        inputStream.close();
        System.out.println("downloadImages");
    }

    @GetMapping("/downloadendedimages")
    public String runDownloadEndedImages() {
        try {
            downloadImages(endedOfferController.runSaveEnded());
            return "POWODZENIE";
        } catch (Exception ex) {
            return ex.toString();
        }
    }


    @GetMapping("/downloadinactiveimages")
    public String runDownloadInactiveImages() {
        try {
            downloadImages(Files.readAllLines(Paths.get("inactiveAuctions2.txt")));
            return "POWODZENIE";
        } catch (Exception ex) {
            return ex.toString();
        }
    }


}

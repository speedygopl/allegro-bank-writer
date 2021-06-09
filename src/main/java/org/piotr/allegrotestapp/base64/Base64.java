package org.piotr.allegrotestapp.base64;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;

public class Base64 {
    public static void main(String[] args) throws UnsupportedEncodingException {


        String clientId = "be47fc650eda4a41839aac4ed036dc73";
        String clientSecret = "ooFdznGjzEDJpj40XoSTeTIhkkjLvrBeADAnfAAQD8dskaWv0ILtFxeWRoLBFCqr";
        String encodedData = DatatypeConverter.printBase64Binary((clientId + ":" + clientSecret).getBytes("UTF-8"));
        String authorizationHeaderString = "Authorization: Basic " + encodedData;
        System.out.println(authorizationHeaderString);
    }
}

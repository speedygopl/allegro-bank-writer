package org.piotr.allegrotestapp.controller;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestClass {
    public static void main(String[] args) throws IOException {
        System.out.println(doStr("aaacodebbb"));

    }

    public void test() throws FileNotFoundException, UnsupportedEncodingException {
        OutputStream os = new FileOutputStream("test.txt");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "windows-1250"));
        pw.close();
    }

    public static int doStr(String str) {
        List<Integer> list = new ArrayList<>();
        for (int y = 0; y < str.length(); y++) {
            y = str.indexOf("co", y);
            if (y == -1) {
                break;
            }
            if(y<str.length()-3) {
                if (str.charAt(y + 3) == 'e') {
                    list.add(y);
                }
            }
        }
        return list.size();
    }
}

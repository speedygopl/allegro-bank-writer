package org.piotr.allegrotestapp.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Token {

    private String access_token = "";
    private String token_type = "";
    private String refresh_token = "";
    private long expires_in = 0;
    private String scope = "";
    private boolean allegro_api = false;
    private String jti = "";


    public List<String> toList() {
        List<String> list = new ArrayList<>();
        list.add("access_token last 10 characters='" + access_token.substring(access_token.length() - 10) + '\'');
        list.add("token_type='" + token_type + '\'');
        list.add("refresh_token last 10 characters='" + refresh_token.substring(refresh_token.length() - 10) + '\'');
        list.add("expires_in=" + expires_in);
        list.add("scope='" + scope + '\'');
        list.add("allegro_api=" + allegro_api);
        list.add("jti='" + jti + '\'');
        return list;
    }

}

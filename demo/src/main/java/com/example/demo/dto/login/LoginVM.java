package com.example.demo.dto.login;

import com.example.demo.utils.json.LowerCaseTrimDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginVM {

    @JsonDeserialize(using = LowerCaseTrimDeserializer.class)
    private String username;
    private String password;
    private boolean rememberMe;
}
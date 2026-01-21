package com.example.demo.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class CommonUtils {
    public static boolean isEmpty(String... values){
        return Stream.of(values).anyMatch(StringUtils::isBlank);
    }
}

package com.example.demo.dto.excel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExcelTemplateConfig {

    private List<String> headers;
    private List<Integer> fieldRequired;
    private boolean autoNumber;
    private List<ExcelValidation> listValidations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class ExcelValidation {
        private String rangeName;
        private int rowIndex;
        private List<String> data;
    }
}

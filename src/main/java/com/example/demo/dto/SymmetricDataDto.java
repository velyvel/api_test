package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SymmetricDataDto {
    private String key;
    private String iv;
    private String hmac_key;
}

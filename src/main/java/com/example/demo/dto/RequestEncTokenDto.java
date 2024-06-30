package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestEncTokenDto {
  private String accessToken;
  private String clientId;
}

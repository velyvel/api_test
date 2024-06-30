package com.example.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@Tag(name = "기관 토큰 발급", description = "NICE 기관토큰 발급 관련 API")
public class MakeAccessTokenController {
  //private final String RETURN_URL = "http://localhost:8080/";
  @Value("${nice.clientId}")
  private String CLIENT_ID;

  @Value("${nice.clientSecret}")
  private String CLIENT_SECRET;

  private final String ACCESS_TOKEN_URL = "https://svc.niceapi.co.kr:22001/digital/niceid/oauth/oauth/token";

  // 기관 토큰 발급 시 ip 주소 중요함
  @PostMapping("/requestAccessToken")
  public ResponseEntity<JsonNode> requestAccessToken() {
    JsonNode returnData = null;

    String authorization = makeAccessTokenAuthorization();
    // HttpHeaders 설정
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/x-www-form-urlencoded");
    headers.add("Authorization", authorization);

    // Body 데이터 설정
    String body = "grant_type=client_credentials&scope=default";

    // HttpEntity 생성
    HttpEntity<String> request = new HttpEntity<>(body, headers);

    // RestTemplate 생성
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();

    try {
      // POST 요청 보내기
      ResponseEntity<String> response = restTemplate.exchange(
          // nice api 개발 통합문서랑 다름(통합문서에서는 /digital/niceid/oauth/oauth/token HTTP/1.1 이렇게 되어있음)
          "https://svc.niceapi.co.kr:22001/digital/niceid/oauth/oauth/token",
          HttpMethod.POST,
          request,
          String.class
      );

      // 응답 데이터 파싱
      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        returnData = objectMapper.readTree(response.getBody());
      }
      else {
        returnData = objectMapper.createObjectNode().put("error", "Failed to get access token");
      }
    } catch (Exception e) {
      e.printStackTrace();
      returnData = objectMapper.createObjectNode().put("error", e.getMessage());
    }

    return new ResponseEntity<>(returnData, HttpStatus.OK);
  }

  private String makeAccessTokenAuthorization() {
    String auth = CLIENT_ID + ":" + CLIENT_SECRET;
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
    return "Basic " + encodedAuth;
  }
}

package com.example.demo.controller;//package com.api_nice_gradle.controller;
//
//import com.api_nice_gradle.dto.RequestEncTokenDto;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Base64;
//import java.util.Date;
//import lombok.AllArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//
//@RestController
//@AllArgsConstructor
//@Tag(name = "암호화 토큰 발급(데이터 직접 입력) 컨트롤러", description = "기관 토큰(accessToken) clientId(app > 상세보기) 에 있는 값을 직접 입력해 주세요")
//public class AuthControllerWithPlanData {
//
//
//  // MakeAccessTokenController 에서 발급받은 기관트큰
//  @Value("${nice.accessToken}")
//  private static String ACCESS_TOKEN;
//
//  // 로그인 > 앱목록 > 상세보기 > 인증키 보기의 Client ID
//  @Value("${nice.clientId}")
//  private static String CLIENT_ID;
//
//  //통합형 상품 아이디
//  private final String PRODUCT_ID = "2101979031";
//
//  //
//  private final ObjectMapper objectMapper;
//
//
//  /**
//   * 암호화 토큰 발급 요청 API
//   * * ================= REQUEST ====================================================================
//   * - HEADERS
//   *   Content-type : application/json
//   *   Authorization : bearer Base64Encoding(${access_token}:${current_timestamp}:${client_id})
//   *   ProductID : 2101979031(통합형)
//   *
//   * - BODY (dataHeader, dataBody)
//   * -- dataHeader
//   *    CNTY_CD : ko (한국어), en(영어) 등 확인하기
//   * -- dataBody
//   *    req_dtim : 요청일시, yyyyMMddHHmmss 형식(14)
//   *    req_no : 요청 고유번호, 업체에서 구현(30)
//   *    enc_mode : 사용할 암복호화 구분, 1로 고정
//   *
//   * * ================= RESPONSE ===================================================================
//   *
//   * */
//
//  @PostMapping("/requestEncTokenWithData")
//  public ResponseEntity<JsonNode> requestEncTokenWithData(@RequestBody RequestEncTokenDto dto) {
//    // response 초기화
//    JsonNode returnData = null;
//
//    // authorization 만들기
//    String authorization = makeEncTokenAuthorization(dto);
//
//    // headers(Content-type, Authorization, ProductID) 만들기
//    HttpHeaders headers = makeHttpHeaders(authorization);
//
//    // body(dataHeader(CNTY_CD)) 만들기
//    ObjectNode dataHeader = createDataHeader();
//
//    // body(dataBody(req_dtim, req_no, enc_mode)) 만들기
//    ObjectNode dataBody = createDataBody();
//
//    ObjectNode jsonData = objectMapper.createObjectNode();
//    jsonData.set("dataHeader", dataHeader);
//    jsonData.set("dataBody", dataBody);
//
//    HttpEntity<String> requestEntity = new HttpEntity<>(jsonData.toString(), headers);
//
//    RestTemplate restTemplate = new RestTemplate();
//    try {
//      ResponseEntity<JsonNode> response = restTemplate.exchange(
//          "https://svc.niceapi.co.kr:22001/digital/niceid/api/v1.0/common/crypto/token",
//          HttpMethod.POST,
//          requestEntity,
//          JsonNode.class
//      );
//
//      returnData = response.getBody();
//      return new ResponseEntity<>(returnData, response.getStatusCode());
//    } catch (Exception e) {
//      e.printStackTrace();
//      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//  }
//
//  private HttpHeaders makeHttpHeaders(String authorization) {
//    HttpHeaders headers = new HttpHeaders();
//    headers.add("Content-Type", "application/json");
//    headers.add("Authorization", authorization);
//    headers.add("ProductID", PRODUCT_ID);
//    return headers;
//  }
//
//  private ObjectNode createDataHeader() {
//    ObjectNode dataHeader = objectMapper.createObjectNode();
//    dataHeader.put("CNTY_CD", "ko");
//    return dataHeader;
//  }
//
//  private ObjectNode createDataBody() {
//    ObjectNode dataBody = objectMapper.createObjectNode();
//    String requestTime = makeRequestTime();
//    String requestNumber = makeRequestNumber(requestTime);
//    dataBody.put("req_dtim", requestTime);
//    dataBody.put("req_no", requestNumber);
//    dataBody.put("enc_mode", "1");
//    return dataBody;
//  }
//
//  // 13자리 랜덤 문자열 생성 메서드
//  private static String makeRequestNumber(String requestTime) {
//    StringBuilder sb = new StringBuilder(13);
//
//    for (int i = 0; i < 13; i++) {
//      int digit = (int)(Math.random() * 10); // 0에서 9 사이의 숫자 생성
//      sb.append(digit);
//    }
//    String randomNum = sb.toString();
//    String requestNumber = "REQ" + requestTime + randomNum;
//
//    return requestNumber;
//  }
//
//  private String makeRequestTime() {
//    LocalDateTime currentDateTime = LocalDateTime.now();
//    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//    String requestTime = currentDateTime.format(formatter);
//    return requestTime;
//  }
//
//
//  private String makeEncTokenAuthorization(RequestEncTokenDto dto) {
//    Date date = new Date();
//    long CURRENT_TIMESTAMP = date.getTime()/1000;
//    String auth = dto.getAccessToken() + ":" + CURRENT_TIMESTAMP + ":" + dto.getClientId();
//    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
//    return "bearer " + encodedAuth;
//  }
//
//
//}

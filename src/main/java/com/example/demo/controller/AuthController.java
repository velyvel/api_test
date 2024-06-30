package com.example.demo.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@AllArgsConstructor
@Tag(name = "암호화 토큰 발급 컨트롤러", description = "기관 토큰(accessToken) clientId(app > 상세보기) 에 있는 값을 지정해 주세요")
public class AuthController {


  // MakeAccessTokenController 에서 발급받은 기관트큰
  @Value("${nice.accessToken}")
  private static String ACCESS_TOKEN;

  // 로그인 > 앱목록 > 상세보기 > 인증키 보기의 Client ID
  @Value("${nice.clientId}")
  private static String CLIENT_ID;

  //통합형 상품 아이디
  private final String PRODUCT_ID = "2101979031";
  private final ObjectMapper objectMapper;

  private static String REQ_DTIM = "";
  private static String REQ_NO = "";
  private static String SITE_CODE = "";
  private static String TOKEN_VERSION_ID = "";


  /**
   * 암호화 토큰 발급 요청 API </br>
   * * ================= REQUEST ====================================================================</br>
   * - HEADERS</br>
   *   Content-type : application/json</br>
   *   Authorization : bearer Base64Encoding(${access_token}:${current_timestamp}:${client_id})</br>
   *   ProductID : 2101979031(통합형)</br></br>
   *
   * - BODY (dataHeader, dataBody)</br>
   * -- dataHeader</br>
   *    CNTY_CD : ko (한국어), en(영어) 등 확인하기</br>
   * -- dataBody</br>
   *    req_dtim : 요청일시, yyyyMMddHHmmss 형식(14)</br>
   *    req_no : 요청 고유번호, 업체에서 구현(30)</br>
   *    enc_mode : 사용할 암복호화 구분, 1로 고정</br></br>
   *
   * * ================= RESPONSE ===================================================================</br>
   * - BODY </br>
   * -- dataHeader</br>
   *    GW_RSLT_CD : 응답 코드 : 1200 응답</br>
   *    GW_RSLT_MSG : 응답 메세지</br></br>
   *
   * -- dataBody : * 처리 된 (site_code, token_version_id, token_val)은 이후 표준창 호출하는 부분에서 필요합니다. </br>
   *    rsp_cd : dataBody 정상 처리 여부, POOOO 성공</br>
   *    res_msg : 오류 처리 메세지(업체 구현)</br>
   *    result_cd : rep_cd 가 P0000 일 때 상세결과 코드</br>
   *    * site_code : 사이트 코드</br>
   *    * token_version_id : 서버 토큰 버전</br>
   *    * token_val : 암-복호화를 위한 서버 토큰 값</br>
   *    period : 토큰 만료 시간(초)
   * */

  @PostMapping("/requestEncToken")
  public ResponseEntity<RequestWindowDto> requestEncToken() {

    // 실제로 API 요청하는 로직
    try {
      JsonNode response = executeApiRequest();
      TOKEN_VERSION_ID = extractValue(response, "token_version_id");
      SITE_CODE = extractValue(response, "site_code");
      String token_val = extractValue(response, "token_val");

      String original = REQ_DTIM.trim() + REQ_NO.trim() + token_val.trim();

      // 4. 대칭키 생성 original 을 기반으로 암호화 된 데이터 만들기
      String resultVal = makeResultValue(original);

      // 암호화 된 데이터(resultVal) 기반으로 key, iv, hmac_key 만들기
      String key = resultVal.substring(0, 16); // 앞에서 부터 16byte
      String iv = resultVal.substring(resultVal.length() - 16); // 뒤에서 부터 16byte
      String hmac_key = resultVal.substring(0, 32); // 무결성 키 생성 : 앞에서 부터 32 byte

      // 5. 요청 데이터 암호화 (필수값이 아닌 것은 주석처리)
      String encryptedData = encryptRequestData(key, iv);
      String totalResult = calculateHmac(hmac_key, encryptedData);

      // 6. 표준창 호출하기 위한 데이터 담아두기
      RequestWindowDto dto = makeOpenWindowData(totalResult, encryptedData);
      return new ResponseEntity<>(dto, HttpStatus.OK);
    } catch (Exception e) {
      e.printStackTrace();
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/requestWindow")
  public ResponseEntity<JsonNode> requestWindow(@RequestBody RequestWindowDto dto) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("m", "service");
      map.add("token_version_id", dto.getTokenVersionId());
      map.add("enc_data", dto.getEncData());
      map.add("integrity_value", dto.getTotalResult());

      HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<String> returnData = restTemplate.exchange(
          "https://nice.checkplus.co.kr/CheckPlusSafeModel/service.cb",
          HttpMethod.POST,
          requestEntity,
          String.class
      );

      // 응답을 String으로 받아서 JsonNode로 변환
      JsonNode responseNode = objectMapper.readTree(returnData.getBody());

      return new ResponseEntity<>(responseNode, HttpStatus.OK);
    } catch (Exception e) {
      e.printStackTrace();
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private JsonNode executeApiRequest() {
    String authorization = makeEncTokenAuthorization();
    HttpHeaders headers = makeHttpHeaders(authorization);
    ObjectNode dataHeader = createDataHeader();
    ObjectNode dataBody = createDataBody();

    ObjectNode jsonData = objectMapper.createObjectNode();
    jsonData.set("dataHeader", dataHeader);
    jsonData.set("dataBody", dataBody);

    HttpEntity<String> requestEntity = new HttpEntity<>(jsonData.toString(), headers);
    RestTemplate restTemplate = new RestTemplate();

    ResponseEntity<JsonNode> returnData = restTemplate.exchange(
        "https://svc.niceapi.co.kr:22001/digital/niceid/api/v1.0/common/crypto/token",
        HttpMethod.POST,
        requestEntity,
        JsonNode.class
    );

    return returnData.getBody();
  }

  private HttpHeaders makeHttpHeaders(String authorization) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("Authorization", authorization);
    headers.add("ProductID", PRODUCT_ID);
    return headers;
  }

  private ObjectNode createDataHeader() {
    ObjectNode dataHeader = objectMapper.createObjectNode();
    dataHeader.put("CNTY_CD", "ko");
    return dataHeader;
  }

  private ObjectNode createDataBody() {
    ObjectNode dataBody = objectMapper.createObjectNode();
    REQ_DTIM = makeRequestTime();
    REQ_NO = makeRequestNumber(REQ_DTIM);
    dataBody.put("req_dtim", REQ_DTIM);
    dataBody.put("req_no", REQ_NO);
    dataBody.put("enc_mode", "1");
    return dataBody;
  }

  // 13자리 랜덤 문자열 생성 메서드
  private static String makeRequestNumber(String requestTime) {
    StringBuilder sb = new StringBuilder(13);

    for (int i = 0; i < 13; i++) {
      int digit = (int)(Math.random() * 10); // 0에서 9 사이의 숫자 생성
      sb.append(digit);
    }
    String randomNum = sb.toString();
    String requestNumber = "REQ" + requestTime + randomNum;

    return requestNumber;
  }

  private String makeRequestTime() {
    LocalDateTime currentDateTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    String requestTime = currentDateTime.format(formatter);
    return requestTime;
  }


  private String makeEncTokenAuthorization() {
    Date date = new Date();
    long CURRENT_TIMESTAMP = date.getTime()/1000;
    String auth = ACCESS_TOKEN + ":" + CURRENT_TIMESTAMP + ":" + CLIENT_ID;
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
    return "bearer " + encodedAuth;
  }

  private String extractValue(JsonNode response, String fieldName) {
    return response.path(fieldName).asText();
  }

  private String makeResultValue(String original) throws Exception{
    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
    messageDigest.update(original.getBytes());
    byte[] encodedOriginal = messageDigest.digest();
    String resultValue = Base64.getEncoder().encodeToString(encodedOriginal);

    return resultValue;
  }

  private String encryptRequestData(String key, String iv) throws Exception {
    ObjectNode requestData = objectMapper.createObjectNode();
    requestData.put("requestno", REQ_NO);
    requestData.put("returnurl", "http://localhost:8080/returnData");
    requestData.put("sitecode", SITE_CODE);
    requestData.put("methodtype", "get");
    /**
     * requestData.put("authtype", "M"); // (필수 아님) 인증수단 고정 (M:휴대폰인증,C:카드본인확인인증,X:인증서인증,U:공동인증서인증,F:금융인증서인증,S:PASS인증서인증)
     * requestData.put("mobilceco", "S"); // (필수 아님) 이통사 우선 선택(S : SKT, K : KT, L : LGU+)
     * requestData.put("businessno", ""); // (필수 아님) 사업자번호(법인인증인증에 한함)
     * requestData.put("popupyn", "Y"); // (필수 아님) 고정값 Y, N
     * requestData.put("receivedata", ""); // 인증 후 전달받을 데이터 세팅 (요청값 그대로 리턴)
     */

    String createRequestData = requestData.toString();

    SecretKey secretKey = new SecretKeySpec(key.getBytes(),"AES");
    Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
    c.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv.getBytes()));
    byte[] encrypted = c.doFinal(createRequestData.trim().getBytes());
    return Base64.getEncoder().encodeToString(encrypted);
  }

  private String calculateHmac(String hmacKey, String data) throws Exception {
    byte[] encodingByte = hmac256(hmacKey.getBytes(), data.getBytes());
    return Base64.getEncoder().encodeToString(encodingByte);
  }

  public byte[] hmac256(byte[] secretKey, byte[] message) {
    byte[] hmac256 = null;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec sks = new SecretKeySpec(secretKey, "HmacSHA256");
      mac.init(sks);
      hmac256 = mac.doFinal(message);
      return hmac256;
    } catch (Exception e) {
      throw new RuntimeException("HMACSHA256 암호화에 실패햐였습니다.");
    }
  }

  private RequestWindowDto makeOpenWindowData(String totalResult, String encData) {
    RequestWindowDto dto = new RequestWindowDto();
    dto.setEncData(encData);
    dto.setTokenVersionId(TOKEN_VERSION_ID);
    dto.setTotalResult(totalResult);
    return dto;
  }

}

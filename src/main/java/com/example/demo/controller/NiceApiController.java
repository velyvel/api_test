package com.example.demo.controller;

import com.example.demo.dto.EncDataDto;
import com.example.demo.service.NiceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.json.simple.JSONObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.beans.Encoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@AllArgsConstructor
public class NiceApiController {
    private final NiceService niceService;

    private final String CLIENT_ID = "";
    private final String CLIENT_SECRET = "";
    // 통합형의 경우
    private final String PRODUCT_ID = "2101979031";

    // 그래서 accessToken을 발급받았다
    private final String ACCESS_TOKEN = "71c469e0-235d-480a-b16e-3cefdf8b6da1";

    /*
     * 50년 기관토큰 발급, POST : /digital/niceid/oauth/oauth/token
     * 필요한 데이터 : Authorization
     * Authorization 은 "Basic  " + Base64Encoding(client_id:client_secret) 으로 만든다
     * client_id, client secret 은 앱등록 > 상세보기 > 인증키 조회 버튼에서 확인 가능
     */
    @GetMapping("/makeAccessToken")
    public String makeAccessToken(Model model){
        // Base64 인코딩
        String text = CLIENT_ID + ":" + CLIENT_SECRET;
        byte[] bytes = text.getBytes();
        String data = Base64.getEncoder().encodeToString(bytes);
        //공백이 중요
        String authorization = "Basic" + " " + data;
        System.out.println(">>>>>>>>" + authorization);
        model.addAttribute("authorization", authorization);

        return "makeAccessToken";
    }

    @PostMapping("/accessTokenResult")
    public ResponseEntity<JsonNode> accessTokenResult(@RequestParam("authorization") String authorization) {
        JsonNode returnData = null;

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
            } else {
                returnData = objectMapper.createObjectNode().put("error", "Failed to get access token");
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnData = objectMapper.createObjectNode().put("error", e.getMessage());
        }

        return new ResponseEntity<>(returnData, HttpStatus.OK);
    }
    @GetMapping("/makeEncData")
    public String makeEncData(Model model){
        // req_dtim 변수 만들기 : 형식은 yyyyMMddHHmmss
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String req_dtim = dateFormat.format(new Date());

        // 고유 요청번호 최대길이 30
        String req_no = "REQ" + req_dtim + Double.toString(Math.random()).substring(2, 6);

        // enc_mode 는 1로 고정
        String enc_mode = "1";
        model.addAttribute("req_dtim", req_dtim);
        model.addAttribute("req_no", req_no);
        model.addAttribute("enc_mode", enc_mode);
        return "makeEncData";

    }

    //암호화 토큰 확인
    @PostMapping("/encToken")
    public ResponseEntity<JsonNode> encToken(@RequestBody EncDataDto dto) {
        JsonNode returnData = null;
        ObjectMapper objectMapper = new ObjectMapper();


        // header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("CNTY_CD", "ko");

        // Body 데이터 설정 : req_dtim, req_no, enc_mode
        String body = String.format("{\"req_dtim\":\"%s\", \"req_no\":\"%s\", \"enc_mode\":\"%s\"}", dto.getReq_dtim(), dto.getReq_no(), dto.getEnc_mode());

        // HttpEntity 생성
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            // POST 요청 보내기
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://svc.niceapi.co.kr:22001/digital/niceid/api/v1.0/common/crypto/token",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            // 응답 데이터 파싱
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                returnData = objectMapper.readTree(response.getBody());
            } else {
                returnData = objectMapper.createObjectNode().put("error", "Failed to get encrypted token");
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnData = objectMapper.createObjectNode().put("error", e.getMessage());
        }

        return new ResponseEntity<>(returnData, HttpStatus.OK);
    }

    /* 대칭키 생성 key :데이터암호화할 대칭키(앞에서 16바이트), iv : 데이터암호화할 initial Vector(뒤에서 16바이트)
      * hmac_key : 암호화 값 위변조 체크용
      */
    @GetMapping("/makeSymmetricKey")
    public String makeSymmetricKey(Model model) {
        //Body에 떨어진 데이터 읽기
        String key = "";
        String iv = "";
        String hmac_key = "";

        model.addAttribute("key", key);
        model.addAttribute("iv", iv);
        model.addAttribute("hmac_key", hmac_key);

        return "/makeSymmetricKey";
    }


}

package com.example.demo.controller;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Hello, Spring Boot with JSP!");
        return "index";
    }

    @GetMapping("/main")
    public String checkMain(Model model) {

    String access_token="12313213123132"; //기관토큰(access_token)은 반영구적으로 사용가능하며, 한번 발급 후 50년 유효합니다.
    String client_id = "2222222";
    String productID = "2222222222";

    String returnURL = "";
    //URL의 경우 프로토콜(http/https)부터 사용바랍니다. 다를 경우 CORS 오류가 발생 할 수 있습니다.
    //예) http://localhost/checkplus_success.jsp

    SimpleDateFormat TestDate = new SimpleDateFormat("yyyyMMddhhmmss");

    String req_dtim = TestDate.format(new Date());
    String req_no="REQ"+req_dtim+Double.toString(Math.random()).substring(2,6);
    //요청고유번호(req_no)의 경우 업체 정책에 따라 거래 고유번호 설정 후 사용하면 됩니다.
    //제공된 값은 예시입니다.

    Date currentDate = new Date();
    long current_timestamp = currentDate.getTime() /1000;

    String Auth = Base64.getEncoder().encodeToString((access_token+":"+current_timestamp+":"+client_id).getBytes());

    String tURL = "https://svc.niceapi.co.kr:22001/digital/niceid/api/v1.0/common/crypto/token";

    String uParam="{\"dataHeader\":{\"CNTY_CD\":\"kr\"},"
            + "\"dataBody\":{\"req_dtim\":\""+req_dtim+"\","
            +"\"req_no\":\""+req_no+"\","
            +"\"enc_mode\":\"1\""
            + "}}";

    String responseData = test(tURL, uParam, Auth, productID);


    String token_version_id = "123412341";
    String sitecode = "Z9999";
    String token_val = "123123132";

    try{
        JSONParser jsonParse = new JSONParser();
        JSONObject jsonObj = (JSONObject) jsonParse.parse(responseData);

        JSONObject dataBody = (JSONObject) jsonParse.parse(jsonObj.get("dataBody").toString());

//        token_version_id = dataBody.get("token_version_id").toString();
//        sitecode = dataBody.get("site_code").toString();
//        token_val = dataBody.get("token_val").toString();

    }catch (ParseException e){
        e.printStackTrace();
    }

    String result = req_dtim.trim()+req_no.trim()+token_val.trim();

        String resultVal = null;
        try {
            resultVal = encryptSHA256(result);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String key =resultVal.substring(0,16);
    String iv =resultVal.substring(resultVal.length()-16);
    String hmac_key =resultVal.substring(0,32);

    String plain ="{"
            +"\"requestno\":\""+req_no+"\","
            +"\"returnurl\":\""+returnURL+"\","
            +"\"sitecode\":\""+sitecode+"\""
            +"}";

        String enc_data = null;
        try {
            enc_data = encryptAES(plain, key, iv);
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }

        byte[] hmacSha256 = null;
        try {
            hmacSha256 = hmac256(hmac_key.getBytes(), enc_data.getBytes());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        String integrity = Base64.getEncoder().encodeToString(hmacSha256);

     /*
      * 인증 완료 후 success페이지에서 사용을 위한 key값은 DB,세션등 업체 정책에 맞춰 관리 후 사용하면 됩니다.
      * 예시에서 사용하는 방법은 세션이며, 세션을 사용할 경우 반드시 인증 완료 후 세션이 유실되지 않고 유지되도록 확인 바랍니다.
      * key, iv, hmac_key 값들은 token_version_id에 따라 동일하게 생성되는 고유값입니다.
      * success페이지에서 token_version_id가 일치하는지 확인 바랍니다.
      */
//    session.setAttribute("req_no", req_no);
//    session.setAttribute("key" , key);
//    session.setAttribute("iv" , iv);
//    session.setAttribute("hmac_key" , hmac_key);
//    session.setAttribute("token_version_id", token_version_id);

        model.addAttribute("req_no", req_no);
        model.addAttribute("key" , key);
        model.addAttribute("iv" , iv);
        model.addAttribute("hmac_key" , hmac_key);
        model.addAttribute("token_version_id", token_version_id);

        return "main";
    }

    @GetMapping("/success")
    public String success(Model model) {
        return "success";
    }

    public static String test(String targetURL, String parameters , String Auth, String productID) {
        HttpURLConnection connection = null;
        try{
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.setRequestProperty("Authorization","bearer "+Auth);
            connection.setRequestProperty("productID", productID);
            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream (connection.getOutputStream());

            wr.writeBytes(parameters);
            wr.close();
            InputStream is = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(is, "utf-8"));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    //대칭키 생성을 위한 함수
    public static String encryptSHA256(String result)throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(result.getBytes());
        byte[] arrHashValue = md.digest();
        String resultVal = Base64.getEncoder().encodeToString(arrHashValue);

        return resultVal;
    }

    //암호화를 위한 함수
    public static String encryptAES(String reqData, String key, String iv)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        SecretKey secureKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher c = null;
        try {
            c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(iv.getBytes()));
        byte[] encrypted = c.doFinal(reqData.trim().getBytes());
        String reqDataEnc =Base64.getEncoder().encodeToString(encrypted);

        return reqDataEnc;
    }

    //무결성값 생성을 위한 함수
    public static byte[] hmac256(byte[] secretKey,byte[] message)
            throws NoSuchAlgorithmException, InvalidKeyException{
        byte[] hmac256 = null;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec sks = new SecretKeySpec(secretKey, "HmacSHA256");
        mac.init(sks);
        hmac256 = mac.doFinal(message);

        return hmac256;
    }

//%>
//
//
//<%
//    String access_token=""; //기관토큰(access_token)은 반영구적으로 사용가능하며, 한번 발급 후 50년 유효합니다.
//    String client_id = "";
//    String productID = "";
//
//    String returnURL = "";
//    //URL의 경우 프로토콜(http/https)부터 사용바랍니다. 다를 경우 CORS 오류가 발생 할 수 있습니다.
//    //예) http://localhost/checkplus_success.jsp
//
//    SimpleDateFormat TestDate = new SimpleDateFormat("yyyyMMddhhmmss");
//
//    String req_dtim = TestDate.format(new Date());
//    String req_no="REQ"+req_dtim+Double.toString(Math.random()).substring(2,6);
//    //요청고유번호(req_no)의 경우 업체 정책에 따라 거래 고유번호 설정 후 사용하면 됩니다.
//    //제공된 값은 예시입니다.
//
//    Date currentDate = new Date();
//    long current_timestamp = currentDate.getTime() /1000;
//
//    String Auth = Base64.getEncoder().encodeToString((access_token+":"+current_timestamp+":"+client_id).getBytes());
//
//    String tURL = "https://svc.niceapi.co.kr:22001/digital/niceid/api/v1.0/common/crypto/token";
//
//    String uParam="{\"dataHeader\":{\"CNTY_CD\":\"kr\"},"
//            + "\"dataBody\":{\"req_dtim\":\""+req_dtim+"\","
//            +"\"req_no\":\""+req_no+"\","
//            +"\"enc_mode\":\"1\""
//            + "}}";
//
//    String responseData = testHttpRequest(tURL, uParam, Auth, productID);
//
//
//    String token_version_id = "";
//    String sitecode = "";
//    String token_val = "";
//
//    try{
//        JSONParser jsonParse = new JSONParser();
//        JSONObject jsonObj = (JSONObject) jsonParse.parse(responseData);
//
//        JSONObject dataBody = (JSONObject) jsonParse.parse(jsonObj.get("dataBody").toString());
//
//        token_version_id = dataBody.get("token_version_id").toString();
//        sitecode = dataBody.get("site_code").toString();
//        token_val = dataBody.get("token_val").toString();
//
//    }catch (ParseException e){
//        e.printStackTrace();
//    }
//
//    String result = req_dtim.trim()+req_no.trim()+token_val.trim();
//
//    String resultVal = encryptSHA256(result);
//
//    String key =resultVal.substring(0,16);
//    String iv =resultVal.substring(resultVal.length()-16);
//    String hmac_key =resultVal.substring(0,32);
//
//    String plain ="{"
//            +"\"requestno\":\""+req_no+"\","
//            +"\"returnurl\":\""+returnURL+"\","
//            +"\"sitecode\":\""+sitecode+"\""
//            +"}";
//
//    String enc_data = encryptAES(plain, key, iv);
//
//    byte[] hmacSha256 = hmac256(hmac_key.getBytes(), enc_data.getBytes());
//    String integrity = Base64.getEncoder().encodeToString(hmacSha256);
//
//    // 인증 완료 후 success페이지에서 사용을 위한 key값은 DB,세션등 업체 정책에 맞춰 관리 후 사용하면 됩니다.
//    // 예시에서 사용하는 방법은 세션이며, 세션을 사용할 경우 반드시 인증 완료 후 세션이 유실되지 않고 유지되도록 확인 바랍니다.
//    // key, iv, hmac_key 값들은 token_version_id에 따라 동일하게 생성되는 고유값입니다.
//    // success페이지에서 token_version_id가 일치하는지 확인 바랍니다.
//    session.setAttribute("req_no", req_no);
//    session.setAttribute("key" , key);
//    session.setAttribute("iv" , iv);
//    session.setAttribute("hmac_key" , hmac_key);
//    session.setAttribute("token_version_id", token_version_id);
//
//%>

}

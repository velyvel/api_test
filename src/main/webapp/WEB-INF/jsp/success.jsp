<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import = "javax.crypto.Cipher" %>
<%@ page import = "javax.crypto.Mac" %>
<%@ page import = "javax.crypto.SecretKey" %>
<%@ page import = "javax.crypto.spec.IvParameterSpec" %>
<%@ page import = "javax.crypto.spec.SecretKeySpec" %>

<%@ page import = "java.security.InvalidKeyException" %>
<%@ page import = "java.security.NoSuchAlgorithmException" %>
<%@ page import = "java.util.Base64" %>
<%@ page import = "java.util.Base64.Encoder" %>
<%@ page import = "java.util.Base64.Decoder" %>
<%@ page import = "java.net.URLDecoder" %>

<%@ page import = "org.json.simple.JSONArray" %>
<%@ page import = "org.json.simple.JSONObject" %>
<%@ page import = "org.json.simple.parser.JSONParser" %>
<%@ page import = "org.json.simple.parser.ParseException" %>


<%!
    //복호화를 위한 함수
    public static String getAesDecDataPKCS5(byte[] key, byte[] iv, String base64Enc) throws Exception {
        SecretKey secureKey = new SecretKeySpec(key, "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(iv));
        byte[] cipherEnc = Base64.getDecoder().decode(base64Enc);
                
        String Dec = new String(c.doFinal(cipherEnc), "utf-8");
                
        return Dec;
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

%>


<%

    String req_no = (String)session.getAttribute("req_no");
    String key = (String)session.getAttribute("key");
    String iv = (String)session.getAttribute("iv");
    String hmac_key = (String)session.getAttribute("hmac_key");
    String s_token_version_id = (String)session.getAttribute("token_version_id");

    String enc_data = request.getParameter("enc_data");
    String token_version_id = request.getParameter("token_version_id");
    String integrity_value = request.getParameter("integrity_value");

    String enctime ="";
    String requestno ="";
    String responseno ="";
    String authtype ="";
    String name ="";
    String birthdate = "";
    String gender ="";
    String nationalinfo="";
    String ci ="";
    String di ="";
    String mobileno ="";
    String mobileco ="";

    String sMessage ="";
            
    byte[] hmacSha256 = hmac256(hmac_key.getBytes(), enc_data.getBytes());
    String integrity = Base64.getEncoder().encodeToString(hmacSha256);
    
    
    if (!integrity.equals(integrity_value)){
        sMessage = "무결성 값이 다릅니다. 데이터가 변경된 것이 아닌지 확인 바랍니다.";
    }else{
        String dec_data = getAesDecDataPKCS5(key.getBytes(), iv.getBytes(), enc_data);
        
        JSONParser jsonParse = new JSONParser();
        JSONObject plain_data = (JSONObject) jsonParse.parse(dec_data);
        
        if (!req_no.equals(plain_data.get("requestno").toString())){
            sMessage = "세션값이 다릅니다. 올바른 경로로 접근하시기 바랍니다.";
        }else{
            sMessage = "복호화 성공";
            
            enctime =plain_data.get("enctime").toString();
            requestno =plain_data.get("requestno").toString();
            responseno =plain_data.get("responseno").toString();
            authtype =plain_data.get("authtype").toString();
            name = URLDecoder.decode(plain_data.get("utf8_name").toString(), "UTF-8");
            birthdate = plain_data.get("birthdate").toString();
            gender =plain_data.get("gender").toString();
            nationalinfo=plain_data.get("nationalinfo").toString();
            ci =plain_data.get("ci").toString();
            di =plain_data.get("di").toString();
            mobileno =plain_data.get("mobileno").toString();
            mobileco =plain_data.get("mobileco").toString();
        }
    }
        
%>



<html>
<head>
    <title>NICE평가정보 - CheckPlus 본인인증 테스트</title>
</head>
<body>
    <center>
    <p><p><p><p>
    본인인증이 완료 되었습니다.<br>
    Message : <%= sMessage %>
    <table border=1>
        <tr>
            <td>암호화 시간</td>
            <td><%= enctime %> (YYMMDDHHMMSS)</td>
        </tr>
        <tr>
            <td>요청 번호</td>
            <td><%= requestno %></td>
        </tr>            
        <tr>
            <td>나신평응답 번호</td>
            <td><%= responseno %></td>
        </tr>            
        <tr>
            <td>인증수단</td>
            <td><%= authtype %></td>
        </tr>
                <tr>
            <td>성명</td>
            <td><%= name %></td>
        </tr>
                <tr>
            <td>생년월일(YYYYMMDD)</td>
            <td><%= birthdate %></td>
        </tr>
                <tr>
            <td>성별</td>
            <td><%= gender %></td>
        </tr>
                <tr>
            <td>내/외국인정보</td>
            <td><%= nationalinfo %></td>
        </tr>
                <tr>
            <td>DI(64 byte)</td>
            <td><%= di %></td>
        </tr>
                <tr>
            <td>CI(88 byte)</td>
            <td><%= ci %></td>
        </tr>
        <tr>
            <td>휴대폰번호</td>
            <td><%= mobileno %></td>
        </tr>
        <tr>
            <td>통신사</td>
            <td><%= mobileco %></td>
        </tr>
        <tr>
            <td colspan="2">인증 후 결과값은 내부 설정에 따른 값만 리턴받으실 수 있습니다. <br>
            일부 결과값이 null로 리턴되는 경우 관리담당자 또는 계약부서(02-2122-4615)로 문의바랍니다.</td>
        </tr>
    </table>
    </center>
</body>
</html>
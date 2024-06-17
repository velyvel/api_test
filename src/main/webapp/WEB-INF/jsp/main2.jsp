<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import = "java.util.Base64" %>
<%@ page import = "java.util.Date" %>

<%@ page import = "java.net.HttpURLConnection" %>
<%@ page import = "java.net.URL" %>
<%@ page import = "java.io.DataOutputStream" %>
<%@ page import = "java.io.InputStream" %>
<%@ page import = "java.io.InputStreamReader" %>
<%@ page import = "java.io.BufferedReader" %>

<%@  page import = "java.security.InvalidAlgorithmParameterException" %>
<%@  page import = "java.security.InvalidKeyException" %>
<%@  page import = "java.security.MessageDigest" %>
<%@  page import = "java.security.NoSuchAlgorithmException" %>
<%@  page import = "java.util.Base64" %>
<%@  page import = "java.util.Base64.Encoder" %>

<%@  page import = "javax.crypto.BadPaddingException" %>
<%@  page import = "javax.crypto.Cipher" %>
<%@  page import = "javax.crypto.IllegalBlockSizeException" %>
<%@  page import = "javax.crypto.Mac" %>
<%@  page import = "javax.crypto.NoSuchPaddingException" %>
<%@  page import = "javax.crypto.SecretKey" %>
<%@  page import = "javax.crypto.spec.IvParameterSpec" %>
<%@  page import = "javax.crypto.spec.SecretKeySpec" %>

<%@ page import = "org.json.simple.JSONArray" %>
<%@ page import = "org.json.simple.JSONObject" %>
<%@ page import = "org.json.simple.parser.JSONParser" %>
<%@ page import = "org.json.simple.parser.ParseException" %>

<%@ page import = "java.text.SimpleDateFormat" %>


<%!

    //http 통신을 위한 함수
    public static String testHttpRequest(String targetURL, String parameters , String Auth, String productID) {
        HttpURLConnection connection = null;

        try {
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
    public static String encryptSHA256(String result)throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(result.getBytes());
        byte[] arrHashValue = md.digest();
        String resultVal = Base64.getEncoder().encodeToString(arrHashValue);

        return resultVal;
    }

    //암호화를 위한 함수
    public static String encryptAES(String reqData, String key, String iv)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
        SecretKey secureKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
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

%>


<%
    String access_token=""; //기관토큰(access_token)은 반영구적으로 사용가능하며, 한번 발급 후 50년 유효합니다.
    String client_id = "";
    String productID = "";

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

    String responseData = testHttpRequest(tURL, uParam, Auth, productID);


    String token_version_id = "";
    String sitecode = "";
    String token_val = "";

    try{
        JSONParser jsonParse = new JSONParser();
        JSONObject jsonObj = (JSONObject) jsonParse.parse(responseData);

        JSONObject dataBody = (JSONObject) jsonParse.parse(jsonObj.get("dataBody").toString());

        token_version_id = dataBody.get("token_version_id").toString();
        sitecode = dataBody.get("site_code").toString();
        token_val = dataBody.get("token_val").toString();

    }catch (ParseException e){
        e.printStackTrace();
    }

    String result = req_dtim.trim()+req_no.trim()+token_val.trim();

    String resultVal = encryptSHA256(result);

    String key =resultVal.substring(0,16);
    String iv =resultVal.substring(resultVal.length()-16);
    String hmac_key =resultVal.substring(0,32);

    String plain ="{"
    +"\"requestno\":\""+req_no+"\","
    +"\"returnurl\":\""+returnURL+"\","
    +"\"sitecode\":\""+sitecode+"\""
    +"}";

    String enc_data = encryptAES(plain, key, iv);

    byte[] hmacSha256 = hmac256(hmac_key.getBytes(), enc_data.getBytes());
    String integrity = Base64.getEncoder().encodeToString(hmacSha256);

    // 인증 완료 후 success페이지에서 사용을 위한 key값은 DB,세션등 업체 정책에 맞춰 관리 후 사용하면 됩니다.
    // 예시에서 사용하는 방법은 세션이며, 세션을 사용할 경우 반드시 인증 완료 후 세션이 유실되지 않고 유지되도록 확인 바랍니다.
    // key, iv, hmac_key 값들은 token_version_id에 따라 동일하게 생성되는 고유값입니다.
    // success페이지에서 token_version_id가 일치하는지 확인 바랍니다.
    session.setAttribute("req_no", req_no);
    session.setAttribute("key" , key);
    session.setAttribute("iv" , iv);
    session.setAttribute("hmac_key" , hmac_key);
    session.setAttribute("token_version_id", token_version_id);

%>




<html>
<head>
    <title>본인인증 테스트</title>

    <script language='javascript'>
        window.name="Parent_window";

        function fnPopup(){
            window.open('', 'popupChk', 'width=480, height=812, top=100, fullscreen=no, menubar=no, status=no, toolbar=no,titlebar=yes, location=no, scrollbar=no');
            document.form_chk.action = "https://nice.checkplus.co.kr/CheckPlusSafeModel/checkplus.cb";
            document.form_chk.target = "popupChk";
            document.form_chk.submit();
        }
    </script>

    <style type="text/css">td{border: 1px solid #ccc}</style>
</head>
<body>
    <table style="width:100%; border:1px solid #ccc">
        <tbody>
            <tr>
                <td style="width:200px">req_no</td>
                <td><%=req_no%></td>
            </tr>
            <tr>
                <td>req_dtim</td>
                <td><%=req_dtim%></td>
            </tr>
            <tr>
                <td>token_value</td>
                <td><%=token_val%></td>
            </tr>
            <tr>
                <td>token version id</td>
                <td><%=token_version_id %></td>
            </tr>
            <tr>
                <td>result value</td>
                <td><%=resultVal %></td>
            </tr>
            <tr>
                <td>key</td>
                <td><%=key %></td>
            </tr>
            <tr>
                <td>iv</td>
                <td><%=iv %></td>
            </tr>
            <tr>
                <td>hmac_key</td>
                <td><%=hmac_key%></td>
            </tr>
            <tr>
                <td>jsondata</td>
                <td><%=plain%></td>
            </tr>
            <tr>
                <td>enc_data</td>
                <td><%=enc_data %></td>
            </tr>
            <tr>
                <td>integrity_value</td>
                <td><%=integrity %></td>
            </tr>
        </tboty>
    </table>

    <!--본인인증 서비스 팝업을 호출하기 위해서는 다음과 같은 form이 필요합니다 -->

    <form name="form_chk" method="post">
        <input type="hidden" name="m" value="service"/>
        <input type="hidden" name="token_version_id" value="<%=token_version_id%>"/>
        <input type="hidden" name="enc_data" value="<%=enc_data%>"/>
        <input type="hidden" name="integrity_value" value="<%=integrity%>"/>
        <a href="javascript:fnPopup();"> CheckPlus 안심본인인증 Click</a>
    </form>
</body>
</html>

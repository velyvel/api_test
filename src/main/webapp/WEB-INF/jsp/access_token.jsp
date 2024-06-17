<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import = "java.util.Base64" %>
<%@ page import = "java.net.HttpURLConnection" %>
<%@ page import = "java.net.URL" %>
<%@ page import = "java.io.DataOutputStream" %>
<%@ page import = "java.io.InputStream" %>
<%@ page import = "java.io.InputStreamReader" %>
<%@ page import = "java.io.BufferedReader" %>

<%@ page import = "org.json.simple.JSONArray" %>
<%@ page import = "org.json.simple.JSONObject" %>
<%@ page import = "org.json.simple.parser.JSONParser" %>
<%@ page import = "org.json.simple.parser.ParseException" %>

<%!
    //http 통신을 위한 함수
    public static String testHttpRequest(String targetURL, String parameters , String Auth) {
    HttpURLConnection connection = null;
    
        try {
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST"); 
            connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            connection.setRequestProperty("Authorization","Basic "+Auth);
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

%>

<%

    String tURL = "https://svc.niceapi.co.kr:22001/digital/niceid/oauth/oauth/token";
    String uParam="grant_type=client_credentials&scope=default";
    
    String clientID="";
    String secretKey="";
    
    String Auth = Base64.getEncoder().encodeToString((clientID+":"+secretKey).getBytes());
    String responseData = testHttpRequest(tURL, uParam,Auth);
    String access_token="";
    
    try{
        JSONParser jsonParse = new JSONParser();
        JSONObject jsonObj = (JSONObject) jsonParse.parse(responseData);
        JSONObject dataBody = (JSONObject) jsonParse.parse(jsonObj.get("dataBody").toString());
        
        access_token= dataBody.get("access_token").toString();
                
    }catch (ParseException e){
        e.printStackTrace();
    }
    
%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=EUC-KR">
<title>access_token</title>
</head>
<body>
    access_token : <%=access_token %>
    
</body>
</html>
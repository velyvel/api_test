<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<html>
<head>
    <title>본인인증 테스트</title>

   <script language='javascript'>
           function fnPopup() {
               document.forms["accessToken"].submit();
           }
       </script>

    <style type="text/css">td{border: 1px solid #ccc}</style>
</head>
<body>

<h2>AccessToken 발급하기 위한 authorization ? ${authorization}</h2>

    <form name="accessToken" method="post" action="/AccessTokenResult">
        <input type="text" id="authorization" name="authorization" value="${authorization}">
        <a href="javascript:fnPopup();"> AccessToken 발급받기</a>
    </form>
</body>
</html>

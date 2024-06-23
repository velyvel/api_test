<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<html>
<head>
    <title>본인인증 테스트</title>

   <script language='javascript'>

       </script>

    <style type="text/css">td{border: 1px solid #ccc}</style>
</head>
<body>

<h2>AccessToken 발급하기 위한 authorization ? ${authorization}</h2>

    <form name="encData" method="post" action="/encToken">
        <input type="text" id="key" name="key" value="${key}">
        <input type="text" id="iv" name="iv" value="${iv}">
        <input type="text" id="hmac_key" name="hmac_key" value="${hmac_key}">
        <a href="javascript:fnPopup();"> encData 발급받기</a>
    </form>
</body>
</html>

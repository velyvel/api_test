<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<html>
<head>
    <title>본인인증 테스트</title>

   <script language='javascript'>
           function fnPopup() {
                   var req_dtim = document.getElementById('req_dtim').value;
                   var enc_mode = document.getElementById('enc_mode').value;
                   var req_no = document.getElementById('req_no').value;

                   var data = {
                       req_dtim: req_dtim,
                       enc_mode: enc_mode,
                       req_no: req_no
                   };

                   // fetch를 사용하여 POST 요청 보내기
                   fetch('/encToken', {
                       method: 'POST',
                       headers: {
                           'Content-Type': 'application/json',
                           'CNTY_CD': 'ko'
                       },
                       body: JSON.stringify(data)
                   })
                   .then(response => {
                       if (!response.ok) {
                           throw new Error('Network response was not ok');
                       }
                       return response.json();
                   })
                   .then(data => {
                       console.log('Encrypted token request successful:', data);
                       // redirect 추가
                       redirect "/makeSymmetricKey"
                   })
                   .catch(error => {
                       console.error('Error during encrypted token request:', error);
                       // 오류 처리 코드 추가
                   });
               }
       </script>

    <style type="text/css">td{border: 1px solid #ccc}</style>
</head>
<body>

<h2>AccessToken 발급하기 위한 authorization ? ${authorization}</h2>

    <form name="encData" method="post" action="/encToken">
        <input type="text" id="req_dtim" name="req_dtim" value="${req_dtim}">
        <input type="text" id="enc_mode" name="enc_mode" value="${enc_mode}">
        <input type="text" id="req_no" name="req_no" value="${req_no}">
        <a href="javascript:fnPopup();"> encData 발급받기</a>
    </form>
</body>
</html>

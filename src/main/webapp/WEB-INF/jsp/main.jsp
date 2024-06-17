<%@ page language="java" contentType="text/html; charset=UTF-8" %>
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
                <%--<td><%=req_no%></td>--%>
            </tr>
            <tr>
                <td>req_dtim</td>
                <%--<td><%=req_dtim%></td>--%>
            </tr>
            <tr>
                <td>token_value</td>
                <%--<td><%=token_val%></td>--%>
            </tr>
            <tr>
                <td>token version id</td>
                <%--<td><%=token_version_id %></td>--%>
            </tr>
            <tr>
                <td>result value</td>
                <%--<td><%=resultVal %></td>--%>
            </tr>
            <tr>
                <td>key</td>
                <%--<td><%=key %></td>--%>
            </tr>
            <tr>
                <td>iv</td>
                <%--<td><%=iv %></td>--%>
            </tr>
            <tr>
                <td>hmac_key</td>
                <%--<td><%=hmac_key%></td>--%>
            </tr>
            <tr>
                <td>jsondata</td>
                <%--<td><%=plain%></td>--%>
            </tr>
            <tr>
                <td>enc_data</td>
                <%--<td><%=enc_data %></td>--%>
            </tr>
            <tr>
                <td>integrity_value</td>
                <%--<td><%=integrity %></td>--%>
            </tr>
        </tbody>
    </table>

    <!--본인인증 서비스 팝업을 호출하기 위해서는 다음과 같은 form이 필요합니다 -->

    <form name="form_chk" method="post">
        <input type="text" name="m" value=${service}>
        <input type="text" name="token_version_id" value=${token_version_id}>
        <input type="text" name="enc_data" value=${enc_data}>
        <input type="text" name="integrity_value" value=${integrity_value}>
        <a href="javascript:fnPopup();"> CheckPlus 안심본인인증 Click</a>
    </form>
</body>
</html>

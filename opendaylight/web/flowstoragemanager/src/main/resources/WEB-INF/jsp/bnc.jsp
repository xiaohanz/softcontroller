<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
<%@ page 
language="java"
import="java.sql.*"
contentType="text/html; charset=gbk"
pageEncoding="GBK"
%>
<META http-equiv="Content-Type" content="text/html; charset=GBK">
<META http-equiv="Content-Style-Type" content="text/css">
<LINK href="theme/Master.css" rel="stylesheet" type="text/css">
<TITLE>bnc.jsp</TITLE>
</HEAD>
<BODY>
 <script type="text/javascript">
 function jump(){
     location.href="http://localhost:8080/";
 }
 </script>
<P>NODE</P>
<table border=1>
<%String url = "jdbc:odbc:MySql";
 String user ="root";//username
 String password = "hou";//password
 String sqlStr = "select distinct node_id from `flow`";
 Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
 Connection con = DriverManager.getConnection(url,"root","hou");
 Statement st = con.createStatement();
 ResultSet rs = st.executeQuery(sqlStr);
 while(rs.next())
 {%>
     <tr><td><%=rs.getString(1)%></td><td><a href='#' onclick="jump()">allflow</a></td></tr>
     <% }%>
     <%
         rs.close();
         st.close();
         con.close();
     %>
</table>
</BODY>
</HTML>
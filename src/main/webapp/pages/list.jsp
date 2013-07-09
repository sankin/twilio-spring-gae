<%@ page import="java.util.List" %>
<%@ page import="com.google.appengine.api.datastore.Entity" %>
<html>
<body>
	<h2>Voice Message Log</h2>

	<a href="new">Send a new message</a>
	<hr />

	<h2>Message Log</h2>
	<table border="1">
		<thead>
			<tr>
				<td>To</td>
				<td>From</td>
                <td>Message</td>
				<td>Created Date</td>
				<td>Status</td>
                <td>Delivered Date</td>
			</tr>
		</thead>
		<%
		
			List<Entity> messages = (List<Entity>)request.getAttribute("messagesList");
		    for(Entity e : messages){
		     
		%>
			<tr>
				<td><%=e.getProperty("to") %></td>
				<td><%=e.getProperty("from") %></td>
				<td><%=e.getProperty("created") %></td>
                <td><%=e.getProperty("message") %></td>
                <td><%=e.getProperty("status") %></td>
                <td><%=e.getProperty("delivered") %></td>
				<!-- <td><a href="update/<%=e.getProperty("id")%>">Update</a> | <a href="delete/<%=e.getProperty("id")%>">Delete</a></td> -->
			</tr>
		<%
			}
		%>
	</table>

</body>
</html>

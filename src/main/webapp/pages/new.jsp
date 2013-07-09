<html>
<body>
	<h1>Send a voice message</h1>
	<form method="post" action=send>
		<table>
            <tr>
                <td>To :</td>
                <td><input type="text" style="width: 200px;" maxlength="30" name="to" id="to" /></td>
            </tr>
            <tr>
				<td>From :</td>
				<td><input type="text" style="width: 200px;" maxlength="30" name="from" id="from" /></td>
			</tr>
			<tr>
				<td>Message :</td>
				<td><input type="text" style="width: 200px;" maxlength="30" name="message" id="message" /></td>
			</tr>
            <tr>
                <td>Account Id (not saved):</td>
                <td><input type="text" style="width: 200px;" maxlength="50" name="account_id" id="account_id" /></td>
            </tr>
            <tr>
                <td>Oauth Token (not saved) :</td>
                <td><input type="text" style="width: 200px;" maxlength="50" name="oauth_token" id="oauth_token" /></td>
            </tr>
		</table>
		<input type="submit" class="call" title="Send Voice Message" value="Call" />
	</form>
</body>
</html>

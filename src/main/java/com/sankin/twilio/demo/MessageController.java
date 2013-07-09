package com.sankin.twilio.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.common.util.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/messages")
public class MessageController {

    private static final Logger log = Logger.getLogger(MessageController.class.getName());
	public static final String charset = "US-ASCII";
	public static final String DS_NAMESPACE = "Messages";


    @RequestMapping(value="/{messageId}", method = RequestMethod.POST)
    public @ResponseBody String getMessage(@PathVariable("messageId") long messageId) {
        String message = "default message";
        final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        final Key key = KeyFactory.createKey(DS_NAMESPACE, messageId);
        try {
            final Entity entity = datastoreService.get(key);
            message = (String) entity.getProperty("message");
            entity.setProperty("status", "sent");
            entity.setProperty("delivered", new Date());
            datastoreService.put(entity);
        } catch (EntityNotFoundException e) {
            log.log(Level.SEVERE, "Failed to find message by id", e);
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say voice=\"woman\">" + message + "</Say></Response>";
    }

    @RequestMapping(value="new", method = RequestMethod.GET)
	public String getCallPage() {
		return "new";
	}
	
    @RequestMapping(value="send", method = RequestMethod.POST)
    public String send(final HttpServletRequest request) {
        final String to = request.getParameter("to");
        final String from = request.getParameter("from");
        final String message = request.getParameter("message");
        final String oauthToken = request.getParameter("oauth_token");
        final String accountId = request.getParameter("account_id");

        final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        final Entity entity = new Entity(DS_NAMESPACE);
        entity.setProperty("to", to);
        entity.setProperty("from", from);
        entity.setProperty("message", message);
        entity.setProperty("created", new Date());
        datastoreService.put(entity);
        final String status = callTwilio(to, from, entity.getKey().getId(), accountId, oauthToken);
        entity.setProperty("status", status);
        datastoreService.put(entity);
        return "redirect:list";
    }

    private String callTwilio(String to, String from, Long messageId, String accountId, String oauthToken) {
        try {
            final String authorization = accountId + ":" + oauthToken;
            final URL url = new URL(String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Calls.json", accountId));
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
            connection.setRequestProperty("Authorization", "Basic " + Base64.encode(authorization.getBytes()));
            final String query = String.format("To=%s&From=%s&Url=%s",
                    URLEncoder.encode(to, charset),
                    URLEncoder.encode(from, charset),
                    URLEncoder.encode("http://twilioapidemo.appspot.com/messages/" + messageId, charset));
            final OutputStream output = connection.getOutputStream();
            output.write(query.getBytes(charset));
            output.close();
            return connection.getResponseCode() + ":" + connection.getResponseMessage();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to make API call to Twilio", e);
            return "Error: " + e.getMessage();
        }
    }

    @RequestMapping(value="/list", method = RequestMethod.GET)
    public String listMessages(ModelMap model) {
        final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        final Query query = new Query(DS_NAMESPACE).addSort("created", Query.SortDirection.DESCENDING);
        final List<Entity> messages = datastoreService.prepare(query).asList(FetchOptions.Builder.withLimit(10));
        model.addAttribute("messagesList",  messages);
        return "list";
    }
}

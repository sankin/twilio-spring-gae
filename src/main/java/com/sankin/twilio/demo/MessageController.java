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

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.common.util.Base64;

@Controller
@RequestMapping("/messages")
public class MessageController {
    private static final Logger log = Logger.getLogger(MessageController.class.getName());

	private static final String charset = "US-ASCII";
	private static final String DATA_STORE_NAMESPACE = "Messages";
	private static final String XML_RESPONSE =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say voice=\"woman\">%s</Say></Response>";


	/**
	 * This will be called by twilio to retrieve the XML mark-up that describes what should be done during the call.
	 * For now, just read the message entered by user.
	 *
	 */
    @RequestMapping(value="/{messageId}", method = RequestMethod.POST)
    public @ResponseBody String getMessage(@PathVariable("messageId") long messageId) {
        String message = "Sorry, could not find your message.";
        final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        final Key key = KeyFactory.createKey(DATA_STORE_NAMESPACE, messageId);
        try {
            final Entity entity = datastoreService.get(key);
            message = (String) entity.getProperty("message");
            entity.setProperty("status", "sent");
            entity.setProperty("delivered", new Date());
            datastoreService.put(entity);
        } catch (EntityNotFoundException e) {
            log.log(Level.SEVERE, "Failed to find message by id, using default message.", e);
        }
        return String.format(XML_RESPONSE, message);
    }

	/**
	 * Just redirect to a form where user can enter details of a new message.
	 *
	 * @return
	 */
    @RequestMapping(value="new", method = RequestMethod.GET)
	public String getCallPage() {
		return "new";
	}

	/**
	 * This method will save the details of the new message and then attempt to send using twilio service
	 *
	 * @param request
	 * @return
	 */
    @RequestMapping(value="send", method = RequestMethod.POST)
    public String send(final HttpServletRequest request) {
	    // retrieve form fields
        final String to = request.getParameter("to");
        final String from = request.getParameter("from");
        final String message = request.getParameter("message");
        final String oauthToken = request.getParameter("oauth_token");
        final String accountId = request.getParameter("account_id");
	    // save message details to data store
        final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        final Entity entity = new Entity(DATA_STORE_NAMESPACE);
        entity.setProperty("to", to);
        entity.setProperty("from", from);
        entity.setProperty("message", message);
        entity.setProperty("created", new Date());
        datastoreService.put(entity);
	    // send the message
        sendMessage(to, from, entity.getKey().getId(), accountId, oauthToken);
        return "redirect:list";
    }

	/**
	 * This method will use the built-in UrlFetch service of Goolge Apps Engine to post a message to twilio service.
	 * //TODO: consider switching to http-client or restTemplate.
	 *
	 */
    private String sendMessage(final String to, final String from, final Long messageId, final String accountId, final String oauthToken) {
        try {
            final String authorization = accountId + ":" + oauthToken;
            final URL url = new URL(String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Calls.json", accountId));
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
            connection.setRequestProperty("Authorization", String.format("Basic %s", Base64.encode(authorization.getBytes())));
            final String query = String.format("To=%s&From=%s&Url=%s",
                    URLEncoder.encode(to, charset),
                    URLEncoder.encode(from, charset),
                    URLEncoder.encode(String.format("http://twilioapidemo.appspot.com/messages/%s", messageId), charset));
            final OutputStream output = connection.getOutputStream();
            output.write(query.getBytes(charset));
            output.close();
            return connection.getResponseCode() + ":" + connection.getResponseMessage();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to make http post request.", e);
            return "Error: " + e.getMessage();
        }
    }

    @RequestMapping(value="/list", method = RequestMethod.GET)
    public String listMessages(ModelMap model) {
        final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        final Query query = new Query(DATA_STORE_NAMESPACE).addSort("created", Query.SortDirection.DESCENDING);
        final List<Entity> messages = datastoreService.prepare(query).asList(FetchOptions.Builder.withLimit(100));
        model.addAttribute("messagesList",  messages);
        return "list";
    }
}

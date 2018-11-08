package eu.interiot.intermw.bridge.uaal.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UAALClient {
    
    private static final Logger logger = LoggerFactory.getLogger(UAALClient.class);

    public static String invoke(String url, String usr, String pwd,
	    String method, String type, String body) throws Exception {
	HttpURLConnection conn = null;
	try {
	    byte[] data = null;
	    if (body != null) {
		data = body.getBytes(Charset.forName("UTF-8"));
	    }
	    String auth = "Basic "
		    + Base64.encodeBytes((usr + ":" + pwd).getBytes());
	    URL server = new URL(url);

	    conn = (HttpURLConnection) server.openConnection();
	    conn.setRequestMethod(method);
	    conn.setInstanceFollowRedirects(false);
	    conn.setDoOutput(true);
	    conn.setDoInput(true);
	    conn.setUseCaches(false);
	    conn.setReadTimeout(30000);
	    conn.setRequestProperty("Content-Type", type);
	    conn.setRequestProperty("charset", "utf-8");
	    if (data != null) {
		conn.setRequestProperty("Content-Length",
			"" + Integer.toString(data.length));
	    }
	    conn.setRequestProperty("Authorization", auth);

	    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
	    if (data != null) {
		wr.write(data);
	    }
	    wr.flush();
	    wr.close();

	    BufferedReader rd = new BufferedReader(
		    new InputStreamReader(conn.getInputStream(), "UTF-8"));
	    String line = rd.readLine();
	    StringBuilder result = new StringBuilder();
	    while (line != null) {
		result.append(line);
		line = rd.readLine();
	    }
	    
	    if ((conn.getResponseCode() < 200 || conn.getResponseCode() > 299) 
		    && !(conn.getResponseCode()==409 && type.equals("POST"))) { // POST on existing
		throw new Exception("Unsuccessful server response: "
			+ conn.getResponseCode() + ". Result: " + result.toString());
	    }
	    
	    if (!result.toString().isEmpty()) {
		return result.toString();
	    }
	} finally {
	    // close the connection and set all objects to null
	    if (conn != null) {
		conn.disconnect();
	    }
	}
	return null;
    }
    
    public static String post(String url, String usr, String pwd, String type,
	    String body) throws Exception {
	logger.trace("Sending POST request to uAAL: " + url );
	return invoke(url, usr, pwd, "POST", type, body);
    }
    
    public static String put(String url, String usr, String pwd, String type,
	    String body) throws Exception {
	logger.trace("Sending PUT request to uAAL: " + url );
	return invoke(url, usr, pwd, "PUT", type, body);
    }

    public static void delete(String url, String usr, String pwd)
	    throws Exception {
	logger.trace("Sending DELETE request to uAAL: " + url );
	invoke(url, usr, pwd, "DELETE", "application/json", null);
    }

}

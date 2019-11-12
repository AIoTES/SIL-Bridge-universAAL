/*
 * INTER-IoT. Interoperability of IoT Platforms.
 * INTER-IoT is a R&D project which has received funding from the European
 * Union's Horizon 2020 research and innovation programme under grant
 * agreement No 687283.
 * <p>
 * Copyright 2019 ITACA-SABIEN, http://www.tsb.upv.es
 * Instituto Tecnologico de Aplicaciones de Comunicacion
 * Avanzadas - Grupo Tecnologias para la Salud y el
 * Bienestar (SABIEN)
 * <p>
 * For more information, contact:
 * - @author <a href="mailto:alfiva@itaca.upv.es">Alvaro Fides</a>
 * - Project coordinator:  <a href="mailto:coordinator@inter-iot.eu"></a>
 * <p>
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership
 * <p>
 * This code is made available under either EPL license, or the 
 * Apache Software License, Version 2.0;
 * You may use the entirety of the code under the conditions specified 
 * by one of either licenses, chosen at your discretion.
 * <p>
 * A copy of the EPL is available at the root application directory.
 * You may obtain a copy of the Apcache License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

	    if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299) {
	    	BufferedReader rd = new BufferedReader(
	    			new InputStreamReader(conn.getInputStream(), "UTF-8"));
	    	String line = rd.readLine();
	    	StringBuilder result = new StringBuilder();
	    	while (line != null) {
	    		result.append(line);
	    		line = rd.readLine();
	    	}

	    	if (!result.toString().isEmpty()) {
	    		return result.toString();
	    	}
	    }else if(conn.getResponseCode()==409 && method.equals("POST")){ // getInputStream fails if 409
	    	return "OK"; // POST on existing. Ignore
	    }else{
	    	throw new Exception("Unsuccessful server response: "
	    			+ conn.getResponseCode());
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

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
package eu.interiot.intermw.bridge.uaal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;

import eu.interiot.message.MessagePayload;

public class Util {
    protected static String getSuffix(String interiotID) {
	int lastindex = interiotID.lastIndexOf("#");
	if (lastindex < 0) { // Now interiot ID ends with InterIoT.owl# but it used to end with /
	    lastindex = interiotID.lastIndexOf("/");
	}
	return interiotID.substring(lastindex + 1);
    }
    
    protected static String getSuffixCalleeGET(String interiotID) {
	return getSuffix(interiotID)+"device";
    }
    
    protected static String getSuffixCalleeGETVALUE(String interiotID) {
	return getSuffix(interiotID)+"value";
    }
    
    protected static List<Resource> getDevices(MessagePayload payload) {
	Model model = payload.getJenaModel();
	return model.listResourcesWithProperty(RDF.type,
		model.getResource("http://inter-iot.eu/GOIoTP#IoTDevice"))
		.toList();
    }
    
    protected static String getValueType(String deviceType) {
	// TODO Auto-generated method stub: Get type of hasValue property for a given device type
	if (deviceType.equals("http://ontology.universAAL.org/Device.owl#TemperatureSensor"))
	    return "http://www.w3.org/2001/XMLSchema#float";
	else if (deviceType.equals("http://ontology.universaal.inter-iot.eu/Train#PresenceSensor"))
	    return "http://ontology.universAAL.org/Device.owl#StatusValue";
	else if (deviceType.equals("http://ontology.universaal.inter-iot.eu/Train#TurnoutActuator"))
	    return "http://ontology.universaal.inter-iot.eu/Train#TurnoutState";
	
	return "http://www.w3.org/2001/XMLSchema#float";
    }

    protected static String getSpecializedType(Resource device, Logger log) {
	try{
	    StmtIterator types = device.listProperties(RDF.type);
	    while (types.hasNext()){
		String t = types.next().getResource().getURI();
		if (t.equals("http://ontology.universAAL.org/Device.owl#TemperatureSensor")
			|| t.equals("http://ontology.universaal.inter-iot.eu/Train#PresenceSensor")
			|| t.equals("http://ontology.universaal.inter-iot.eu/Train#TurnoutActuator")) {
		    return t;
		}
	    }
	    //TODO Extract device most specialized RDF Type !!!
	}catch(Exception ex){
	    log.warn("Error extracting most specialized Device type. Using generic uAAL ValueDevice", ex);
	    return "http://ontology.universAAL.org/Device.owl#ValueDevice";
	}
	log.warn("Could not extract most specialized Device type. Using generic uAAL ValueDevice");
	return "http://ontology.universAAL.org/Device.owl#ValueDevice";
    }
    
    protected static String encodePlatformId(String platformId){
	// From https://stackoverflow.com/questions/607176/ ... 
	// ... java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
	try {
	    return URLEncoder.encode(platformId, "UTF-8")
	    	    .replaceAll("\\+", "%20")
//	    	    .replaceAll("\\%21", "!")
//	    	    .replaceAll("\\%27", "'")
//	    	    .replaceAll("\\%28", "(")
//	    	    .replaceAll("\\%29", ")")
//	    	    .replaceAll("\\%7E", "~")
	    	    .replaceAll("\\%..", "");
	} catch (UnsupportedEncodingException e) {
	    return platformId;
	}
    }
    
    protected static String injectHash(String uri){
	if(uri.contains("#")){ // something.owl#thing
	    return uri; // > something.owl#thing
	}else{
	    int index = uri.lastIndexOf("/");
	    if(index<0){ // somethingthing
		return "http://inter-iot.eu/default.owl#"+uri; // > ...owl#somethingthing
	    }else{ // something/thing
		return uri.substring(0, index+1)+"default.owl#"+uri.substring(index+1); 
	    } // > something/default.owl#thing
	}
    }
}

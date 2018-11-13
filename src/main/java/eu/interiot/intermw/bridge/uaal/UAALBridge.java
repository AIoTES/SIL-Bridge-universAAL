/**
 * INTER-IoT. Interoperability of IoT Platforms.
 * INTER-IoT is a R&D project which has received funding from the European
 * Union�s Horizon 2020 research and innovation programme under grant
 * agreement No 687283.
 * <p>
 * Copyright (C) 2017-2018, by :
 * - 	ITACA-SABIEN, http://www.sabien.upv.es/
 * Instituto Tecnologico de Aplicaciones de Comunicacion
 * Avanzadas - Grupo Tecnologias para la Salud y el
 * Bienestar
 * <p>
 * <p>
 * For more information, contact:
 * - @author <a href="mailto:alfiva@itaca.upv.es">Alvaro Fides</a>
 * - Project coordinator:  <a href="mailto:coordinator@inter-iot.eu"></a>
 * <p>
 * <p>
 * This code is licensed under the EPL license, available at the root
 * application directory.
 */
package eu.interiot.intermw.bridge.uaal;

import static spark.Spark.post;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
//import java.util.HashSet;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import eu.interiot.intermw.bridge.BridgeConfiguration;
import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.uaal.client.Body;
import eu.interiot.intermw.bridge.uaal.client.UAALClient;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.metadata.PlatformMessageMetadata;
import eu.interiot.message.payload.types.IoTDevicePayload;

@eu.interiot.intermw.bridge.annotations.Bridge(platformType = "http://inter-iot.eu/UniversAAL")
public class UAALBridge extends AbstractBridge {
    private final static String PROPERTIES_PREFIX = "universaal-";
    private final static String DEFAULT_CALLER = "default";
    private final static String JSON = "application/json";
    private final static String TEXT = "text/plain";
    private final static String PATH_CONTEXT = "/uaal/context/";
    private final static String PATH_DEVICE = "/uaal/device/";
    private final static String PATH_VALUE = "/uaal/value/";
//    private static final String URI_RETURNS = "http://ontology.universAAL.org/uAAL.owl#returns";
    private static final String URI_MULTI = "http://ontology.universAAL.org/uAAL.owl#MultiServiceResponse";
    private static final String URI_PARAM = "http://www.daml.org/services/owl-s/1.1/Process.owl#parameterValue";
    private static final String URI_OUTPUT = "http://ontology.universAAL.org/InterIoT.owl#output1";
    private static final String URI_EVENT = "http://ontology.universAAL.org/Context.owl#ContextEvent";
    private static final String URI_PROVIDER = "http://ontology.universAAL.org/Context.owl#hasProvider";
    private static final String URI_STATUS = "http://ontology.universAAL.org/uAAL.owl#callStatus";
//    private static final String URI_SUCCEEDED = "http://ontology.universAAL.org/uAAL.owl#call_succeeded";
    private static final String URI_TIMEOUT = "http://ontology.universAAL.org/uAAL.owl#response_timed_out";
    private static final String URI_FAIL = "http://ontology.universAAL.org/uAAL.owl#service_specific_failure";
    private static final String URI_DENIED = "http://ontology.universAAL.org/uAAL.owl#denied";
    private static final String URI_NOMATCH = "http://ontology.universAAL.org/uAAL.owl#no_matching_service_found";
    private final Logger log = LoggerFactory.getLogger(UAALBridge.class);
    private String url;
    private String usr;
    private String pwd;
    private String space;
    private String bridgeCallback_ID;
    private String bridgeCallback_CONTEXT;
    private String bridgeCallback_DEVICE;
    private String bridgeCallback_VALUE;
    // These are to keep track of which callbacks Spark must listen, and which must 404
    // TODO Temporarily disabled because devices apparently are not re-created/subscribed at restart
//    private HashSet<String> validCallback_CONTEXT =new HashSet<String>();
//    private HashSet<String> validCallback_DEVICE =new HashSet<String>();
//    private HashSet<String> validCallback_VALUE =new HashSet<String>();

    public UAALBridge(BridgeConfiguration config, Platform platform) throws MiddlewareException {
	super(config, platform);
	log.debug("UniversAAL bridge is initializing...");
	try {
	    url = config.getProperty(PROPERTIES_PREFIX + "url");
	    usr = config.getProperty(PROPERTIES_PREFIX + "user");
	    pwd = config.getProperty(PROPERTIES_PREFIX + "password");
	    space = config.getProperty(PROPERTIES_PREFIX + "space");
	    bridgeCallback_ID = "/"+encodePlatformId(platform.getPlatformId());
	    bridgeCallback_CONTEXT = bridgeCallbackUrl.toString()+bridgeCallback_ID+PATH_CONTEXT;
	    bridgeCallback_DEVICE = bridgeCallbackUrl.toString()+bridgeCallback_ID+PATH_DEVICE;
	    bridgeCallback_VALUE = bridgeCallbackUrl.toString()+bridgeCallback_ID+PATH_VALUE;
	} catch (Exception e) {
	    throw new MiddlewareException(
		    "Failed to read UAALBridge configuration: "
			    + e.getMessage());
	}
	if (Strings.isNullOrEmpty(url) 
		|| Strings.isNullOrEmpty(usr)
		|| Strings.isNullOrEmpty(pwd)
		|| Strings.isNullOrEmpty(space)){
	    throw new MiddlewareException(
		    "Invalid UAALBridge configuration: empty or invalid values.");
	}
	if (!url.endsWith("/")) url = url + "/"; // Just in case

	log.info("UniversAAL bridge has been initialized successfully.");
    }

    // ------------------------------------------

    @Override
    public Message registerPlatform(Message msg) throws Exception {
	/*
	 * Create a Space in universAAL REST API for INTER-IoT as a user. Then
	 * create a Default Service Caller, since we will only ever need a
	 * single Service Caller for all our calls.
	 */

	log.info("Entering registerPlatform");
	
	boolean init=msg.getMetadata().getMessageTypes().contains(MessageTypesEnum.SYS_INIT);
	if(!init){
	    String bodySpace = Body.CREATE_SPACE
		    .replace(Body.ID, space)
		    .replace(Body.CALLBACK, bridgeCallbackUrl.toString());
	    String bodyCaller = Body.CREATE_CALLER
		    .replace(Body.ID, DEFAULT_CALLER);

	    UAALClient.post(url + "spaces", usr, pwd, JSON, bodySpace);
	    UAALClient.post(url + "spaces/" + space + "/service/callers", usr, pwd, JSON, bodyCaller);
	}// Else do not re-create platform (only to save network, it's OK anyway)
	
	// Register the Spark callback servlets, only once per platform, INIT or not
	registerCallback_CONTEXT();
	registerCallback_DEVICE();
	registerCallback_VALUE();

	log.info("Completed registerPlatform");

	return ok(msg);
    }

    @Override
    public Message unregisterPlatform(Message msg) throws Exception {
	/*
	 * Delete the Space created in registerPlatform for INTER-IoT from the
	 * uAAL REST API. This will also delete all its elements that were
	 * created afterwards.
	 */

	// TODO Is it OK to remove the Space? It depends on the recovery: If the
	// Space is deleted, when INTER-IoT registers the platform again, it
	// will have to re-create and re-subscribe all the things it had before.

	log.info("Entering unregisterPlatform");

	UAALClient.delete(url + "spaces/" + space, usr, pwd);

	log.info("Completed unregisterPlatform");

	return ok(msg);
    }
    
    @Override
    public Message updatePlatform(Message arg0) throws Exception {
	// TODO Auto-generated method stub
	return null;
    }

    // ------------------------------------------

    @Override
    public Message subscribe(Message msg) throws Exception  {
	/*
	 * Subscribe to events sent by an existing device in uAAL and forward them to intermw
	 * Create a CSubscriber for this thing
	 *  CSubscriber
	 *    CEP[sub=thing.getThingID, pred=*, obj=*]
	 * The Subscriber: Receive anything about this device
	 */

	log.info("Entering subscribe");

	String deviceURI, body;
	String conversationId = msg.getMetadata().getConversationId().orElse(null);
	boolean init=msg.getMetadata().getMessageTypes().contains(MessageTypesEnum.SYS_INIT);
	
//	validCallback_CONTEXT.add(conversationId);
	for (Resource device : getDevices(msg.getPayload())) {
	    deviceURI=device.getURI();
	    if(!init){
		body = Body.CREATE_SUBSCRIBER
			.replace(Body.ID, getSuffix(deviceURI))
			.replace(Body.URI, deviceURI)
			.replace(Body.CALLBACK, bridgeCallback_CONTEXT+conversationId);
		UAALClient.post(url + "spaces/" + space + "/context/subscribers", usr, pwd, JSON, body);
	    }// Else do not re-create subscriber (only to save network, it's OK anyway)
	}

	log.info("Completed subscribe");

	return ok(msg);
    }

    @Override
    public Message unsubscribe(Message msg) throws Exception {
	/* Remove the CSubscriber created in subscribe */

	log.info("Entering unsubscribe");

//	String conversationId = msg.getMetadata().getConversationId().orElse(null);

//	validCallback_CONTEXT.remove(conversationId);	
	for (Resource device : getDevices(msg.getPayload())) { 
	    UAALClient.delete(url + "spaces/" + space + "/context/subscribers/" + getSuffix(device.getURI()), usr, pwd);
	}

	log.info("Completed unsubscribe");

	return ok(msg);
    }

    // ------------------------------------------

    @Override
    public Message platformCreateDevices(Message msg) throws Exception {
	/*
	* Replicate a thing from another platform into uAAL
	* Create a Device mydevice here that acts like a LDDI Exporter, with:
	*  SCallee
	*    DeviceService>controls>mydevice[GET]
	*      Return the current instance of mydevice
	*    DeviceService>controls>mydevice>hasvalue>value[GET]
	*      Return the current value of mydevice
	*    DeviceService>controls>mydevice[CHANGE(mydevice*)] ???
	*      Changes current instance with the same instance with different props values
	*    DeviceService>controls>mydevice[REMOVE] ???
	*      Removes current instance? Notify INTER-IoT subscribers? Remove self?
	*
	*  CPublisher
	*    CEP[sub=mydev, pred=*, obj=*]
	*      Publisher to be used by update(String deviceURI, Message message)
	* The Publisher: A controller that can publish anything about this thing
	* This is the publisher that has to be used by observe and updateDevice?
	* When a call is requested from uaal, uAAL REST will send it to CALLBACK_SERVICE....
	* Create a RESTlet on that URL with Spark, and whenever a call arrives,
	* build a Message with a Query and push it to InterIoT (?). Post the response back to uAAL
	*/
	
	// TODO For now I am assuming this bridge can answer to calls from uAAL asking for:
	// Get me the entire device with all its properties
	// Get me the "value" sensed/controlled by the device
	// Should I cover anything else?
	// ...but platformCreateDevices is not for what I though. What then?

	log.info("Entering platformCreateDevice");
	log.debug("Entering platformCreateDevice\n"+msg.serializeToJSONLD());

	String deviceURI, deviceType, deviceValueType, bodyS1, bodyS2, bodyC;

	for (Resource device : getDevices(msg.getPayload())) { 
	    deviceURI = device.getURI();
	    deviceType = getSpecializedType(device);
	    deviceValueType = getValueType(deviceType);
	    bodyS1 = Body.CREATE_CALLEE_GET
		    .replace(Body.ID, getSuffixCalleeGET(deviceURI))
		    .replace(Body.CALLBACK, bridgeCallback_DEVICE+getSuffix(deviceURI))
		    .replace(Body.TYPE, deviceType)
		    .replace(Body.URI, deviceURI);
	    bodyS2 = Body.CREATE_CALLEE_GETVALUE
		    .replace(Body.ID, getSuffixCalleeGETVALUE(deviceURI))
		    .replace(Body.CALLBACK, bridgeCallback_VALUE+getSuffix(deviceURI))
		    .replace(Body.TYPE, deviceType)
		    .replace(Body.TYPE_OBJ, deviceValueType)
		    .replace(Body.URI, deviceURI);
	    bodyC = Body.CREATE_PUBLISHER
		    .replace(Body.ID, getSuffix(deviceURI))
		    .replace(Body.URI, deviceURI);
//	    validCallback_DEVICE.add(getSuffix(deviceURI));
//	    validCallback_VALUE.add(getSuffix(deviceURI));
	    UAALClient.post(url + "spaces/" + space + "/service/callees", usr, pwd, JSON, bodyS1);
	    UAALClient.post(url + "spaces/" + space + "/service/callees", usr, pwd, JSON, bodyS2);
	    UAALClient.post(url + "spaces/" + space + "/context/publishers", usr, pwd, JSON, bodyC);
	}

	log.info("Completed platformCreateDevice");

	return ok(msg);
    }

    @Override
    public Message platformUpdateDevices(Message msg) throws Exception {
	/*
	* Modify the status of a thing from another platform and notify uAAL
	* Modify the Device mydevice that will be handled by the Callee
	*/
	// TODO publish the change as event? I'd say no...
	// ...but platformUpdateDevices is not for what I though. What then?

	log.info("Entering platformUpdateDevice");
	log.debug("Entering platformUpdateDevice\n"+msg.serializeToJSONLD());

	String deviceURI, deviceType, deviceValueType, bodyS1, bodyS2, bodyC;

	for (Resource device : getDevices(msg.getPayload())) { 
	    deviceURI = device.getURI();
	    deviceType = getSpecializedType(device);
	    deviceValueType = getValueType(deviceType);
	    bodyS1=Body.CREATE_CALLEE_GET
		    .replace(Body.ID, getSuffixCalleeGET(deviceURI))
		    .replace(Body.CALLBACK, bridgeCallback_DEVICE+getSuffix(deviceURI))
		    .replace(Body.TYPE, deviceType)
		    .replace(Body.URI, deviceURI);
	    bodyS2 = Body.CREATE_CALLEE_GETVALUE
		    .replace(Body.ID, getSuffixCalleeGETVALUE(deviceURI))
		    .replace(Body.CALLBACK, bridgeCallback_VALUE+getSuffix(deviceURI))
		    .replace(Body.TYPE, deviceType)
		    .replace(Body.TYPE_OBJ, deviceValueType)
		    .replace(Body.URI, deviceURI);
	    bodyC = Body.CREATE_PUBLISHER
		    .replace(Body.ID, getSuffix(deviceURI))
		    .replace(Body.URI, deviceURI);
//	    validCallback_DEVICE.add(getSuffix(deviceURI));
//	    validCallback_VALUE.add(getSuffix(deviceURI));
	    UAALClient.put(url + "spaces/" + space + "/service/callees/"+getSuffixCalleeGET(deviceURI), usr, pwd, JSON, bodyS1);
	    UAALClient.put(url + "spaces/" + space + "/service/callees/"+getSuffixCalleeGETVALUE(deviceURI), usr, pwd, JSON, bodyS2);
	    UAALClient.put(url + "spaces/" + space + "/context/publishers/"+getSuffix(deviceURI), usr, pwd, JSON, bodyC);
	}

	log.info("Completed platformUpdateDevice");

	return ok(msg);
    }

    @Override
    public Message platformDeleteDevices(Message msg) throws Exception {
	/* Remove the Device mydevice created in platformUpdateDevices */

	log.info("Entering platformDeleteDevice");
	log.debug("Entering platformDeleteDevice\n"+msg.serializeToJSONLD());

	for (Resource device : getDevices(msg.getPayload())) { 
	    String deviceURI=device.getURI();
//	    validCallback_DEVICE.remove(getSuffix(deviceURI));
//	    validCallback_VALUE.remove(getSuffix(deviceURI));
	    UAALClient.delete(url+"spaces/"+space+"/service/callees/"+getSuffixCalleeGET(deviceURI), usr, pwd);
	    UAALClient.delete(url+"spaces/"+space+"/service/callees/"+getSuffixCalleeGETVALUE(deviceURI), usr, pwd);
	    UAALClient.delete(url + "spaces/" + space + "/context/publishers/" + getSuffix(deviceURI), usr, pwd);
	}

	log.info("Completed platformDeleteDevice");

	return ok(msg);
    }

    // ------------------------------------------

    @Override
    public Message query(Message msg) throws Exception {
	/*
	 * Create a ServiceRequest asking for the equivalent query from a DefaultServiceCaller
	 *  SCaller
	 *    DeviceService>controls>URI=query.getEntityID + MY_URI=query.getType
	 * Return a parsed Thing from the returned Device instance
	 */

	log.info("Entering query");
	log.debug("Entering query\n"+msg.serializeToJSONLD());
	Message responseMsg = createResponseMessage(msg);
	String deviceURI=""; //TODO How to extract single device from msg?
	String deviceType="";
	// Many devices per call? According to docs, no
	String body = Body.CALL_GETDEVICE
		.replace(Body.URI, deviceURI)
		.replace(Body.TYPE, deviceType);

	String serviceResponse = UAALClient
		.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, TEXT, body);
	Model jena = ModelFactory.createDefaultModel();
	Model result = ModelFactory.createDefaultModel();
	jena.read(new ByteArrayInputStream(serviceResponse.getBytes()), null, "TURTLE");
	if(jena.contains(null, jena.getProperty(URI_STATUS), jena.getResource(URI_NOMATCH))){
	    log.warn("Could not find devices in uAAL because there is no one answering to the request");
	    responseMsg.setPayload(new IoTDevicePayload(result));
	    responseMsg.getMetadata().setStatus("OK");
	    return responseMsg;
	}else if(jena.contains(null, jena.getProperty(URI_STATUS), jena.getResource(URI_TIMEOUT)) ||
		jena.contains(null, jena.getProperty(URI_STATUS), jena.getResource(URI_FAIL)) ||
		jena.contains(null, jena.getProperty(URI_STATUS), jena.getResource(URI_DENIED))){
	    log.warn("Could not find devices in uAAL because there was an error");
	    return error(msg);
	}
	// The output is itself a serialized device TODO prevent multivalues (see listDevices)
	String output=jena.getRequiredProperty(
		jena.getResource(URI_OUTPUT), 
		jena.getProperty(URI_PARAM))
		.getObject().asLiteral().getString();
	result.read(new ByteArrayInputStream(output.getBytes()), null, "TURTLE");
	responseMsg.setPayload(new IoTDevicePayload(result));
	responseMsg.getMetadata().setStatus("OK");

	log.info("Completed query");

	return responseMsg;
    }

    @Override
    public Message listDevices(Message msg) throws Exception {
	/*
	 * Return the full reconstructed Device, including value. This
	 * implementation asks for any "get device" that may be out there, then
	 * goes through all the received responses. There is an alternative
	 * implementation commented out at the end of the class.
	 */

	log.info("Entering listDevices");
	log.debug("Entering listDevices\n"+msg.serializeToJSONLD());
	Message responseMsg = createResponseMessage(msg);
	String body = Body.CALL_GETALLDEVICES_BUS;

	String serviceResponse = UAALClient
		.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, TEXT, body);
	
	Model jena = ModelFactory.createDefaultModel();
	Model result = ModelFactory.createDefaultModel();
	jena.read(new ByteArrayInputStream(serviceResponse.getBytes()), null, "TURTLE");
	
	if(jena.contains(null, jena.getProperty(URI_STATUS), jena.getResource(URI_NOMATCH))){
	    log.warn("Could not find devices in uAAL because there is no one answering to the request");
	    responseMsg.setPayload(new IoTDevicePayload(result));
	    responseMsg.getMetadata().setStatus("OK");
	    return responseMsg;
	}else if(jena.contains(null, jena.getProperty(URI_STATUS), jena.getResource(URI_TIMEOUT)) ||
		jena.contains(null, jena.getProperty(URI_STATUS), jena.getResource(URI_FAIL)) ||
		jena.contains(null, jena.getProperty(URI_STATUS), jena.getResource(URI_DENIED))){
	    log.warn("Could not find devices in uAAL because there was an error");
	    return error(msg);
	}
	
	deviceRegistryInitializeOld(msg, result);
	
	ResIterator roots = jena.listResourcesWithProperty(RDF.type, jena.getResource(URI_MULTI));
	if (roots.hasNext()) { // Many responses aggregated into one RDF list. Values are in each "first"
	    NodeIterator responses = jena.listObjectsOfProperty(RDF.first);
	    while (responses.hasNext()) {
		// Each individual response is a serialized ServiceResponse, extract the output
		String response = responses.next().asLiteral().getLexicalForm();
		Model auxJena = ModelFactory.createDefaultModel();
		auxJena.read(new ByteArrayInputStream(response.getBytes()), null, "TURTLE");
		String turtle = auxJena.listObjectsOfProperty(auxJena.getProperty(URI_PARAM))
			.next().asLiteral().getString();
		// The output is a serialized Device, add it to the result
		result = ModelFactory.createDefaultModel();
		result.read(new ByteArrayInputStream(turtle.getBytes()), null, "TURTLE");
		deviceAddOld(msg, result);
	    }
	} else { // Only one response, no list, it is right in the model
	    String turtle = jena.listObjectsOfProperty(jena.getProperty(URI_PARAM))
		    .next().asLiteral().getString();
	    // The output is a serialized Device, add it to the result
	    result.read(new ByteArrayInputStream(turtle.getBytes()), null, "TURTLE");
	}
	
//	responseMsg.setPayload(new IoTDevicePayload(result));
	responseMsg.getMetadata().setStatus("OK");

	log.info("Completed listDevices");

//	return deviceRegistryInitializeOld(msg, result);
	return responseMsg;
    }
    
    private Message deviceRegistryInitializeOld(Message original, Model jena) throws Exception{
	// Initialize device registry
	try{
	    Message deviceRegistryInitializeMessage = new Message();
	    PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
	    metadata.initializeMetadata();
	    metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.DEVICE_REGISTRY_INITIALIZE);
	    metadata.setSenderPlatformId(new EntityID(platform.getPlatformId()));
	    //metadata.setConversationId(conversationId);
	    String conversationId = original.getMetadata().getConversationId().orElse(null);
	    metadata.setConversationId(conversationId);
//	    MessagePayload devicePayload = new MessagePayload(jena);
	    MessagePayload devicePayload = new MessagePayload();
	    deviceRegistryInitializeMessage.setMetadata(metadata);
	    deviceRegistryInitializeMessage.setPayload(devicePayload);
	    publisher.publish(deviceRegistryInitializeMessage);
	    log.debug("Device_Registry_Initialize message has been published upstream.");
	    return ok(original);
	}catch(Exception ex){
	    return error(original);
	}
    }
    
    private Message deviceAddOld(Message original, Model jena) throws Exception{
	try{
	    Message deviceAddMessage = new Message();
	    PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
	    metadata.initializeMetadata();
	    metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.DEVICE_ADD_OR_UPDATE);
	    metadata.setSenderPlatformId(new EntityID(platform.getPlatformId()));
	    //metadata.setConversationId(conversationId); 
	    String conversationId = original.getMetadata().getConversationId().orElse(null);
	    metadata.setConversationId(conversationId);
	    MessagePayload devicePayload = new MessagePayload(jena);
	    deviceAddMessage.setMetadata(metadata);
	    deviceAddMessage.setPayload(devicePayload);
	    publisher.publish(deviceAddMessage);
	    log.debug("Device_Add message has been published upstream.");
	    return ok(original);
	}catch(Exception ex){
	    return error(original);
	}
    }
    
//    private Message deviceRegistryInitializeNew(Message original, Model jena){
//	Message responseMessage = this.createResponseMessage(original);
//	responseMessage.getMetadata().setMessageType(MessageTypesEnum.DEVICE_REGISTRY_INITIALIZE);
//	responseMessage.getMetadata().addMessageType(MessageTypesEnum.RESPONSE);
//	try {
//	    MessagePayload mwMessagePayload = new MessagePayload(jena);
//	    responseMessage.setPayload(mwMessagePayload);
//	} catch (Exception e) {
//	    responseMessage.getMetadata().addMessageType(MessageTypesEnum.ERROR);
//	    responseMessage.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
//	}
//	return responseMessage;
//    }

    @Override
    public Message observe(Message msg) throws Exception {
	/* Send event to universAAL */
	// TODO There are going to be multiple observations? Maybe

	log.info("Entering observe");
	log.debug("Entering observe\n"+msg.serializeToJSONLD());
	
	Model event = msg.getPayload().getJenaModel();
	String deviceURI = event.listObjectsOfProperty(RDF.subject).next().asResource().getURI();
	String eventURI = event.listStatements(null, RDF.type, event.getResource(URI_EVENT)).nextStatement().getSubject().getURI();
	Writer turtle = new StringWriter();
	event.removeAll(event.getResource(eventURI), event.getProperty(URI_PROVIDER), null)
	.write(turtle, "TURTLE");
	String body = turtle.toString();
	turtle.close();
	
	//TODO PATCH This is to temporarily solve the issue of uAAL serializing only the object in the first line
	String firstLine = "<" + eventURI + "> <" + RDF.type.toString() + "> <" + URI_EVENT + "> . ";

	UAALClient.post(url + "spaces/" + space + "/context/publishers/" + getSuffix(deviceURI), usr, pwd, TEXT, firstLine+body);

	log.info("Completed observe");

	return ok(msg);
    }

    @Override
    public Message actuate(Message msg) throws Exception {
	/*
	 * Here I am going to assume that it works like observer, only that the
	 * payload is an "actuation" instead of am "observation", and that, just
	 * like in observer, it has been properly translated to uAAL format by
	 * IPSM. In the case of observe, that was a ContextEvent. Here it should
	 * be a ServiceRequest (but that's considerably more difficult).
	 */
	
	log.info("Entering actuate");
	log.debug("Entering actuate\n"+msg.serializeToJSONLD());
	
	Model request = msg.getPayload().getJenaModel();
	Writer turtle = new StringWriter();
	request.write(turtle, "TURTLE");
	String body = turtle.toString(); // TODO Check this way to get request works
	turtle.close();

	UAALClient.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, TEXT, body);

	log.info("Completed actuate");

	return ok(msg);
    }

    // ------------------------------------------

    @Override
    public Message error(Message msg) throws Exception {
	// TODO Sends information about any error that occurred inside bridge or inside InterMW.
	log.info("Error occured in {}...", msg);
	Message responseMessage = createResponseMessage(msg);
	responseMessage.getMetadata().setStatus("KO");
	responseMessage.getMetadata().addMessageType(URIManagerMessageMetadata.MessageTypesEnum.ERROR);
	return responseMessage;
    }

    @Override
    public Message unrecognized(Message msg) throws Exception {
	// TODO Custom message type which was not recognized by InterMW.
	log.info("Unrecognized message type.");
	return ok(msg);
    }

    // ------------------------------------------

    private String getSuffix(String interiotID) {
	int lastindex = interiotID.lastIndexOf("#");
	if (lastindex < 0) { // Now interiot ID ends with InterIoT.owl# but it used to end with /
	    lastindex = interiotID.lastIndexOf("/");
	}
	return interiotID.substring(lastindex + 1);
    }
    
    private String getSuffixCalleeGET(String interiotID) {
	return getSuffix(interiotID)+"device";
    }
    
    private String getSuffixCalleeGETVALUE(String interiotID) {
	return getSuffix(interiotID)+"value";
    }
    
    private void registerCallback_CONTEXT() throws BrokerException {
	// When an event is notified, uAAL REST will send it to
	// bridgeCallback_CONTEXT/ConversationId. Create there a RESTlet, and
	// whenever an event arrives, build a Message with an equivalent payload
	// and push it to InterIoT.
	
	// Spark cannot unregister paths, so once bridgeCallback_CONTEXT + conversationId
	// is there, it remains forever. An alternative is using bridgeCallback_CONTEXT + ":/id"
	// and then getting the conversationId with req.params(":conversationId")

	post(bridgeCallback_ID+PATH_CONTEXT + ":conversationId", (req, res) -> {
	    log.debug("CONTEXT CALLBACK -> Got event from uaal");
//	    if(!validCallback_CONTEXT.contains(req.params(":conversationId"))){
//		res.status(404);
//		return "";
//	    }
	    Message messageForInterIoT = new Message();
	    // Metadata
	    PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
	    metadata.initializeMetadata();
	    metadata.setConversationId(req.params(":conversationId"));
	    metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
	    metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
	    metadata.setSenderPlatformId(new EntityID(platform.getPlatformId()));
	    messageForInterIoT.setMetadata(metadata);
	    // Payload
	    String event = req.body();
	    Model eventModel = ModelFactory.createDefaultModel();
	    eventModel.read(new ByteArrayInputStream(event.getBytes()), null, "TURTLE");
	    messageForInterIoT.setPayload(new MessagePayload(eventModel));
	    // Send to InterIoT
	    log.debug("CONTEXT CALLBACK -> Publish message to interiot");
	    publisher.publish(messageForInterIoT);
	    log.debug("CONTEXT CALLBACK -> After publish. Msg: \n"+messageForInterIoT.serializeToJSONLD());
	    res.status(200);
	    return "";
	});
    }
    
    private void registerCallback_DEVICE() throws BrokerException {
	// This is for GET DEVICE.
	// When a call is requested from uaal, uAAL REST will send it to this
	// RESTlet. Build a Message with a Query? and push it to InterIoT (?).
	// Post the response back to uAAL

	post(bridgeCallback_ID+PATH_DEVICE + ":deviceId", (req, res) -> {
	    log.debug("SERVICE CALLBACK -> Got request from uaal");
//	    if(!validCallback_DEVICE.contains(req.params(":deviceId"))){
//		res.status(404);
//		return "";
//	    }
	    String originalCall = req.queryParams("o");
	    Message messageForInterIoT = new Message();
	    // TODO Metadata
	    // TODO Payload
	    // Send to InterIoT
	    log.debug("SERVICE CALLBACK -> Send request to interiot");
	    publisher.publish(messageForInterIoT);
	    // TODO Get response from interiot ???
	    log.debug("SERVICE CALLBACK -> After request. Msg:.... \n");
	    String body = "";// TODO turn response into ServiceResponse ???
	    new Thread() { // TODO Pool?
		public void run() {
		    try {
			wait(2500);
			UAALClient.post(url+"spaces/"+space+"/service/callees/"+req.params(":deviceId")+"?o="+originalCall,
				usr, pwd, TEXT, body);
		    } catch (Exception e) {
			log.error("Error sending service response back to uAAL at registerServiceCallback1", e);
		    }
		}
	    }.start();
	    res.status(200);
	    return "";
	});
    }
    
    private void registerCallback_VALUE() throws BrokerException {
	// This is for GET DEVICE VALUE
	// When a call is requested from uaal, uAAL REST will send it to this
	// RESTlet. Build a Message with a Query? and push it to InterIoT (?).
	// Post the response back to uAAL

	post(bridgeCallback_ID+PATH_VALUE + ":deviceId", (req, res) -> {
	    log.debug("SERVICE CALLBACK -> Got request from uaal");
//	    if(!validCallback_VALUE.contains(req.params(":deviceId"))){
//		res.status(404);
//		return "";
//	    }
	    String originalCall = req.queryParams("o");
	    Message messageForInterIoT = new Message();
	    // TODO Metadata
	    // TODO Payload
	    // Send to InterIoT
	    log.debug("SERVICE CALLBACK -> Send request to interiot");
	    publisher.publish(messageForInterIoT);
	    // TODO Get response from interiot ???
	    log.debug("SERVICE CALLBACK -> After request. Msg:.... \n");
	    String body = "";// TODO turn response into ServiceResponse ???
	    new Thread() { // TODO Pool?
		public void run() {
		    try {
			wait(2500);
			UAALClient.post(url+"spaces/"+space+"/service/callees/"+req.params(":deviceId")+"?o="+originalCall,
				usr, pwd, JSON, body);
		    } catch (Exception e) {
			log.error("Error sending service response back to uAAL at registerServiceCallback2", e);
		    }
		}
	    }.start();
	    res.status(200);
	    return "";
	});
    }
    
    private Message ok(Message inMsg){
	Message responseMsg = createResponseMessage(inMsg);
	responseMsg.getMetadata().setStatus("OK");
	return createResponseMessage(responseMsg);
    }
    
    private List<Resource> getDevices(MessagePayload payload) {
	Model model = payload.getJenaModel();
	return model.listResourcesWithProperty(RDF.type,
		model.getResource("http://inter-iot.eu/GOIoTP#IoTDevice"))
		.toList();
    }
    
    private String getValueType(String deviceType) {
	// TODO Auto-generated method stub: Get type of hasValue property for a given device type
	if (deviceType.equals("http://ontology.universAAL.org/Device.owl#TemperatureSensor"))
	    return "http://www.w3.org/2001/XMLSchema#float";
	
	return "http://www.w3.org/2001/XMLSchema#float";
    }

    private String getSpecializedType(Resource device) {
	try{
	    //TODO Extract device most specialized RDF Type !!!
	}catch(Exception ex){
	    log.warn("Error extracting most specialized Device type. Using generic uAAL ValueDevice", ex);
	    return "http://ontology.universAAL.org/Device.owl#ValueDevice";
	}
	log.warn("Could not extract most specialized Device type. Using generic uAAL ValueDevice");
	return "http://ontology.universAAL.org/Device.owl#ValueDevice";
    }
    
    private String encodePlatformId(String platformId){
	// From https://stackoverflow.com/questions/607176/ ... 
	// ... java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
	try {
	    return URLEncoder.encode(platformId, "UTF-8")
	    .replaceAll("\\+", "%20")
	    .replaceAll("\\%21", "!")
	    .replaceAll("\\%27", "'")
	    .replaceAll("\\%28", "(")
	    .replaceAll("\\%29", ")")
	    .replaceAll("\\%7E", "~");
	} catch (UnsupportedEncodingException e) {
	    log.error("This should have never happened!", e);
	    return platformId;
	}
    }
    
//    @Override
//    public Message listDevices(Message msg) throws Exception {
//	/*
//	 * This is an alternative implementation of listDevices that checks
//	 * existing Devices in CHe rather than ask for any potential
//	 * "get device" service profile. The downside of this one is that some
//	 * devices recorded by CHe may not have a "get device" service, or are
//	 * not present anymore. On the good side, it is more simple and reliable
//	 * (as long as CHe is running).
//	 */
//
//	log.info("Entering listDevices");
//	log.debug("Entering listDevices\n" + msg.serializeToJSONLD());
//	Message responseMsg = createResponseMessage(msg);
//	String body = Body.CALL_GETALLDEVICES_CHE;
//
//	String serviceResponse = UAALClient
//		.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, JSON, body);
//	
//	// Extract CHe response from the returned ServiceResponse
//	Model jena2 = ModelFactory.createDefaultModel();
//	jena2.read(new ByteArrayInputStream(serviceResponse.getBytes()), null, "TURTLE");
//	String turtle2 = jena2.getRequiredProperty(jena2.getResource(URI_OUTPUT),jena2.getProperty(URI_PARAM))
//		.getObject().asLiteral().getString();
//	// Extract devices from CHe response
//	jena2 = ModelFactory.createDefaultModel();
//	jena2.read(new ByteArrayInputStream(turtle2.getBytes()), null, "TURTLE");
//	List<Resource> devicesList = jena2.listResourcesWithProperty(RDF.type,
//		jena2.getResource("http://ontology.universaal.org/PhThing.owl#Device")).toList();
//	// Return that list to interiot. TODO Check if this is OK
//	responseMsg.setPayload(new IoTDevicePayload(jena2));
//	responseMsg.getMetadata().setStatus("OK");
//
//	log.info("Completed listDevices");
//
//	return responseMsg;
//    }

}
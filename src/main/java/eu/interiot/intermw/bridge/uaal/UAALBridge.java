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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.uaal.client.Body;
import eu.interiot.intermw.bridge.uaal.client.UAALClient;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
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
    private final Logger log = LoggerFactory.getLogger(UAALBridge.class);
    private String url;
    private String usr;
    private String pwd;
    private String space;
    private String bridgeCallback_CONTEXT;
    private String bridgeCallback_DEVICE;
    private String bridgeCallback_VALUE;
    // These are to keep track of which callbacks Spark must listen, and which must 404
    private HashSet<String> validCallback_CONTEXT =new HashSet<String>();
    private HashSet<String> validCallback_DEVICE =new HashSet<String>();
    private HashSet<String> validCallback_VALUE =new HashSet<String>();

    public UAALBridge(Configuration config, Platform platform)
	    throws MiddlewareException {
	super(config, platform);
	log.debug("UniversAAL bridge is initializing...");
	Properties properties = configuration.getProperties();
	try {
	    url = properties.getProperty(PROPERTIES_PREFIX + "url");
	    usr = properties.getProperty(PROPERTIES_PREFIX + "user");
	    pwd = properties.getProperty(PROPERTIES_PREFIX + "password");
	    space = properties.getProperty(PROPERTIES_PREFIX + "space");
	    bridgeCallback_CONTEXT = bridgeCallbackUrl.toString()+PATH_CONTEXT;
	    bridgeCallback_DEVICE = bridgeCallbackUrl.toString()+PATH_DEVICE;
	    bridgeCallback_VALUE = bridgeCallbackUrl.toString()+PATH_VALUE;
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
	// Registers specified platform in the InterMW and creates bridge
	// instance for the platform.

	/*
	 * Create a Space in universAAL REST API for INTER-IoT as a user. Then
	 * create a Default Service Caller, since we will only ever need a
	 * single Service Caller for all our calls. I don't need to handle
	 * SYS_INIT because there is nothing wrong with double-POSTing, nothing
	 * happens the second time.
	 */

	log.info("Entering registerPlatform");
	
	log.debug("Entering registerPlatform\n"+msg.serializeToJSONLD());
	
	String bodySpace = Body.CREATE_SPACE
		.replace(Body.ID, space)
		.replace(Body.CALLBACK, bridgeCallbackUrl.toString());
	String bodyCaller = Body.CREATE_CALLER
		.replace(Body.ID, DEFAULT_CALLER);

	UAALClient.post(url + "spaces/", usr, pwd, JSON, bodySpace);
	UAALClient.post(url + "spaces/" + space + "/service/callers/", usr, pwd, JSON, bodyCaller);
	
	// Register the Spark callback servlets, only once per platform.
	registerCallback_CONTEXT();
	registerCallback_DEVICE();
	registerCallback_VALUE();

	log.info("Completed registerPlatform");

	return ok(msg);
    }

    @Override
    public Message unregisterPlatform(Message msg) throws Exception {
	// Unregisters specified platform.

	/*
	 * Delete the Space created in registerPlatform for INTER-IoT from the
	 * uAAL REST API. This will also delete all its elements that were
	 * created afterwards.
	 */

	// TODO Is it OK to remove the Space? It depends on the recovery: If the
	// Space is deleted, when INTER-IoT registers the platform again, it
	// will have to re-create and re-subscribe all the things it had before.

	log.info("Entering unregisterPlatform");
	log.debug("Entering unregisterPlatform\n"+msg.serializeToJSONLD());
	UAALClient.delete(url + "spaces/" + space, usr, pwd);

	log.info("Completed unregisterPlatform");

	return ok(msg);
    }

    // ------------------------------------------

    @Override
    public Message subscribe(Message msg) throws Exception  {
	// Subscribes client to observations provided by the platform for the
	// specified device.
	/*
	 * The subscribe method must implement listener that accepts observation
	 * messages sent from the platform to InterMW for corresponding
	 * subscription. The listener is implemented using Spark framework. It
	 * listens on port defined by the configuration property
	 * bridge.callback.address (8980 by default) and path matching the
	 * conversationId parameter. In the handler (callback) method the
	 * observation data is converted to InterMW observation message of type
	 * Message and sent upstream.
	 */

	// Subscribe to events sent by an existing device in uAAL and forward them to intermw
	// Create a CSubscriber for this thing
	//  CSubscriber
	//    CEP[sub=thing.getThingID, pred=*, obj=*]
	// ConversationId IS NOT SubscriptionId. It appears I have to build the SubscriptionId
	// and return it somehow TBD. ConversationId could perhaps be more akin to a ContextProvider URI...
	// The Subscriber: Receive anything about this device

	log.info("Entering subscribe");
	log.debug("Entering subscribe\n"+msg.serializeToJSONLD());

	String deviceURI, body;
	String conversationId = msg.getMetadata().getConversationId().orElse(null);
	boolean init=msg.getMetadata().getMessageTypes().contains(MessageTypesEnum.SYS_INIT);
	
//	registerCallback_Context(conversationId);
	validCallback_CONTEXT.add(conversationId);
	
	for (Resource device : getDevices(msg.getPayload())) {
	    deviceURI=device.getURI();
	    if(!init){
		body = Body.CREATE_SUBSCRIBER
			.replace(Body.ID, getSuffix(deviceURI))
			.replace(Body.URI, deviceURI)
			.replace(Body.CALLBACK, bridgeCallback_CONTEXT+conversationId);
		UAALClient.post(url + "spaces/" + space + "/context/subscribers/", usr, pwd, JSON, body);
	    }// Else do not re-create subscriber (only to save network, it's OK anyway)
	}

	log.info("Completed subscribe");

	return ok(msg);
    }

    @Override
    public Message unsubscribe(Message msg) throws Exception {
	// Cancels specified subscription created by the Subscribe.

	// Remove the CSubscriber created in subscribe

	log.info("Entering unsubscribe");
	log.debug("Entering unsubscribe\n"+msg.serializeToJSONLD());

	String conversationId = msg.getMetadata().getConversationId().orElse(null);

//	unregisterCallback_Context(conversationId);
	validCallback_CONTEXT.remove(conversationId);
	
	for (Resource device : getDevices(msg.getPayload())) { 
	    UAALClient.delete(url + "spaces/" + space + "/context/subscribers/" + getSuffix(device.getURI()), usr, pwd);
	}

	log.info("Completed unsubscribe");

	return ok(msg);
    }

    // ------------------------------------------

    @Override
    public Message platformCreateDevices(Message msg) throws Exception {
	// An instruction for a platform to start managing (i.e. create) a new
	// device.

	// Replicate a thing from another platform into uAAL
	// Create a Device mydevice here that acts like a LDDI Exporter, with:
	//  SCallee
	//    DeviceService>controls>mydevice[GET]
	//      Return the current instance of mydevice
	//    DeviceService>controls>mydevice>hasvalue>value[GET]
	//      Return the current value of mydevice
	//    DeviceService>controls>mydevice[CHANGE(mydevice*)] ???
	//      Changes current instance with the same instance with different props values
	//    DeviceService>controls>mydevice[REMOVE] ???
	//      Removes current instance? Notify INTER-IoT subscribers? Remove self?
	//
	//  CPublisher
	//    CEP[sub=mydev, pred=*, obj=*]
	//      Publisher to be used by update(String deviceURI, Message message)
	// The Publisher: A controller that can publish anything about this thing
	// This is the publisher that has to be used by observe and updateDevice?
	// When a call is requested from uaal, uAAL REST will send it to CALLBACK_SERVICE....
	// Create here a RESTlet on that URL with Spark, and whenever a call arrives,
	// build a Message with a Query and push it to InterIoT (?). Post the response back to uAAL
	
	// TODO For now I am assuming this bridge can answer to calls from uAAL asking for:
	// Get me the entire device with all its properties
	// Get me the "value" sensed/controlled by the device
	// Should I cover anything else?

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
//	    registerCallback_ServiceGET(getSuffixCalleeGET(deviceURI));
	    validCallback_DEVICE.add(getSuffixCalleeGET(deviceURI));
//	    registerCallback_ServiceGETVALUE(getSuffixCalleeGETVALUE(deviceURI));
	    validCallback_VALUE.add(getSuffixCalleeGETVALUE(deviceURI));
	    UAALClient.post(url + "spaces/" + space + "/service/callees/", usr, pwd, JSON, bodyS1);
	    UAALClient.post(url + "spaces/" + space + "/service/callees/", usr, pwd, JSON, bodyS2);
	    UAALClient.post(url + "spaces/" + space + "/context/publishers/", usr, pwd, JSON, bodyC);
	}

	log.info("Completed platformCreateDevice");

	return ok(msg);
    }

    @Override
    public Message platformUpdateDevices(Message msg) throws Exception {
	// An instruction for a platform to update information about a device it
	// is managing

	// Modify the status of a thing from another platform and notify uAAL
	// Modify the Device mydevice that will be handled by the Callee,
	// TODO publish the change as event?

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
//	    registerCallback_ServiceGET(getSuffixCalleeGET(deviceURI));
	    validCallback_DEVICE.add(getSuffixCalleeGET(deviceURI));
//	    registerCallback_ServiceGETVALUE(getSuffixCalleeGETVALUE(deviceURI));
	    validCallback_VALUE.add(getSuffixCalleeGETVALUE(deviceURI));
	    UAALClient.put(url + "spaces/" + space + "/service/callees/"+getSuffixCalleeGET(deviceURI), usr, pwd, JSON, bodyS1);
	    UAALClient.put(url + "spaces/" + space + "/service/callees/"+getSuffixCalleeGETVALUE(deviceURI), usr, pwd, JSON, bodyS2);
	    UAALClient.put(url + "spaces/" + space + "/context/publishers/"+getSuffix(deviceURI), usr, pwd, JSON, bodyC);
	}

	log.info("Completed platformUpdateDevice");

	return ok(msg);
    }

    @Override
    public Message platformDeleteDevices(Message msg) throws Exception {
	// An instruction for a platform to stop managing (i.e. remove) a
	// device.

	// Remove the Device mydevice that was created in create(String thingID,
	// String thingType)

	log.info("Entering platformDeleteDevice");
	log.debug("Entering platformDeleteDevice\n"+msg.serializeToJSONLD());

	for (Resource device : getDevices(msg.getPayload())) { 
	    String deviceURI=device.getURI();
//	    unregisterCallback_ServiceGET(getSuffixCalleeGET(deviceURI));
	    validCallback_DEVICE.remove(getSuffixCalleeGET(deviceURI));
//	    unregisterCallback_ServiceGETVALUE(getSuffixCalleeGETVALUE(deviceURI));
	    validCallback_VALUE.remove(getSuffixCalleeGETVALUE(deviceURI));
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
	// Makes a query about a status and last observation made by a specified
	// device.

	// Create a ServiceRequest asking for the equivalent query from a DefaultServiceCaller
	//  SCaller
	//    DeviceService>controls>URI=query.getEntityID + MY_URI=query.getType
	// Return a parsed Thing from the returned Device instance

	log.info("Entering query");
	log.debug("Entering query\n"+msg.serializeToJSONLD());
	Message responseMsg = createResponseMessage(msg);
	String deviceURI="";
	String deviceType="";
	// TODO Also many devices per call? How to extract single device from msg?
	String body = Body.CALL_GETDEVICE
		.replace(Body.URI, deviceURI)
		.replace(Body.TYPE, deviceType);

	String serviceResponse = UAALClient
		.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, TEXT, body);
	Model jena = ModelFactory.createDefaultModel();
	jena.read(new ByteArrayInputStream(serviceResponse.getBytes()), null, "TURTLE");
	Resource device=jena.getRequiredProperty(
		jena.getResource("http://ontology.universAAL.org/InterIoT.owl#output1"), 
		jena.getProperty("http://www.daml.org/services/owl-s/1.1/Process.owl#parameterValue"))
		.getResource();
	responseMsg.setPayload(new IoTDevicePayload(device.getModel())); // Isn't the model the jena var?
	responseMsg.getMetadata().setStatus("OK");
	// TODO return response. What if many Resources?

	log.info("Completed query");

	return responseMsg;
    }

    @Override
    public Message listDevices(Message msg) throws Exception {
	// Makes a query to a platform to get all devices managed by it, that it
	// deems discoverable

	// TODO Do they have to be only the Ids? Or the full reconstructed Device, including value? 

	log.info("Entering listDevices");
	log.debug("Entering listDevices\n"+msg.serializeToJSONLD());
	Message responseMsg = createResponseMessage(msg);
	String body = Body.CALL_GETALLDEVICES;

	String serviceResponse = UAALClient
		.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, JSON, body);
	// Extract CHe response from the returned ServiceResponse
	Model jena = ModelFactory.createDefaultModel();
	jena.read(new ByteArrayInputStream(serviceResponse.getBytes()), null, "TURTLE");
	String turtle=jena.getRequiredProperty(
		jena.getResource("http://ontology.universAAL.org/InterIoT.owl#output1"), 
		jena.getProperty("http://www.daml.org/services/owl-s/1.1/Process.owl#parameterValue"))
		.getObject().asLiteral().getString();
	// Extract devices from CHe response
	jena = ModelFactory.createDefaultModel();
	jena.read(new ByteArrayInputStream(turtle.getBytes()), null, "TURTLE");
//	List<Resource> devicesList = jena.listResourcesWithProperty(RDF.type, 
//		jena.getResource("http://ontology.universaal.org/PhThing.owl#Device")).toList();
	// Return that list to interiot. TODO Check if this is OK
	responseMsg.setPayload(new IoTDevicePayload(jena));
	responseMsg.getMetadata().setStatus("OK");
	
	log.info("Completed listDevices");

	return responseMsg;
    }

    @Override
    public Message observe(Message msg) throws Exception {
	// Pushes given observation message from InterMW to platform (bridge
	// acting as a publisher for platform)

	log.info("Entering observe");
	log.debug("Entering observe\n"+msg.serializeToJSONLD());
	Message responseMsg = createResponseMessage(msg);
	String deviceURI = "";// TODO There are going to be multiple observations. Maybe
	String body = "";// TODO How to convert? Can I expect to receive a uAAL ContextEvent thanks to IPSM?

	UAALClient.post(url + "spaces/" + space + "/context/publishers/" + getSuffix(deviceURI), usr, pwd, TEXT, body);

	log.info("Completed observe");

	return responseMsg;
    }

    @Override
    public Message actuate(Message msg) throws Exception {
	// TODO Same as Observe, except the message payload contains actuation instructions.
	return null;
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
    
    private void registerCallback_CONTEXT(/*String conversationId*/) throws BrokerException {
	// When an event is notified, uAAL REST will send it to
	// bridgeCallback_CONTEXT/ConversationId. Create there a RESTlet, and
	// whenever an event arrives, build a Message with an equivalent payload
	// and push it to InterIoT.
	
	// Spark cannot unregister paths, so once bridgeCallback_CONTEXT + conversationId
	// is there, it remains forever. An alternative is using bridgeCallback_CONTEXT + ":/id"
	// and then getting the conversationId with req.params(":conversationId")
//	if(!validCallback_CONTEXT.contains(conversationId)){
//	    validCallback_CONTEXT.add(conversationId);
//	}
	post(PATH_CONTEXT + ":conversationId", (req, res) -> {
	    log.debug("CONTEXT CALLBACK -> Got event from uaal");
	    if(!validCallback_CONTEXT.contains(req.params(":conversationId"))){
		res.status(404);
		return "";
	    }
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
    
//    private void unregisterCallback_Context(String conversationId) {
//	validCallback_CONTEXT.remove(conversationId);
//    }
    
    private void registerCallback_DEVICE(/*String deviceId_device*/) throws BrokerException {
	// This is for GET DEVICE.
	// When a call is requested from uaal, uAAL REST will send it to this
	// RESTlet. Build a Message with a Query? and push it to InterIoT (?).
	// Post the response back to uAAL

//	if(!validCallback_DEVICE.contains(deviceId_device))validCallback_DEVICE.add(deviceId_device);
	post(PATH_DEVICE + ":deviceId", (req, res) -> {
	    log.debug("SERVICE CALLBACK -> Got request from uaal");
	    if(!validCallback_DEVICE.contains(req.params(":deviceId"))){
		res.status(404);
		return "";
	    }
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
			log.error("Error sending service response back to uAAL at registerServiceCallback1", e);
		    }
		}
	    }.start();
	    res.status(200);
	    return "";
	});
    }
    
//    private void unregisterCallback_ServiceGET(String deviceId_device) {
//	validCallback_DEVICE.remove(deviceId_device);
//    }
    
    private void registerCallback_VALUE(/*String deviceId_value*/) throws BrokerException {
	// This is for GET DEVICE VALUE
	// When a call is requested from uaal, uAAL REST will send it to this
	// RESTlet. Build a Message with a Query? and push it to InterIoT (?).
	// Post the response back to uAAL

//	if(!validCallback_VALUE.contains(deviceId_value))validCallback_VALUE.add(deviceId_value);
	post(PATH_VALUE + ":deviceId", (req, res) -> {
	    log.debug("SERVICE CALLBACK -> Got request from uaal");
	    if(!validCallback_VALUE.contains(req.params(":deviceId"))){
		res.status(404);
		return "";
	    }
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
    
//    private void unregisterCallback_ServiceGETVALUE(String deviceId_value) {
//	validCallback_VALUE.remove(deviceId_value);
//    }
    
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
	// TODO Auto-generated method stub: Extract device most specialized RDF Type
	String type = device
		.getProperty(device.getModel()
			.getProperty("http://inter-iot.eu/device-type"))
		.getObject().asLiteral().getString();
	if (type.equals("LIGHT"))//TODO Check possible values of interiot types
	    return "http://ontology.universAAL.org/Device.owl#LightController";

	return "http://ontology.universAAL.org/Device.owl#TemperatureSensor";
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

}
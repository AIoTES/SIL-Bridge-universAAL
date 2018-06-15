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
import java.util.ArrayList;
import java.util.Properties;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.uaal.client.Body;
import eu.interiot.intermw.bridge.uaal.client.UAALClient;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.IoTDevice;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.requests.PlatformCreateDeviceReq;//TODO Do not use
import eu.interiot.intermw.commons.requests.PlatformDeleteDeviceReq;//TODO Do not use
import eu.interiot.intermw.commons.requests.PlatformUpdateDeviceReq;//TODO Do not use
import eu.interiot.intermw.commons.requests.SubscribeReq;//TODO Do not use
import eu.interiot.intermw.commons.requests.UnsubscribeReq;//TODO Do not use
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
    private final Logger log = LoggerFactory.getLogger(UAALBridge.class);
    private String url;
    private String usr;
    private String pwd;
    private String space;
    private String bridgeCallbackContext;
    private String bridgeCallbackServiceDevice;
    private String bridgeCallbackServiceValue;
    // These are to keep track of which callbacks Spark must listen, and which must 404
    private ArrayList<String> callbacksContext =new ArrayList<String>();
    private ArrayList<String> callbacksService1 =new ArrayList<String>();
    private ArrayList<String> callbacksService2 =new ArrayList<String>();

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
	    bridgeCallbackContext = bridgeCallbackUrl.toString()+"context/"; //TODO Check all these URLs
	    bridgeCallbackServiceDevice = bridgeCallbackUrl.toString()+"servicedevice/";
	    bridgeCallbackServiceValue = bridgeCallbackUrl.toString()+"servicevalue/";
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
	
	String bodySpace = Body.CREATE_SPACE
		.replace(Body.ID, space)
		.replace(Body.CALLBACK, bridgeCallbackUrl.toString());
	String bodyCaller = Body.CREATE_CALLER
		.replace(Body.ID, DEFAULT_CALLER);

	UAALClient.post(url + "spaces/", usr, pwd, JSON, bodySpace);
	UAALClient.post(url + "spaces/" + space + "/service/callers/", usr, pwd, JSON, bodyCaller);

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

	SubscribeReq req = new SubscribeReq(msg);
	String thingId, body;
	String conversationId = msg.getMetadata().getConversationId().orElse(null);
	boolean init=msg.getMetadata().getMessageTypes().contains(MessageTypesEnum.SYS_INIT);
	
	registerContextCallback(conversationId);
	for (IoTDevice iotDevice : req.getDevices()) { //TODO Get all devices, in uAAL format???
	    thingId=iotDevice.getDeviceId();// TODO Check which format. Need to get suffix?
	    if(!init){
		body = Body.CREATE_SUBSCRIBER
			.replace(Body.ID, getSuffix(thingId))
			.replace(Body.SUBSCRIBED, thingId)
			.replace(Body.CALLBACK, bridgeCallbackContext+conversationId);
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

	UnsubscribeReq req = new UnsubscribeReq(msg);
	String conversationId = msg.getMetadata().getConversationId().orElse(null);

	unregisterContextCallback(conversationId);
	
	for (String thingId : req.getDevices()) {  //TODO Get all devices, in uAAL format???
	    UAALClient.delete(url + "spaces/" + space + "/context/subscribers/" + getSuffix(thingId), usr, pwd);
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
	//      Publisher to be used by update(String thingId, Message message)
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
	
	PlatformCreateDeviceReq req = new PlatformCreateDeviceReq(msg);
	String thingId, thingType, thingValueType, bodyS1, bodyS2, bodyC;

	for (IoTDevice iotDevice : req.getDevices()) { //TODO Get all devices, in uAAL format???
	    thingId = iotDevice.getDeviceId();// TODO Check which format. Need to get suffix?
	    thingType = "http://ontology.universAAL.org/Device.owl#TemperatureSensor";// TODO
	    thingValueType = "http://www.w3.org/2001/XMLSchema#float";// TODO
	    bodyS1 = Body.CREATE_CALLEE_1
		    .replace(Body.ID, getSuffixForCallee1(thingId))
		    .replace(Body.CALLBACK, bridgeCallbackServiceDevice+getSuffix(thingId))
		    .replace(Body.TYPE, thingType);
	    bodyS2 = Body.CREATE_CALLEE_2
		    .replace(Body.ID, getSuffixForCallee2(thingId))
		    .replace(Body.CALLBACK, bridgeCallbackServiceValue+getSuffix(thingId))
		    .replace(Body.TYPE, thingType)
		    .replace(Body.TYPE_OBJ, thingValueType);
	    bodyC = Body.CREATE_PUBLISHER
		    .replace(Body.ID, getSuffix(thingId));
	    registerServiceCallback1(getSuffixForCallee1(thingId));
	    registerServiceCallback2(getSuffixForCallee2(thingId));
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
	// TODO No need to change publisher because thingID is the same?

	log.info("Entering platformUpdateDevice");
	
	PlatformUpdateDeviceReq req = new PlatformUpdateDeviceReq(msg);
	String thingId, thingType, thingValueType, bodyS1, bodyS2, bodyC;

	for (IoTDevice iotDevice : req.getDevices()) { //TODO Get all devices, in uAAL format???
	    thingId = iotDevice.getDeviceId();// TODO Check which format. Need to get suffix?
	    thingType = "http://ontology.universAAL.org/Device.owl#TemperatureSensor";// TODO
	    thingValueType = "http://www.w3.org/2001/XMLSchema#float";// TODO
	    bodyS1=Body.CREATE_CALLEE_1
		    .replace(Body.ID, getSuffixForCallee1(thingId))
		    .replace(Body.CALLBACK, bridgeCallbackServiceDevice+getSuffix(thingId))
		    .replace(Body.TYPE, thingType);
	    bodyS2 = Body.CREATE_CALLEE_2
		    .replace(Body.ID, getSuffixForCallee2(thingId))
		    .replace(Body.CALLBACK, bridgeCallbackServiceValue+getSuffix(thingId))
		    .replace(Body.TYPE, thingType)
		    .replace(Body.TYPE_OBJ, thingValueType);
	    bodyC = Body.CREATE_PUBLISHER
		    .replace(Body.ID, getSuffix(thingId));
	    registerServiceCallback1(getSuffixForCallee1(thingId));
	    registerServiceCallback2(getSuffixForCallee2(thingId));
	    UAALClient.put(url + "spaces/" + space + "/service/callees/"+getSuffixForCallee1(thingId), usr, pwd, JSON, bodyS1);
	    UAALClient.post(url + "spaces/" + space + "/service/callees/"+getSuffixForCallee2(thingId), usr, pwd, JSON, bodyS2);
	    UAALClient.post(url + "spaces/" + space + "/context/publishers/"+getSuffix(thingId), usr, pwd, JSON, bodyC);
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

	PlatformDeleteDeviceReq req = new PlatformDeleteDeviceReq(msg);

	for (String thingId : req.getDeviceIds()) { //TODO Get all devices, in uAAL format???
	    unregisterServiceCallback1(getSuffixForCallee1(thingId));
	    unregisterServiceCallback2(getSuffixForCallee2(thingId));
	    UAALClient.delete(url+"spaces/"+space+"/service/callees/"+getSuffixForCallee1(thingId), usr, pwd);
	    UAALClient.delete(url+"spaces/"+space+"/service/callees/"+getSuffixForCallee2(thingId), usr, pwd);
	    UAALClient.delete(url + "spaces/" + space + "/context/publishers/" + getSuffix(thingId), usr, pwd);
	}

	log.info("Completed platformDeleteDevice");

	return ok(msg);
    }

    // ------------------------------------------

    @Override
    public Message query(Message msg) throws Exception {
	// Makes a query about a status and last observation made by a specified
	// device.

	// Create a ServiceRequest asking for the equivalent query(?) from a DefaultServiceCaller
	//  SCaller
	//    DeviceService>controls>URI=query.getEntityID + MY_URI=query.getType
	// Return a parsed Thing from the returned Device instance

	log.info("Entering query");

	Message responseMsg = createResponseMessage(msg);
	String body = Body.CALL_QUERY
		.replace(Body.QUERY, "SPARQL Query");// TODO What kind of Query can I expect?

	String serviceResponse = UAALClient
		.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, JSON, body);
	// TODO return response

	log.info("Completed query");

	return responseMsg;
    }

    @Override
    public Message listDevices(Message msg) throws Exception {
	// Makes a query to a platform to get all devices managed by it, that it
	// deems discoverable

	// TODO Do they have to be only the Ids? Or the full reconstructed Device, including value? 

	log.info("Entering query");

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
	
	log.info("Completed query");

	return responseMsg;
    }

    @Override
    public Message observe(Message msg) throws Exception {
	// Pushes given observation message from InterMW to platform (bridge
	// acting as a publisher for platform)

	log.info("Entering observe");

	Message responseMsg = createResponseMessage(msg);
	String thingId = "";// TODO There are going to be multiple observations. Maybe
	String body = "";// TODO How to convert? Can I expect to receive a uAAL ContextEvent thanks to IPSM?

	UAALClient.post(url + "spaces/" + space + "/context/publishers/" + getSuffix(thingId), usr, pwd, TEXT, body);

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
	responseMessage.getMetadata().setMessageType(MessageTypesEnum.ERROR);
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
    
    private String getSuffixForCallee1(String interiotID) {
	return getSuffix(interiotID)+"device";
    }
    
    private String getSuffixForCallee2(String interiotID) {
	return getSuffix(interiotID)+"value";
    }
    
    private void registerContextCallback(String conversationId) throws BrokerException {
	// When an event is notified, uAAL REST will send it to
	// bridgeCallbackContext/ConversationId. Create there a RESTlet, and
	// whenever an event arrives, build a Message with an equivalent payload
	// and push it to InterIoT.
	
	// Spark cannot unregister paths, so once bridgeCallbackContext + conversationId
	// is there, it remains forever. An alternative is using bridgeCallbackContext + ":/id"
	// and then getting the conversationId with req.params(":conversationId")

	if(!callbacksContext.contains(conversationId))callbacksContext.add(conversationId);
	post(bridgeCallbackContext + "/:conversationId", (req, res) -> {
	    log.debug("CONTEXT CALLBACK -> Got event from uaal");
	    if(!callbacksContext.contains(req.params(":conversationId"))){
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
    
    private void unregisterContextCallback(String conversationId) {
	callbacksContext.remove(conversationId);
    }
    
    private void registerServiceCallback1(String deviceId_device) throws BrokerException {
	// This is for GET DEVICE.
	// When a call is requested from uaal, uAAL REST will send it to this
	// RESTlet. Build a Message with a Query? and push it to InterIoT (?).
	// Post the response back to uAAL

	if(!callbacksService1.contains(deviceId_device))callbacksService1.add(deviceId_device);
	post(bridgeCallbackServiceDevice + ":/deviceId", (req, res) -> {
	    log.debug("SERVICE CALLBACK -> Got request from uaal");
	    if(!callbacksService1.contains(req.params(":deviceId"))){
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
    
    private void unregisterServiceCallback1(String deviceId_device) {
	callbacksService1.remove(deviceId_device);
    }
    
    private void registerServiceCallback2(String deviceId_value) throws BrokerException {
	// This is for GET DEVICE VALUE
	// When a call is requested from uaal, uAAL REST will send it to this
	// RESTlet. Build a Message with a Query? and push it to InterIoT (?).
	// Post the response back to uAAL

	if(!callbacksService2.contains(deviceId_value))callbacksService2.add(deviceId_value);
	post(bridgeCallbackServiceValue + ":/deviceId", (req, res) -> {
	    log.debug("SERVICE CALLBACK -> Got request from uaal");
	    if(!callbacksService2.contains(req.params(":deviceId"))){
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
    
    private void unregisterServiceCallback2(String deviceId_value) {
	callbacksService2.remove(deviceId_value);
    }
    
    private Message ok(Message inMsg){
	Message responseMsg = createResponseMessage(inMsg);
	responseMsg.getMetadata().setStatus("OK");
	return createResponseMessage(responseMsg);
    }

}
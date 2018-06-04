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
import java.util.Properties;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.annotations.Bridge;
import eu.interiot.intermw.bridge.uaal.client.Body;
import eu.interiot.intermw.bridge.uaal.client.UAALClient;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.metadata.PlatformMessageMetadata;

@Bridge(platformType = "UniversAAL")
public class UAALBridge extends AbstractBridge {
    private final static String CALLBACK_PATH = "/receiver/";
    private final static String CALLBACK_PATH_CONTEXT = CALLBACK_PATH+"context/";
    private final static String CALLBACK_PATH_SERVICE = CALLBACK_PATH+"service/";
    private final static String DEFAULT_CALLER = "default";
    private final static String JSON = "application/json";
    private final static String TEXT = "text/plain";
    private final Logger log = LoggerFactory.getLogger(UAALBridge.class);
    private String url;
    private String usr;
    private String pwd;
    private String space;
    private String callback_host;
    private String callback_port;

    public UAALBridge(Configuration config, Platform platform)
	    throws MiddlewareException {
	super(config, platform);
	log.debug("UniversAAL bridge is initializing...");

	Properties properties = configuration.getProperties();
	try {
	    url = properties.getProperty("universaal.url");
	    usr = properties.getProperty("universaal.user");
	    pwd = properties.getProperty("universaal.password");
	    space = properties.getProperty("universaal.space");
	    callback_host = properties.getProperty("bridge-callback-host");
	    callback_port = properties.getProperty("bridge-callback-port");
	} catch (Exception e) {
	    throw new MiddlewareException(
		    "Failed to read UAALBridge configuration: "
			    + e.getMessage());
	}

	if (Strings.isNullOrEmpty(url) 
		|| Strings.isNullOrEmpty(usr)
		|| Strings.isNullOrEmpty(pwd)
		|| Strings.isNullOrEmpty(space)
		|| Strings.isNullOrEmpty(callback_host)
		|| Strings.isNullOrEmpty(callback_port)
		|| Integer.parseInt(callback_port)<0 ){
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
	 * single Service Caller for all our calls.
	 */

	log.info("Entering registerPlatform");

	if(!msg.getMessageConfig().isInitializeNewMessage()){//TODO INIT???
	    String bodySpace = Body.CREATE_SPACE
		    .replace(Body.SPACE, space)
		    .replace(Body.HOST, callback_host)
		    .replace(Body.PORT, callback_port)
		    .replace(Body.PATH, CALLBACK_PATH);
	    String bodyCaller = Body.CREATE_CALLER
		    .replace(Body.CALLER, DEFAULT_CALLER);

	    try {
		UAALClient.post(url + "spaces/", usr, pwd, JSON, bodySpace);
		UAALClient.post(url + "spaces/" + space + "/service/callers/", usr, pwd, JSON, bodyCaller);
	    } catch (Exception e) {
		log.error("Error sending request to uAAL at registerPlatform: " + e);
		e.printStackTrace();
	    }
	} // else Not INIT, don't create Space because it is already created
	
	log.info("Completed registerPlatform");

	Message responseMsg = createResponseMessage(msg);
	responseMsg.getMetadata().setStatus("OK"); //TODO Why not sending KO case in FIWARE?
	return createResponseMessage(responseMsg);
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

	try {
	    UAALClient.delete(url + "spaces/" + space, usr, pwd);
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at unregisterPlatform: " + e);
	    e.printStackTrace();
	}

	log.info("Completed unregisterPlatform");

	Message responseMessage = createResponseMessage(msg);
	responseMessage.getMetadata().setStatus("OK"); //TODO Why not sending KO case in FIWARE?
	return responseMessage;
    }

    // ------------------------------------------

    @Override
    public Message subscribe(Message msg)  {
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
	
	Message responseMsg = createResponseMessage(msg);
	String thingId="";//TODO
	String conversationId="";//TODO
	String body = Body.CREATE_SUBSCRIBER
		.replace(Body.SUBSCRIBER, conversationId)
		.replace(Body.HOST, callback_host)
		.replace(Body.PORT, callback_port)
		.replace(Body.PATH_C, CALLBACK_PATH_CONTEXT)
		.replace(Body.ID, getSuffix(thingId));

	try {
	    registerContextCallback(conversationId);
	    UAALClient.post(url + "spaces/" + space + "/context/subscribers/", usr, pwd, JSON, body);
//	    responseMessage.setPayload(responsePayload); //TODO Why does it need a payload in FIWARE????
	    responseMsg.getMetadata().setStatus("OK");
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at subscribe: " + e);
	    e.printStackTrace();
	    responseMsg.getMetadata().setStatus("KO");
	    responseMsg.getMetadata().setMessageType(MessageTypesEnum.ERROR);
	    responseMsg.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}

	log.info("Completed subscribe");

	return responseMsg;
    }

    @Override
    public Message unsubscribe(Message msg) throws Exception {
	// Cancels specified subscription created by the Subscribe.

	// Remove the CSubscriber created in subscribe

	log.info("Entering unsubscribe");
	
	Message responseMsg = createResponseMessage(msg);
	String conversationId = "";// TODO

	try {
	    //TODO unregisterContextCallback
	    UAALClient.delete(url + "spaces/" + space + "/context/subscribers/" + conversationId, usr, pwd);
//	    responseMessage.setPayload(responsePayload); //TODO Why does it need a payload in FIWARE????
	    responseMsg.getMetadata().setStatus("OK");
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at unsubscribe: " + e);
	    e.printStackTrace();
	    responseMsg.getMetadata().setStatus("KO");
	    responseMsg.getMetadata().setMessageType(MessageTypesEnum.ERROR);
	    responseMsg.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}

	log.info("Completed unsubscribe");

	return responseMsg;
    }

    // ------------------------------------------

    @Override
    public Message platformCreateDevice(Message msg) throws Exception {
	// An instruction for a platform to start managing (i.e. create) a new
	// device.

	// Replicate a thing from another platform into uAAL
	// Create a Device mydevice here that acts like a LDDI Exporter, with:
	//  SCallee
	//    DeviceService>controls>mydevice[GET]
	//      Return the current instance of mydevice
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

	log.info("Entering platformCreateDevice");
	
	Message responseMsg = createResponseMessage(msg);
	String thingId = "";// TODO
	String thingType = "http://ontology.universaal.org/PhThing.owl#Device";// TODO
	String bodyS = Body.CREATE_CALLEE
		.replace(Body.ID, getSuffix(thingId))
		.replace(Body.HOST, callback_host)
		.replace(Body.PORT, callback_port)
		.replace(Body.PATH_S, CALLBACK_PATH_SERVICE)
		.replace(Body.TYPE, thingType);
	String bodyC = Body.CREATE_PUBLISHER
		.replace(Body.ID, getSuffix(thingId));

	try {
	    //TODO registerServiceCallback
	    UAALClient.post(url + "spaces/" + space + "/service/callees/", usr, pwd, JSON, bodyS);
	    UAALClient.post(url + "spaces/" + space + "/context/publishers/", usr, pwd, JSON, bodyC);
//	    responseMessage.setPayload(responsePayload); //TODO Why does it need a payload in FIWARE????
	    responseMsg.getMetadata().setStatus("OK");
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at subscribe: " + e);
	    e.printStackTrace();
	    responseMsg.getMetadata().setStatus("KO");
	    responseMsg.getMetadata().setMessageType(MessageTypesEnum.ERROR);
	    responseMsg.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}

	log.info("Completed platformCreateDevice");

	return responseMsg;
    }

    @Override
    public Message platformUpdateDevice(Message msg) throws Exception {
	// An instruction for a platform to update information about a device it
	// is managing

	// Modify the status of a thing from another platform and notify uAAL
	// Modify the Device mydevice that will be handled by the Callee,
	// publish the change as event?
	// No need to change publisher because thingID is the same?

	log.info("Entering platformUpdateDevice");
	
	Message responseMsg = createResponseMessage(msg);
	String thingId = "";// TODO
	String thingType = "http://ontology.universaal.org/PhThing.owl#Device";// TODO
	String bodyS=Body.CREATE_CALLEE
		.replace(Body.ID, getSuffix(thingId))
		.replace(Body.HOST, callback_host)
		.replace(Body.PORT, callback_port)
		.replace(Body.PATH_S, CALLBACK_PATH_SERVICE)
		.replace(Body.TYPE, thingType);

	try {
	    //TODO registerServiceCallback?
	    UAALClient.put(url + "spaces/" + space + "/service/callees/"+getSuffix(thingId), usr, pwd, JSON, bodyS);
//	    responseMessage.setPayload(responsePayload); //TODO Why does it need a payload in FIWARE????
	    responseMsg.getMetadata().setStatus("OK"); //TODO Why is this not in FIWARE?
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at subscribe: " + e);
	    e.printStackTrace();
	    responseMsg.getMetadata().setStatus("KO");
	    responseMsg.getMetadata().setMessageType(MessageTypesEnum.ERROR);
	    responseMsg.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}

	log.info("Completed platformUpdateDevice");

	return responseMsg;
    }

    @Override
    public Message platformDeleteDevice(Message msg) throws Exception {
	// An instruction for a platform to stop managing (i.e. remove) a
	// device.

	// Remove the Device mydevice that was created in create(String thingID,
	// String thingType)

	log.info("Entering platformDeleteDevice");

	Message responseMsg = createResponseMessage(msg);
	String thingId = "";// TODO

	try {
	    //TODO unregisterServiceCallback
	    UAALClient.delete(url+"spaces/"+space+"/service/callees/"+getSuffix(thingId), usr, pwd);
	    UAALClient.delete(url + "spaces/" + space + "/context/publishers/" + getSuffix(thingId), usr, pwd);
//	    responseMessage.setPayload(responsePayload); //TODO Why does it need a payload in FIWARE????
	    responseMsg.getMetadata().setStatus("OK");
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at subscribe: " + e);
	    e.printStackTrace();
	    responseMsg.getMetadata().setStatus("KO");
	    responseMsg.getMetadata().setMessageType(MessageTypesEnum.ERROR);
	    responseMsg.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}

	log.info("Completed platformDeleteDevice");

	return responseMsg;
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
	String body = "";// TODO

	try {
	    //TODO registerDisposableServiceCallback
	    UAALClient.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, JSON, body);
	    // TODO What to do if response is asynchronous? What to return? Wait?
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at subscribe: " + e);
	    e.printStackTrace();
	    responseMsg.getMetadata().setStatus("KO");
	    responseMsg.getMetadata().setMessageType(MessageTypesEnum.ERROR);
	    responseMsg.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}

	log.info("Completed query");

	return responseMsg;
    }

    @Override
    public Message listDevices(Message msg) throws Exception {
	// Makes a query to a platform to get all devices managed by it, that it
	// deems discoverable

	log.info("Entering query");

	Message responseMsg = createResponseMessage(msg);
	String body = "";// TODO

	try {
	    //TODO registerDisposableServiceCallback
	    UAALClient.post(url + "spaces/" + space + "/service/callers/" + DEFAULT_CALLER, usr, pwd, JSON, body);
	    // TODO What to do if response is asynchronous? What to return? Wait?
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at subscribe: " + e);
	    e.printStackTrace();
	    responseMsg.getMetadata().setStatus("KO");
	    responseMsg.getMetadata().setMessageType(MessageTypesEnum.ERROR);
	    responseMsg.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}

	log.info("Completed query");

	return responseMsg;
    }

    @Override
    public Message observe(Message msg) throws Exception {
	// Pushes given observation message from InterMW to platform (bridge
	// acting as a publisher for platform)

	log.info("Entering observe");

	Message responseMsg = createResponseMessage(msg);
	String thingId = "";// TODO
	String body = "";// TODO

	try {
	    UAALClient.post(url + "spaces/" + space + "/context/publishers/" + getSuffix(thingId), usr, pwd, TEXT, body);
//	    responseMessage.setPayload(responsePayload); //TODO Why does it need a payload in FIWARE????
	    responseMsg.getMetadata().setStatus("OK"); //TODO Why is this not in FIWARE?
	} catch (Exception e) {
	    log.error("Error sending request to uAAL at subscribe: " + e);
	    e.printStackTrace();
	    responseMsg.getMetadata().setStatus("KO");
	    responseMsg.getMetadata().setMessageType(MessageTypesEnum.ERROR);
	    responseMsg.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}

	log.info("Completed observe");

	return responseMsg;
    }

    @Override
    public Message actuate(Message msg) throws Exception {
	// Same as Observe, except the message payload contains actuation
	// instructions.
	return null;
    }

    // ------------------------------------------

    @Override
    public Message error(Message msg) throws Exception {
	// Sends information about any error that occurred inside bridge or
	// inside InterMW.
	return null;
    }

    @Override
    public Message unrecognized(Message msg) throws Exception {
	// Custom message type which was not recognized by InterMW.
	return null;
    }

    // ------------------------------------------

    private String getSuffix(String interiotID) {
	int lastindex = interiotID.lastIndexOf("#");
	if (lastindex < 0) { // Now interiot ID ends with InterIoT.owl# but it used to end with /
	    lastindex = interiotID.lastIndexOf("/");
	}
	return interiotID.substring(lastindex + 1);
    }
    
    private void registerContextCallback(String conversationId) throws BrokerException {
	// When an event is notified, uAAL REST will send it to
	// CALLBACK_PATH_CONTEXT/ConversationId. Create there a RESTlet, and
	// whenever an event arrives, build a Message with an equivalent payload
	// and push it to InterIoT.
	
	// TODO UNREGISTER!!! vs CONTEXT/:conversationId { req.params(":conversationId") }

	post(CALLBACK_PATH_CONTEXT + conversationId, (req, res) -> {
	    log.debug("CONTEXT CALLBACK -> Got message from uaal");
	    Message messageForInterIoT = new Message();
	    // Metadata
	    PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
	    metadata.initializeMetadata();
	    metadata.setConversationId(conversationId);
	    metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
	    metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
	    // metadata.setSenderPlatformId(new EntityID(platform.getId().getId())); //TODO
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

}
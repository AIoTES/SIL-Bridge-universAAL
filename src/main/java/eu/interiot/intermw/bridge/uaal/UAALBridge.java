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

import com.google.common.base.Strings;
import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.bridge.uaal.client.Body;
import eu.interiot.intermw.bridge.uaal.client.UAALClient;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.exceptions.UnknownActionException;
import eu.interiot.intermw.commons.exceptions.UnsupportedActionException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.commons.model.SubscriptionId;
import eu.interiot.message.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.URI.URIManagerMessageMetadata;
import eu.interiot.message.exceptions.MessageException;
import eu.interiot.message.exceptions.payload.PayloadException;
import eu.interiot.message.metaTypes.PlatformMessageMetadata;
import eu.interiot.message.utils.INTERMWDemoUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.Set;

import static spark.Spark.post;

@eu.interiot.intermw.bridge.annotations.Bridge(platformType = "UniversAAL")
public class UAALBridge extends AbstractBridge {
    private final Logger log = LoggerFactory.getLogger(UAALBridge.class);
    private final static String PROPERTIES_PREFIX = "uaal-";

    private final static String JSON = "application/json";
    private final static String TEXT = "text/plain";
    private final static String UAAL_URI_PREFIX = "http://ontology.universAAL.org/InterIoT.owl#";
    //    private final static String INTERIOT_URI_THING_PREFIX = "http://inter-iot.eu/dev/";
//    private final static String INTERIOT_URI_OBS_PREFIX = "http:inter-iot.eu/obs/";
//    private final static String DEMO_THING_ID = "scale"; 
    private final static String SINGLE_CALLER = "default";
//    private final static String DEMO_SINGLE_SUBSCRIBER = "default"; 

    private final static String KEY_URL = "url";
    private final static String KEY_USR = "usr";
    private final static String KEY_PWD = "pwd";
    private final static String KEY_CALLBACK_HOST = "callback-url";
    private final static String KEY_SPACE = "space";

    private final static String CALLBACK_PATH = "/receiver/";
    private final static String CALLBACK_CONTEXT_PATH = "/receiver/context/";
    //    private final static String CALLBACK_SERVICE_PATH = "/receiver/service/";

    //private Publisher<Message> publisher;
    //private Broker broker;
    //private MwFactory mwFactory;

    private String prop_URL;
    private String prop_USR;
    private String prop_PWD;
    private String prop_CALLBACK_HOST;
    private String prop_SPACE;
    private int callbackPort;

    // TODO Try to handle internally as many Exceptions as possible
    public UAALBridge(Configuration configuration, Platform platform) throws MiddlewareException {
        super(configuration, platform);
        log.debug("UAALBridge is initializing...");
//	Properties properties = configuration.getProperties(PROPERTIES_PREFIX);
        // @see OrionBridge for an example of a bridge for FIWARE

        // There is a usage example in the project: example.interiot.consumer
        // To make use of the OM2MBridge you have to change this line
        // interiot.addPlatform(myPlatformId, null, PlatformType.FIWARE);
        // To this
        // interiot.addPlatform(myPlatformId, null, PlatformType.ONEM2M);
        // bridge configuration can be set in example.bridge.properties file

        // TODO commons context and broker context. This is very confusing
        //mwFactory = Context.mwFactory();
        //broker = eu.interiot.intermw.comm.broker.Context.getBroker();

        // Initialize uAAL Context
//	prop_URL=platform.getBaseURL();
//	if(prop_URL==null || prop_URL.isEmpty()){
//	    prop_URL = properties.getProperty(KEY_URL, DEFAULT_URL);
//	}
//	if(!prop_URL.endsWith("/uaal/")){
//	    prop_URL=prop_URL+"/uaal/";
//	}
//	prop_USR = properties.getProperty(KEY_USR, DEFAULT_USR);
//	prop_PWD = properties.getProperty(KEY_PWD, DEFAULT_PWD);
//	prop_CALLBACK_HOST = properties.getProperty(KEY_CALLBACK_HOST, DEFAULT_CALLBACK_HOST);
//	prop_SPACE = properties.getProperty(KEY_SPACE, DEFAULT_SPACE);
        // DEMO

        try {
            prop_URL = configuration.getProperty("universaal.url");
            prop_USR = configuration.getProperty("universaal.user");
            prop_PWD = configuration.getProperty("universaal.password");
            prop_SPACE = configuration.getProperty("universaal.space");
            prop_CALLBACK_HOST = configuration.getProperty("bridge-callback-host");
            callbackPort = Integer.parseInt(configuration.getProperty("bridge-callback-port"));
        } catch (Exception e) {
            throw new MiddlewareException("Failed to read UAALBridge configuration: " + e.getMessage());
        }

        if (Strings.isNullOrEmpty(prop_URL) || Strings.isNullOrEmpty(prop_USR) || Strings.isNullOrEmpty(prop_PWD) ||
                Strings.isNullOrEmpty(prop_SPACE) || Strings.isNullOrEmpty(prop_CALLBACK_HOST)) {
            throw new MiddlewareException("Invalid UAALBridge configuration: empty values.");
        }

        log.debug("UAALBridge has been initialized successfully.");
    }

    private void registerPlatform(boolean SYS_INIT) {
        log.debug("registerPlatform");
        String body = Body.POST_SPACES
                .replace(Body.REPLACE_SPACE, prop_SPACE)
                .replace(Body.REPLACE_HOST, prop_CALLBACK_HOST)
                .replace(Body.REPLACE_PORT, Integer.toString(callbackPort))
                .replace(Body.REPLACE_PATH, CALLBACK_PATH);

        // Default SCaller
        String bodyD = Body.POST_SPACES_S_SERVICE_CALLERS
                .replace(Body.REPLACE_CALLER, SINGLE_CALLER);

        if (!SYS_INIT) {
            try {
                System.out.println("CREATING CONNECTION TO UAAL: " + prop_URL + "spaces/" + prop_USR + prop_PWD);
                log.debug("CREATING CONNECTION TO UAAL: " + prop_URL + "spaces/" + prop_USR + prop_PWD);
                UAALClient.post(prop_URL + "spaces/", prop_USR, prop_PWD, JSON, body);
                UAALClient.post(prop_URL + "spaces/" + prop_SPACE + "/service/callers/", prop_USR, prop_PWD, JSON, bodyD);
            } catch (Exception e) {
                log.error("registerPlatform " + e);
                e.printStackTrace();
            }
        }
    }

    private void create(String thingID, Message message, boolean SYS_INIT) throws BridgeException {
        log.debug("create");
        // Replicate a thing from another platform into uAAL
        //
        // Create a Device mydevice here that acts like a LDDI Exporter, with:
        //
        // SCallee
        //   DeviceService>controls>mydevice[GET]
        //     Return the current instance of mydevice
        //   DeviceService>controls>mydevice[CHANGE(mydevice*)]
        //     Changes current instance with the same instance with different props values
        //   DeviceService>controls>mydevice[REMOVE] ?
        //     Removes current instance? Notify INTER-IoT subscribers? Remove self?
        //
        // CPublisher
        //   CEP[sub=mydev, pred=*, obj=*]
        //     Publisher to be used by update(String thingId, Message message)

        // The Publisher: A controller that can publish anything about this thing
        // DEMO: Not the best way to do it, but it will be replaced by IPSM later anyway...
        String bodyC;
        Resource entity = message.getPayload().getJenaModel().getResource(thingID);
        if (entity.hasProperty(RDF.type, "http://ontology.universaal.org/PersonalHealthDevice.owl#WeighingScale")) {
            log.debug("weight scale");
            bodyC = Body.POST_SPACES_S_CONTEXT_PUBLISHERS_2
                    .replace(Body.REPLACE_ID, getInteriotSuffix(thingID))
                    .replace(Body.REPLACE_TYPE, "http://ontology.universaal.org/PersonalHealthDevice.owl#WeighingScale")
                    .replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX);
        } else if (entity.hasProperty(RDF.type, "http://ontology.universAAL.org/PersonalHealthDevice.owl#BloodPressureSensor")) {
            log.debug("blood presure");
            bodyC = Body.POST_SPACES_S_CONTEXT_PUBLISHERS_2
                    .replace(Body.REPLACE_ID, getInteriotSuffix(thingID))
                    .replace(Body.REPLACE_TYPE, "http://ontology.universAAL.org/PersonalHealthDevice.owl#BloodPressureSensor")
                    .replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX);
        } else {
            log.debug("generic device");
            bodyC = Body.POST_SPACES_S_CONTEXT_PUBLISHERS
                    .replace(Body.REPLACE_ID, getInteriotSuffix(thingID))
                    .replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX);
        }

        // The Callee profiles: for GET, CHANGE (and REMOVE?)
//	String bodyS=Body.POST_SPACES_S_SERVICE_CALLEES
//		.replace(Body.REPLACE_ID, demoSuffixInteriot(thing.getThingId().getId()))
//		.replace(Body.REPLACE_HOST, prop_CALLBACK_HOST)
//		.replace(Body.REPLACE_PORT, prop_CALLBACK_PORT)
//		.replace(Body.REPLACE_PATH_S, CALLBACK_SERVICE_PATH)
//		.replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX);
        // TODO When a call is requested from uaal, uAAL REST will send it to CALLBACK_SERVICE....
        // Create here a RESTlet on that URL with Spark, and whenever a call arrives,
        // build a Message with a Query and push it to InterIoT (?). Post the response back to uAAL

        try {
            log.debug("Posting to uaal " + prop_URL + "spaces/" + prop_SPACE + "/context/publishers/");
            if (!SYS_INIT) {
                UAALClient.post(prop_URL + "spaces/" + prop_SPACE + "/context/publishers/", prop_USR, prop_PWD, JSON, bodyC);
            }
//	    UAALClient.post(prop_URL+"spaces/"+prop_SPACE+"/service/callees/", prop_USR, prop_PWD, JSON, bodyS);
        } catch (Exception e) {
            log.error("create" + e);
            throw new BridgeException(e);
        }
    }

//    public String read() throws BridgeException {
//	// Create a ServiceRequest asking for the equivalent query(?) from a DefaultServiceCaller
//	// SCaller
//	//   DeviceService>controls>URI=query.getEntityID + MY_URI=query.getType
//	//
//	// Return a parsed Thing from the returned Device instance
//	
//	// DEMO This method may never be called in the demo: There will be no discovery, 
//	// all the things referred to in all methods will have an ID that was given to them "manually"
//	// Even if used, I can't get any attributes from thing, other than ID, so the only meaningful 
//	// query I could build would be "get me device with ID", which is a bit of a problem right now in uAAL.
//	
////	if (query.getEntityId()!=null && !query.getEntityId().isEmpty()){
////	    //Request services that return this thing
////	}else if(query.getType()!=null && !query.getType().isEmpty()){
//	    //Request services that return things of type Device
//	    String body=Body.POST_SPACES_S_SERVICE_CALLERS_C
//		    .replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX);
//	    
//		try {
//		    String result=UAALClient.post(prop_URL+"spaces/"+prop_SPACE+"/service/callers/"+DEMO_SINGLE_CALLER, prop_USR, prop_PWD, TEXT, body);
//		    if (result!=null){
//			// TODO Parse and convert to thing
//			// DEMO Again, since I cannot set attributes to Thing, the Thing to return will 
//			// only have to contain one meaningful property: ID
//			return INTERIOT_URI_THING_PREFIX+DEMO_THING_ID;
//		    }else{
//			return null;
//		    }
//		} catch (Exception e) {
//		    throw new BridgeException(e);
//		}
////	}
//    }

    private void update(String thingId, Message message) throws BridgeException {
        log.debug("update");
        // Modify the status of a thing from another platform and notify uAAL
        //
        // Modify the Device mydevice that will be handled by the Callee, publish the change as event
        //
        // DEMO: Not the best way to do it, but it will be replaced by IPSM later anyway...
        String body = "";
        String uuid = Integer.toHexString(new Random(System.currentTimeMillis()).nextInt());
        Resource entity = message.getPayload().getJenaModel().getResource(thingId);
        if (entity.hasProperty(RDF.type, "http://ontology.universaal.org/PersonalHealthDevice.owl#WeighingScale")) {
            log.debug("weight scale");
            Property prop = message.getPayload().getJenaModel().getProperty("http://inter-iot.eu/GOIoTP#hasValue");
            NodeIterator list = message.getPayload().getJenaModel().listObjectsOfProperty(prop);
            if (list.hasNext()) {
                RDFNode o = list.next();
                body = Body.POST_SPACES_S_CONTEXT_PUBLISHERS_P_W
                        .replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX)
                        .replace(Body.REPLACE_ID, getInteriotSuffix(thingId))
                        .replace(Body.REPLACE_TIME, "" + System.currentTimeMillis())
                        .replace(Body.REPLACE_UUID, uuid)
                        .replace(Body.REPLACE_OBJ, o.asLiteral().getLexicalForm());
            }
        } else if (entity.hasProperty(RDF.type, "http://ontology.universAAL.org/PersonalHealthDevice.owl#BloodPressureSensor")) {
            log.debug("blood pressure (disabled)");
//		Property prop = message.getPayload().getJenaModel().getProperty("http://inter-iot.eu/GOIoTP#hasValue");
//		NodeIterator list = message.getPayload().getJenaModel().listObjectsOfProperty(prop);
//		if(list.hasNext()){
//		    RDFNode o = list.next();
//		    body=Body.POST_SPACES_S_CONTEXT_PUBLISHERS_P_BP
//			.replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX)
//			.replace(Body.REPLACE_ID, getInteriotSuffix(thingId))
//			.replace(Body.REPLACE_TIME, ""+System.currentTimeMillis())
//			.replace(Body.REPLACE_UUID, uuid)
//			.replace(Body.REPLACE_OBJ, o.asLiteral().getLexicalForm())
//			.replace(Body.REPLACE_OBJ_2, o.asLiteral().getLexicalForm());
//		}
        } else {
            log.debug("generic device");
            Property prop = message.getPayload().getJenaModel().getProperty("http://inter-iot.eu/GOIoTP#hasValue");
            NodeIterator list = message.getPayload().getJenaModel().listObjectsOfProperty(prop);
            if (list.hasNext()) {
                RDFNode o = list.next();
                if (o.isLiteral()) {
                    log.debug("literal");
                    body = Body.POST_SPACES_S_CONTEXT_PUBLISHERS_P_D
                            .replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX)
                            .replace(Body.REPLACE_ID, getInteriotSuffix(thingId))
                            .replace(Body.REPLACE_TIME, "" + System.currentTimeMillis())
                            .replace(Body.REPLACE_UUID, uuid)
                            .replace(Body.REPLACE_OBJ, "\"" + o.asLiteral().getLexicalForm() + "\"^^" + o.asLiteral().getDatatypeURI());
                } else {
                    log.debug("value");
                    body = Body.POST_SPACES_S_CONTEXT_PUBLISHERS_P_D
                            .replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX)
                            .replace(Body.REPLACE_ID, getInteriotSuffix(thingId))
                            .replace(Body.REPLACE_TIME, "" + System.currentTimeMillis())
                            .replace(Body.REPLACE_UUID, uuid)
                            .replace(Body.REPLACE_OBJ, o.asResource().getURI())
                            .replace(Body.REPLACE_TYPE_OBJ, message.getPayload().getJenaModel().getProperty(o.asResource(), RDF.type).getObject().asResource().getURI());
                }
            }
        }
        try {
            if (!body.isEmpty()) {
                log.debug("Posting to uaal " + prop_URL + "spaces/" + prop_SPACE + "/context/publishers/" + getInteriotSuffix(thingId));
                UAALClient.post(prop_URL + "spaces/" + prop_SPACE + "/context/publishers/" + getInteriotSuffix(thingId), prop_USR, prop_PWD, TEXT, body);
            }
        } catch (Exception e) {
            log.error("update " + e);
            throw new BridgeException(e);
        }
    }

    public void delete(String thingId) throws BridgeException {
        log.debug("delete");
        // Remove the Device mydevice that was created in create(String thingID, String thingType)

        try {
            log.debug("Posting to uaal " + prop_URL + "spaces/" + prop_SPACE + "/context/publishers/" + getInteriotSuffix(thingId));
            UAALClient.delete(prop_URL + "spaces/" + prop_SPACE + "/context/publishers/" + getInteriotSuffix(thingId), prop_USR, prop_PWD);
//	    UAALClient.delete(prop_URL+"spaces/"+prop_SPACE+"/service/callees/"+getInteriotSuffix(thingId), prop_USR, prop_PWD);
        } catch (Exception e) {
            log.error("delete " + e);
            throw new BridgeException(e);
        }
    }

    private void subscribe(String thingId, String conversationId, boolean SYS_INIT)
            throws BridgeException {
        log.debug("subscribe");
        // Subscribe to changes from a thing in uAAL and forward them to other platforms
        //
        // Create a CSubscriber for this thing
        //
        // CSubscriber
        //   CEP[sub=thing.getThingID, pred=*, obj=*]

        // ConversationId IS NOT SubscriptionId. It appears I have to build the SubscriptionId
        // and return it somehow TBD. ConversationId could perhaps be more akin to a ContextProvider URI...

        // The Subscriber: Receive anything about this device
        String body = Body.POST_SPACES_S_CONTEXT_SUBSCRIBERS_1
                .replace(Body.REPLACE_SUBSCRIBER, conversationId)
                .replace(Body.REPLACE_HOST, prop_CALLBACK_HOST)
                .replace(Body.REPLACE_PORT, Integer.toString(callbackPort))
                .replace(Body.REPLACE_PATH_C, CALLBACK_CONTEXT_PATH)
                .replace(Body.REPLACE_ID, getInteriotSuffix(thingId))
                .replace(Body.REPLACE_PREFIX, UAAL_URI_PREFIX);

        try {
            registerContextCallback(conversationId);
            log.debug("Posting to uaal" + prop_URL + "spaces/" + prop_SPACE + "/context/subscribers/");
            if (!SYS_INIT) {
                UAALClient.post(prop_URL + "spaces/" + prop_SPACE + "/context/subscribers/", prop_USR, prop_PWD, JSON, body);
            }
        } catch (Exception e) {
            log.error("subscribe " + e);
            throw new BridgeException(e);
        }
    }

//    private void subscribeById(String conversationId,
//	    String... thingIds) throws BridgeException {
//	// No idea what it does, it depends on ConverstaionID and it is not clear
//	//
//	// For now just batch call subscribe(String thingId, String conversationId)
//	for(String t:thingIds){
//	    subscribe(t, conversationId);
//	}
//    }

    private void unsubscribe(SubscriptionId subscriptionId) throws BridgeException {
        log.debug("unsubscribe");
        // Remove the CSubscriber created in subscribe

        try {
            log.debug("Posting to uaal " + prop_URL + "spaces/" + prop_SPACE + "/context/subscribers/" + subscriptionId.getId());
            UAALClient.delete(prop_URL + "spaces/" + prop_SPACE + "/context/subscribers/" + subscriptionId.getId(), prop_USR, prop_PWD);
        } catch (Exception e) {
            log.error("unsubscribe " + e);
            throw new BridgeException(e);
        }
    }

    // UTIL METHODS

    private void registerContextCallback(String conversationId) throws BrokerException {
        log.debug("registerContextCallback");
        // When an event is notified, uAAL REST will send it to CALLBACK_CONTEXT....
        // Create first a RESTlet on that URL with Spark, and whenever an event arrives,
        // build a Message with a Payload and push it to InterIoT.

        //publisher = broker.createPublisher(BrokerTopics.BRIDGE_IPSM.getTopicName()+ "_" + platform.getId().getId(), Message.class);

        post(CALLBACK_CONTEXT_PATH + conversationId,
                (req, res) -> {
                    log.debug("Got message from uaal");
                    Message callbackMessage = new Message();
                    // Metadata
                    PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
                    metadata.initializeMetadata();
                    metadata.setConversationId(conversationId);
                    metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
                    metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
                    metadata.setSenderPlatformId(new EntityID(platform.getId().getId()));
                    callbackMessage.setMetadata(metadata);
                    // Payload (Parse event and get values)
                    String event = req.body();
                    Model eventModel = ModelFactory.createDefaultModel();
                    eventModel.read(new ByteArrayInputStream(event.getBytes()), null, "TURTLE");
                    // Uncomment and use this payload if IPSM does not translate automatically
//		    String observationURIIdentifier=demoGetMeasurementURI(eventModel);
//		    Calendar observationTime=demoGetTimestamp(eventModel);
//		    String sensorURIIdentifier=demoGetSubject(eventModel);
//		    float observationValue=demoGetValue(eventModel);
//		    MessagePayload payload = INTERMWDemoUtils.createWeightObservationPayload(
//			    observationURIIdentifier, observationTime, 
//			    sensorURIIdentifier, observationValue); 
                    callbackMessage.setPayload(new MessagePayload(eventModel));
                    // Send to InterIoT
                    log.debug("Publish message to interoit");
                    publisher.publish(callbackMessage);
                    log.debug("After publish");
                    res.status(200);
                    return "";
                });
    }

    private String getInteriotSuffix(String interiotID) {
        int lastindex = interiotID.lastIndexOf("#");
        if (lastindex < 0) { // Now interiot ID ends with InterIoT.owl# but it used to end with /
            lastindex = interiotID.lastIndexOf("/");
        }
        return interiotID.substring(lastindex + 1);
    }

//    private String demoSuffixUaal(String uaalID){
//	return uaalID.substring(uaalID.lastIndexOf("#")+1);
//    }
//
//    private Float demoGetValue(Model eventModel) {
//	Property p = eventModel.getProperty("http://ontology.universaal.org/Measurement.owl#value");
//	StmtIterator stmts = eventModel.listStatements(null, p, (RDFNode) null);
//	if (stmts.hasNext()) {
//	    Statement s = stmts.next();
//	    return s.getFloat();
//	}
//	return null;
//    }
//
//    private String demoGetSubject(Model eventModel) {
//	Property p = eventModel.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#subject");
//	StmtIterator stmts = eventModel.listStatements(null, p, (RDFNode) null);
//	if (stmts.hasNext()) {
//	    Statement s = stmts.next();
//	    if(s.getObject().isResource()){
//	    String uri = s.getObject().asResource().getURI();
//	    return INTERIOT_URI_THING_PREFIX+uri.substring(uri.indexOf("#") + 1);
//	    }
//	}
//	return null;
//    }
//
//    private Calendar demoGetTimestamp(Model eventModel) {
//	Property p = eventModel.getProperty("http://ontology.universAAL.org/Context.owl#hasTimestamp");
//	StmtIterator stmts = eventModel.listStatements(null, p, (RDFNode) null);
//	if (stmts.hasNext()) {
//	    Statement s = stmts.next();
//	    Calendar c = Calendar.getInstance();
//	    c.setTimeInMillis(s.getLong());
//	    return c;
//	}
//	return null;
//    }
//
//    private String demoGetMeasurementURI(Model eventModel) {
//	Property p = eventModel.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#object");
//	StmtIterator stmts = eventModel.listStatements(null, p, (RDFNode) null);
//	if (stmts.hasNext()) {
//	    Statement s = stmts.next();
//	    if(s.getObject().isResource()){
//	    String uri = s.getObject().asResource().getURI();
//	    return INTERIOT_URI_OBS_PREFIX+uri.substring(uri.indexOf("#") + 1);
//	    }
//	}
//	return null;
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void send(Message message) throws BridgeException {
        log.debug("send");
        Set<URIManagerMessageMetadata.MessageTypesEnum> messageTypesEnumSet = message.getMetadata().getMessageTypes();
        try {
            if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.THING_REGISTER)) {
                log.debug("THING_REGISTER");
                // TODO Discuss with pawel EntityTypeDevice to enum
                Set<String> entityIds = INTERMWDemoUtils.getEntityIDsFromPayload(message.getPayload(),
                        INTERMWDemoUtils.EntityTypeDevice);

                // XXX Discuss with matevz, flavio At this point we can
                // do two things, iterate over the list and do atomic
                // creations or introduce a bulk creation method
                // For the moment. It is implemented the first option,
                // in order to preserve the Bridge API TODO consisder
                // changing it

                for (String entityId : entityIds) {
                    // TODO Loop over the payload to include possible
                    // observations in the creation or a schema of the
                    // 'Thing'

                    if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.SYS_INIT)) {
                        create(entityId, message, true);
                    } else {
                        create(entityId, message, false);
                    }
                }
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.THING_UNREGISTER)) {
                log.debug("THING_UNREGISTER");
                Set<String> entityIds = INTERMWDemoUtils.getEntityIDsFromPayload(message.getPayload(),
                        INTERMWDemoUtils.EntityTypeDevice);
                for (String entityId : entityIds) {
                    delete(entityId);
                }
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.THING_UPDATE)) {
                log.debug("THING_UPDATE");
                // FIXME This implementation assumes that only one
                // attribute is updated at once
                // FIXME This code is for the demo only. It will break
                // otherwise.
                if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION)) {
                    Set<String> entities = INTERMWDemoUtils.getEntityIDsFromPayload(message.getPayload(),
                            INTERMWDemoUtils.EntityTypeDevice);
                    if (entities.isEmpty())
                        throw new BridgeException("No entities of type Device found in the Payload");
                    String entity = entities.iterator().next();

                    System.out.println("Updating thing:" + entity);
                    update(entity, message);
                }

            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.QUERY)) {
                log.debug("QUERY");
                // XXX Improve the Query definition

                Set<String> entities = INTERMWDemoUtils.getEntityIDsFromPayload(message.getPayload(),
                        INTERMWDemoUtils.EntityTypeDevice);
                if (entities.isEmpty())
                    throw new PayloadException("No entities of type Device found in the Payload");

                throw new BridgeException("QUERY not implemented");
				/*String entity = entities.iterator().next();

				Query q = new DefaultQueryImpl(new ThingId(entity));
				read(q);*/

            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.SUBSCRIBE)) {
                log.debug("SUBSCRIBE");
                // Assuming the subscribing to one thing
                Set<String> entities = INTERMWDemoUtils.getEntityIDsFromPayload(message.getPayload(),
                        INTERMWDemoUtils.EntityTypeDevice);
                if (entities.isEmpty())
                    throw new PayloadException("No entities of type Device found in the Payload");

                String entity = entities.iterator().next();
                if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.SYS_INIT)) {
                    subscribe(entity,
                            message.getMetadata().getConversationId().orElse(""), true);
                } else {
                    subscribe(entity, message.getMetadata().getConversationId().orElse(""), false);
                }

            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.UNSUBSCRIBE)) {
                log.debug("UNSUBSCRIBE");
                String conversationID = message.getMetadata().getConversationId().orElse("");
                unsubscribe(new SubscriptionId(conversationID));
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.DISCOVERY)) {
                throw new UnsupportedActionException("The action DISCOVERY is currently unsupported");
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.PLATFORM_REGISTER)) {
                log.debug("PLATFORM_REGISTER");
                if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.SYS_INIT)) {
                    registerPlatform(true);
                } else {
                    registerPlatform(false);
                }
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.PLATFORM_UNREGISTER)) {
                //We should not throw an exception here,
                // because the bridge should compose a reply message and sent it upstream
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.UNRECOGNIZED)) {
                throw new UnknownActionException(
                        "The action is labelled as UNRECOGNIZED and thus is unprocessable by component "
                                + this.getClass().getName() + " in platform " + platform.getId().getId());
            } else {
                throw new UnknownActionException(
                        "The message type is not properly handled and can't be processed"
                                + this.getClass().getName() + " in platform " + platform.getId().getId());
            }
            // TODO For now we cereate a geenric response message. Think
            // about sending a specific status

            Message responseMessage = createResponseMessage(message);
            try {
                log.debug("Sending response");
                publisher.publish(responseMessage);
            } catch (BrokerException e) {
                log.error("Error publishing response");
                throw new MessageException("error publishing response", e);
            }

        } catch (MessageException | UnsupportedActionException | UnknownActionException e) {
            log.error("send " + e);
            throw new BridgeException(e.toString());
        }


    }
}

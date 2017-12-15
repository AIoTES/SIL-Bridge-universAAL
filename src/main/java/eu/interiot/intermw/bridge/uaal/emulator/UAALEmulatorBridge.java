/**
 * INTER-IoT. Interoperability of IoT Platforms.
 * INTER-IoT is a R&D project which has received funding from the European
 * Union’s Horizon 2020 research and innovation programme under grant
 * agreement No 687283.
 * <p>
 * Copyright (C) 2016-2018, by (Author's company of this file):
 * - XLAB d.o.o.
 * <p>
 * <p>
 * For more information, contact:
 * - @author <a href="mailto:flavio.fuart@xlab.si">Flavio Fuart</a>
 * - Project coordinator:  <a href="mailto:coordinator@inter-iot.eu"></a>
 */
package eu.interiot.intermw.bridge.uaal.emulator;

import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.comm.broker.exceptions.BrokerException;
import eu.interiot.intermw.commons.Context;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.exceptions.UnknownActionException;
import eu.interiot.intermw.commons.exceptions.UnsupportedActionException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.interfaces.MwFactory;
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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.UUID;

import static spark.Spark.post;

@eu.interiot.intermw.bridge.annotations.Bridge(platformType = "UniversAALEmulated")
public class UAALEmulatorBridge extends AbstractBridge {
    private final String BASE_PATH = "http://localhost";

    private final Logger log = LoggerFactory.getLogger(UAALEmulatorBridge.class);

    private MwFactory mwFactory;
    private int callbackPort;

    public UAALEmulatorBridge(Configuration configuration, Platform platform) throws MiddlewareException {
        super(configuration, platform);
        // FIXME make bridges multi-instance
        // client = new
        // OrionApiClient(configuration.getProperties(PROPERTIES_PREFIX));
        mwFactory = Context.mwFactory();
        callbackPort = Integer.parseInt(configuration.getProperty("bridge-callback-port"));
    }

    private void create(String entityId) throws BridgeException {
        callEmulatedPlafrorm(null, URIManagerMessageMetadata.MessageTypesEnum.THING_REGISTER, entityId, null, null);
    }

	/*
	private Thing read(Query query) throws BridgeException {
		try {
			EmulatorQuery _query = (EmulatorQuery) query;
			log.info("Read with query: {}", ((EmulatorQuery) query).getOptionsInCommaSeparatedString());

			return null;
		} catch (Exception e) {
			throw new BridgeException(e);
		}
	}*/

    private void update(String thingId, Message message) throws BridgeException, MessageException {
        // FIXME HACK: getAttributeFromPayload assumes that
        // there is an observation inside the payload
        // It will not work otherwise
        String key = INTERMWDemoUtils.getAttrKeyToUpdateFromPayload(message.getPayload());
        String value = INTERMWDemoUtils.getAttrValueToUpdateFromPayload(message.getPayload());
        String type = INTERMWDemoUtils.getAttrTypeToUpdateFromPayload(message.getPayload());

        callEmulatedPlafrorm(null, URIManagerMessageMetadata.MessageTypesEnum.THING_UPDATE, thingId, value, null);
    }

    /**
     * @param thingId
     */
    private void delete(String thingId) throws BridgeException {
        callEmulatedPlafrorm(null, URIManagerMessageMetadata.MessageTypesEnum.THING_UNREGISTER, thingId, null, null);
    }

    private void subscribe(String thingId, String conversationId) throws BridgeException {

        try {
            log.debug("Subscribe to thing " + thingId);
            UUID randomUniqueEndpoint = UUID.randomUUID();
            String generateURLCallback = BASE_PATH + ":" + callbackPort + "/" + randomUniqueEndpoint;
            log.debug("Platform : " + generateURLCallback);

            //publisher = broker.createPublisher(BrokerTopics.BRIDGE_IPSM.getTopicName() + "_" + platform.getId().getId(), Message.class);

            post("/" + randomUniqueEndpoint, (req, res) -> {
                Message callbackMessage = new Message();
                PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
                metadata.initializeMetadata();
                // conversationID must be overriden, because the answer to a
                // subscription must share the same cID
                metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
                metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
                //metadata.setSenderPlatformId(URI.create(platform.getId().getId())); old MESSAGING code
                metadata.setSenderPlatformId(new EntityID(platform.getId().getId()));
                metadata.setConversationId(conversationId);
                callbackMessage.setMetadata(metadata);
                //get JSON
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(req.body());
                log.info("Received a POST http message: " + jsonObject.toString());

                String value = (String) jsonObject.get("value");
                //TODO value contains string representation of message payload. Find a way to cast it to MessagePayload.
                //	In short, we want to add String value to the message as its payload (it is an example UniversAAL message)
                //	Or even better said, we want to deserialize the string into MessagePayload object (are there any methods in Messaging for doing that?)
                //messagePayload.getJenaModel().add(value);

                Model m = ModelFactory.createDefaultModel();
                InputStream inStream = new ByteArrayInputStream(value.getBytes());
                RDFDataMgr.read(m, inStream, Lang.N3);
                MessagePayload messagePayload = new MessagePayload(m);

                callbackMessage.setPayload(messagePayload);

                publisher.publish(callbackMessage);

                return "200-OK";

            });
            callEmulatedPlafrorm(conversationId, URIManagerMessageMetadata.MessageTypesEnum.SUBSCRIBE, thingId, null, generateURLCallback);
        } catch (Exception e) {
            throw new BridgeException(e);
        }


    }

    private void callEmulatedPlafrorm(String conversationId, URIManagerMessageMetadata.MessageTypesEnum action, String thing, String value, String callBackUrl) throws BridgeException {
        try {
            log.info("Perform action {} for {}={}, conversation {}", action.toString(), thing, value, conversationId);


            //Create new platform message in JSON: Action --> ??, Thing ==> message, listener ==> callbackUrl
            //Put JSON into message body and send it downstream to platfomr baseUrl

            JSONObject json = new JSONObject();
            json.put("action", action.toString());
            json.put("thing", thing);
            json.put("value", value);

            if (conversationId == null)
                conversationId = "";
            json.put("conversationId", conversationId);

            if (callBackUrl == null)
                callBackUrl = "";
            json.put("callbackUrl", callBackUrl);

            CloseableHttpClient httpClient = HttpClientBuilder.create().build();

            try {
                HttpPost request = new HttpPost(platform.getBaseURL());
                StringEntity params = new StringEntity(json.toString());
                request.addHeader("content-type", "application/json");
                request.setEntity(params);
                CloseableHttpResponse responseUpstream = httpClient.execute(request);

                String statusOfResponse = responseUpstream.getStatusLine().toString();
                responseUpstream.close();

                log.info("Sent POST message " + thing + " with callbackUrl " + callBackUrl + " to platform on url " + platform.getBaseURL() + " with status " + statusOfResponse);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String exceptionStackTrace = sw.toString();

                log.error("Exception happened! " + exceptionStackTrace);
            } finally {
                httpClient.close();
            }

        } catch (Exception e) {
            throw new BridgeException(e);
        }

    }

    private void unsubscribe(SubscriptionId subscriptionId) throws BridgeException {
        log.info("Unsubscribe {} ", subscriptionId.getId());
        callEmulatedPlafrorm(subscriptionId.getId(), URIManagerMessageMetadata.MessageTypesEnum.UNSUBSCRIBE, null, null, null);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void send(Message message) throws BridgeException {
        Set<URIManagerMessageMetadata.MessageTypesEnum> messageTypesEnumSet = message.getMetadata().getMessageTypes();
        try {
            if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.THING_REGISTER)) {
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
                    create(entityId);
                }
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.THING_UNREGISTER)) {
                Set<String> entityIds = INTERMWDemoUtils.getEntityIDsFromPayload(message.getPayload(),
                        INTERMWDemoUtils.EntityTypeDevice);
                for (String entityId : entityIds) {
                    delete(entityId);
                }
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.THING_UPDATE)) {
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
                // Assuming the subscribing to one thing
                Set<String> entities = INTERMWDemoUtils.getEntityIDsFromPayload(message.getPayload(),
                        INTERMWDemoUtils.EntityTypeDevice);
                if (entities.isEmpty())
                    throw new PayloadException("No entities of type Device found in the Payload");

                String entity = entities.iterator().next();
                subscribe(entity,
                        message.getMetadata().getConversationId().orElse(""));

            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.UNSUBSCRIBE)) {
                String conversationID = message.getMetadata().getConversationId().orElse("");
                unsubscribe(new SubscriptionId(conversationID));
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.DISCOVERY)) {
                throw new UnsupportedActionException("The action DISCOVERY is currently unsupported");
            } else if (messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.PLATFORM_REGISTER)) {

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
                publisher.publish(responseMessage);
            } catch (BrokerException e) {
                log.error("Error publishing response");
                throw new MessageException("error publishing response", e);
            }

        } catch (MessageException | UnsupportedActionException | UnknownActionException e) {
            throw new BridgeException(e.toString());
        }


    }
}

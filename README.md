# universAAL IoT Bridge
This component is an extension of InterIoT/SIL allowing the semantic connection of universAAL IoT instances (with REST API) to be connected to it.

## Getting started
1. Build the bridge (or copy the jar file from the available repositories) into the InterIoT/SIL bridge folder. 
2. Restart InterIoT/SIL
3. Register universAAL IoT platform.

## Building

You'll need maven.

```
mvn clean install
```
## Testing

Is automatically performed for the instalation process. Check tests in the code (/src/test/java). Execute the test with 

```
mvn test
```

## Where to find further information
Follow the course on Bridge deployment.

### Bridge configuration 
To configure the SIL with the universAAL bridge properly, once insalled, the `UAALBridge.properties` file must be added inside the proper mounted docker volume.

an example UAALBridge.properties file in 
https://git.activageproject.eu/Bridges/universAAL/src/master/src/main/resources
```
universaal-user=interiot
universaal-password=interiot
universaal-space=interiot
```
The values of user, password and space do not need to be the same. These are just examples.
This file specifies default values when not provided through the platform registration request. 

### universAAL Cosiderations
It is advisable to send the user and password for the bridge to connect to universAAL API in the platform registration message instead of setting them in this file. These must not change once the first connection is established, this is because how [REST API registers credentials](https://github.com/universAAL/remote/wiki/REST-API#Security), if changed then of course the credentials must change.

Set a universAAL Space identifier. This must be unique for each Bridge client that connects to a single universAAL instance, since communication between different spaces is not possible by default.

Do not add the base URL of uAAL in the properties. It must be set in the platform registration request.

The universAAL instance to which the Bridge connects needs to meet a series of conditions:
1. Must run universAAL version 3.4.2-SNAPSHOT (this is the latest version at the time of writing), or above. A distribution can be found in https://github.com/universAAL/distro.karaf/releases 
2. Must include at least the following set of ontologies. To install an ontology bundle, type `feature:install {name of the ontology feature listed below}` or add them as additional boot features in the distro.karaf project before building.
  * uAAL-Ont.Device
  * uAAL-Ont.Measurement
  * uAAL-Ont.Health.Measurement
  * uAAL-Ont.personalhealthdevice
  * uAAL-Ont.Che
3. Must include the REST API Manager. To install it, type `feature:install uAAL-RI.RESTAPI` or add it as an additional boot feature in the distro.karaf project before building. 
4. Configure the REST API Manager appropriately, as described in https://github.com/universAAL/remote/wiki/REST-API#Configuration . This requires at least setting the URL with the system property                                                                                                                                                                                                            org.universAAL.ri.rest.manager.serv.host.


### Dependencies 
The universAAL Bridge does not need additional dependencies on top of what INTERMW/SIL already depends on. 

## Example of platform registration in Inter-MW API
Platforms are registered in the SIL using the POST /mw2mw/platforms operation. 
In the case of universAAL, the parameters to be provided are:
* __platformId__: the id that will be assigned to the registered platform. It has to conform to the format `“http://{DS_CODE}.inter-iot.eu/platforms/{id}”`, where DS_CODE is the acronym for the deployment site in ActivAge (e.g., ‘DSVLC’) and ‘id’ is an internal identifier for the platform (eg, ‘universAAL’)
* __type__: this is the bridge type to use (`http://inter-iot.eu/universAAL`) . This label can be obtained from /get platform-types or from the Table of platform types on the main/general guide. Check that the expected platform type is shown using GET platform-types in the API to confirm that the universAAL bridge has been integrated correctly.
* __baseEndpoint__: it refers to universAAL’s REST API address. It should be an URL (e.g., `http://localhost:9000/uaal/`) like the one set in the configuration file.
* __location__: internal attribute used by Inter-Iot to give the geographic location of the platform. This field is optional, but in case it is provided, it has to be an URL.
* __name__: a label to identify the platform
* __downstreamXXX/upstreamXXX__: these fields are used to register alignments. If there are no alignments to register in a specific alignment field use void string “”.
* __username__: used for bridge authentication as client
* __encryptedPassword__: used for Bridge authentication as client
* __encryptedAlgorithm__: used for Bridge aunthenticationas client, to interpret the codification to send the password in.

Example JSON object:

```
{
  "platformId": "http://activage-test.upm.es/universaal",
  "type": "http://inter-iot.eu/UniversAAL",
  "baseEndpoint": "https://dev.uaal.activage.lst.tfo.upm.es/uaal/",
  "location": "http://testbed-activage-upm.es/",
  "name": "universAAL platform",
  "username": "interiot",
  "encryptedPassword": "interiot",
  "encryptionAlgorithm": "SHA-256",
  "downstreamInputAlignmentName": "AIOTES-universAAL-alignment",
  "downstreamInputAlignmentVersion": "1.2",
  "downstreamOutputAlignmentName": "",
  "downstreamOutputAlignmentVersion": "",
  "upstreamInputAlignmentName": "universAAL-AIOTES-Alignment",
  "upstreamInputAlignmentVersion": "1.5",
  "upstreamOutputAlignmentName": "",
  "upstreamOutputAlignmentVersion": ""
}
```

Note that the default alignments for universAAL (which cover the default uAAL ontologies) can be found https://git.activageproject.eu/ALIGNMENTS/universAAL . 

The REST API operation returns 202 (Accepted) response code. To make sure the process has executed successfully, check the response message and the logs.

##Device registration
When a platform is registered, the SIL performs automatically a device discovery process. However, the semantic translation is needed in order to insert this information in the registry. If no semantic alignments are being used (or the device discovery fails for any other reason), the devices can be also manually registered in the SIL. This can be done using the operation `POST /mw2mw/devices`. This operation also allows the creation of virtual devices.

The universAAL platform does not impose any restriction on the devices IDs other than following the URI format and including a # before the final segment of the identifier, for instance:

`http://inter-iot.eu/default.owl#deviceID`

If the ID does not follow the URI format, it may fail. If it does not include #, the bridge will convert it to an equivalent ID as described by the following examples:

arbitraryURIstring > `http://inter-iot.eu/default.owl#arbitraryURIstring`

arbitraryURI/string > `arbitraryURI/default.owl#string`

If the entity already exists on universAAL, only the AIoTES metadata is generated. Otherwise, the entity is created.

Example of POST data:

```
{
  "devices": [
	{
  	"deviceId": "http://dsvlc.interiot.eu/dev/activage/Device/uAALDevices.owl#device01",
  	"hostedBy": "http://dsvlc.inter-iot.eu/platforms/universAAL",
  	"location": "http://dsvlc.inter-iot.eu/activage-server",
  	"name": "Presence Sensor 001"
	},
	...
  ]
}
```

The REST API operation returns 202 (Accepted) response code. To make sure the process has been executed successfully, check the response messages.



## Authentication options supported by the bridge
The authentication required by the current version of the universAAL REST API is HTTP Basic, which is not enough on its won to guarantee security. In order to properly secure the connection, it is necessary to at least use HTTPS in the REST API manager. This is explained [here](https://github.com/universAAL/remote/wiki/REST-API#Security)  

## Limitations of the current version
There are a set of limitations and caveats to connecting universAAL when using the Bridge in its current version:
* Unregistering a universAAL platform in INTERIoT will remove its allocated space in the universAAL instance along with all its devices. When re-registering, the space will be created anew, and all devices will have to be recreated.
* Messages for updatePlatform do not have any effect, since all properties of a universAAL Bridge connection that may vary depend on the configuration file.
* When creating universAAL services (e.g. “get value” or “list devices”) for devices, all devices are considered to be the root class `http://ontology.universAAL.org/Device.owl#ValueDevice` with values of type `http://www.w3.org/2001/XMLSchema#float`. In order to have correct service specializations, the bridge code methods `getValueType` and `getSpecializedType` must be updated. Observations do not need any of this, as the translation is handled entirely by the alignments.

## Other relevant information
Subscriptions are created using the deviceId (which is the same in uAAL and the SIL) as a condition on the subject of the events.

## Contributing

Pull requests are always appreciated. 

## Licence
The bridge is licensed under dual license Apache Sofware License 2.0 and Eclipse Public License.



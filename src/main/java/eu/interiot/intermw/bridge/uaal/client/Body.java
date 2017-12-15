package eu.interiot.intermw.bridge.uaal.client;

public class Body {

    public static final String REPLACE_SPACE = "$%s";
    public static final String REPLACE_HOST = "$%h";
    public static final String REPLACE_PORT = "$%p";
    public static final String REPLACE_PATH = "$%t";
    public static final String REPLACE_PATH_C = "$%c";
    public static final String REPLACE_PATH_S = "$%e";
    public static final String REPLACE_CALLER = "$%a";
    public static final String REPLACE_SUBSCRIBER = "$%u";
    public static final String REPLACE_ID = "$%i";
    public static final String REPLACE_PREFIX = "$%x";
    public static final String REPLACE_TIME = "$%m";
    public static final String REPLACE_TYPE = "$%y";
    public static final String REPLACE_UUID = "$%d";
    public static final String REPLACE_OBJ = "$%o";
    public static final String REPLACE_OBJ_2 = "$%b";
    public static final String REPLACE_TYPE_OBJ = "$%j";

    public static final String POST_SPACES = "{\r\n"
            + "   \"space\": {\r\n"
            + "     \"@id\": \"" + REPLACE_SPACE + "\",\r\n"
            + "     \"callback\": \"" + REPLACE_HOST + REPLACE_PORT + REPLACE_PATH + "\"\r\n"
            + "   }\r\n"
            + " }";
    public static final String POST_SPACES_S_SERVICE_CALLERS = "{\r\n"
            + "  \"caller\": {\r\n"
            + "    \"@id\": \"" + REPLACE_CALLER + "\"\r\n"
            + "  }\r\n"
            + " }";
    public static final String POST_SPACES_S_CONTEXT_PUBLISHERS = "{\r\n"
            + "  \"publisher\": {\r\n"
            + "    \"@id\": \"" + REPLACE_ID + "\",\r\n"
            + "    \"providerinfo\": \""
            + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\r\\n"
            + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\\r\\n"
            + "@prefix : <http://ontology.universAAL.org/Context.owl#> .\\r\\n"
            + "<" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + "> :myClassesOfEvents (\\r\\n"
            + "    [\\r\\n"
            + "      a :ContextEventPattern ;\\r\\n"
            + "      <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
            + "          owl:hasValue <" + REPLACE_PREFIX + REPLACE_ID + "> ;\\r\\n"
            + "          a owl:Restriction ;\\r\\n"
            + "          owl:onProperty rdf:subject\\r\\n"
            + "        ]\\r\\n"
            + "    ]\\r\\n"
            + "  ) ;\\r\\n"
            + "  a :ContextProvider ;\\r\\n"
            + "  :hasType :controller .\\r\\n"
            + ":controller a :ContextProviderType .\\r\\n"
            + "<" + REPLACE_PREFIX + REPLACE_ID + "> a <http://ontology.universaal.org/PhThing.owl#Device>,"
            + " <http://ontology.universaal.org/PhThing.owl#PhysicalThing> .\"\r\n"
            + "  } \r\n"
            + " }";
    public static final String POST_SPACES_S_CONTEXT_PUBLISHERS_2 = "{\r\n"
            + "  \"publisher\": {\r\n"
            + "    \"@id\": \"" + REPLACE_ID + "\",\r\n"
            + "    \"providerinfo\": \""
            + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\r\\n"
            + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\\r\\n"
            + "@prefix : <http://ontology.universAAL.org/Context.owl#> .\\r\\n"
            + "<" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + "> :myClassesOfEvents (\\r\\n"
            + "    [\\r\\n"
            + "      a :ContextEventPattern ;\\r\\n"
            + "      <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
            + "          owl:hasValue <" + REPLACE_PREFIX + REPLACE_ID + "> ;\\r\\n"
            + "          a owl:Restriction ;\\r\\n"
            + "          owl:onProperty rdf:subject\\r\\n"
            + "        ]\\r\\n"
            + "    ]\\r\\n"
            + "  ) ;\\r\\n"
            + "  a :ContextProvider ;\\r\\n"
            + "  :hasType :controller .\\r\\n"
            + ":controller a :ContextProviderType .\\r\\n"
            + "<" + REPLACE_PREFIX + REPLACE_ID + "> a <" + REPLACE_TYPE + ">,"
            + " <http://ontology.universaal.org/PhThing.owl#Sensor>,"
            + " <http://ontology.universaal.org/PhThing.owl#Device>,"
            + " <http://ontology.universaal.org/PhThing.owl#PhysicalThing> .\"\r\n"
            + "  } \r\n"
            + " }";
    public static final String POST_SPACES_S_SERVICE_CALLEES = "{\r\n"
            + "  \"callee\": {\r\n"
            + "    \"@id\": \"" + REPLACE_ID + "\",\r\n"
            + "     \"callback\": \"" + REPLACE_HOST + REPLACE_PORT + REPLACE_PATH_S + REPLACE_ID + "\",\r\n"
            + "    \"profile\": \""
            + "@prefix ns: <http://www.daml.org/services/owl-s/1.1/Service.owl#> .\\r\\n"
            + "@prefix ns2: <http://www.daml.org/services/owl-s/1.1/Profile.owl#> .\\r\\n"
            + "@prefix : <http://www.daml.org/services/owl-s/1.1/Process.owl#> .\\r\\n"
            + "_:BN000000 ns:presentedBy <" + REPLACE_PREFIX + "serverOf" + REPLACE_ID + ">;\\r\\n"
            + "  a ns2:Profile ;\\r\\n"
            + "  ns2:has_process <" + REPLACE_PREFIX + "serverOf" + REPLACE_ID + "Process> ;\\r\\n"
            + "  ns2:hasResult [\\r\\n"
            + "  a :Result;\\r\\n"
            + "    :withOutput (\\r\\n"
            + "      [\\r\\n"
            + "        a :OutputBinding ;\\r\\n"
            + "        :toParam <" + REPLACE_PREFIX + "output> ;\\r\\n"
            + "        :valueForm \\\"\\\"\\\"\\r\\n"
            + "          @prefix : <http://ontology.universAAL.org/Service.owl#> .\\r\\n"
            + "          _:BN000000 a :PropertyPath ;\\r\\n"
            + "            :thePath (\\r\\n"
            + "              <http://ontology.universaal.org/PhThing.owl#controls>\\r\\n"
            + "            ) .\\r\\n"
            + "          \\\"\\\"\\\"^^<http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral>\\r\\n"
            + "      ]\\r\\n"
            + "    )\\r\\n"
            + "  ] ;\\r\\n"
            + "  ns2:hasOutput (\\r\\n"
            + "    <" + REPLACE_PREFIX + "output>\\r\\n"
            + "  ) .\\r\\n"
            + "<" + REPLACE_PREFIX + "serverOf" + REPLACE_ID + "> a <http://ontology.universaal.org/PhThing.owl#DeviceService> ;\\r\\n"
            + "  ns:presents _:BN000000 .\\r\\n"
            + "<" + REPLACE_PREFIX + "output> a :Output ;\\r\\n"
            + "  :parameterType <http://ontology.universaal.org/PhThing.owl#Device> .\"\r\n"
            + "  }\r\n"
            + " }";
    public static final String POST_SPACES_S_SERVICE_CALLERS_C = "@prefix ns: <http://www.daml.org/services/owl-s/1.1/Profile.owl#> .\r\n"
            + "@prefix pvn: <http://ontology.universAAL.org/uAAL.owl#> .\r\n"
            + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\r\n"
            + "@prefix ns2: <http://ontology.universaal.org/PhThing.owl#> .\r\n"
            + "@prefix ns3: <http://www.daml.org/services/owl-s/1.1/Service.owl#> .\r\n"
            + "@prefix : <http://www.daml.org/services/owl-s/1.1/Process.owl#> .\r\n"
            + "_:BN000000 a pvn:ServiceRequest ;\r\n"
            + "  pvn:requiredResult [\r\n"
            + "    :withOutput (\r\n"
            + "      [\r\n"
            + "        a :OutputBinding ;\r\n"
            + "        :toParam <" + REPLACE_PREFIX + "controlledThings> ;\r\n"
            + "        :valueForm \"\"\"\r\n"
            + "          @prefix : <http://ontology.universAAL.org/Service.owl#> .\r\n"
            + "          _:BN000000 a :PropertyPath ;\r\n            :thePath (\r\n"
            + "              <http://ontology.universaal.org/PhThing.owl#controls>\r\n"
            + "            ) .\r\n"
            + "          \"\"\"^^<http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral>\r\n"
            + "      ]\r\n"
            + "    ) ;\r\n"
            + "    a :Result\r\n"
            + "  ] ;\r\n"
            + "  pvn:requestedService <" + REPLACE_PREFIX + "theService> .\r\n"
            + "<" + REPLACE_PREFIX + "theService> a ns2:DeviceService ;\r\n"
            + "  pvn:instanceLevelRestrictions (\r\n"
            + "    [\r\n"
            + "      a owl:Restriction ;\r\n"
            + "      owl:allValuesFrom ns2:Device ;\r\n"
            + "      owl:onProperty ns2:controls\r\n"
            + "    ]\r\n"
            + "  ) ;\r\n"
            + "  ns3:presents [\r\n"
            + "    ns3:presentedBy <" + REPLACE_PREFIX + "theService> ;\r\n"
            + "    a ns:Profile ;\r\n"
            + "    ns:has_process <" + REPLACE_PREFIX + "theServiceProcess>\r\n"
            + "  ] .\r\n"
            + "ns2:Device a owl:Class .\r\n"
            + "<" + REPLACE_PREFIX + "controlledThings> a :Output .";

    public static final String POST_SPACES_S_CONTEXT_PUBLISHERS_P_L = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n"
            + "@prefix : <http://ontology.universAAL.org/Context.owl#> .\r\n"
            + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\r\n"
            + "<urn:org.universAAL.middleware.context.rdf:ContextEvent#_:" + REPLACE_UUID + ">\r\n"
            + "  a :ContextEvent ;\r\n"
            + "  :hasProvider <" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + ">;\r\n"
            + "  :hasTimestamp \"" + REPLACE_TIME + "\"^^xsd:long ;\r\n"
            + "  rdf:subject <" + REPLACE_PREFIX + REPLACE_ID + "> ;\r\n"
            + "  rdf:predicate <http://ontology.universAAL.org/Device.owl#hasValue> ;\r\n"
            + "  rdf:object " + REPLACE_OBJ + " .\r\n"
            + "<" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + "> a :ContextProvider ;\r\n"
            + "  :hasType :controller .\r\n"
            + ":controller a :ContextProviderType .\r\n"
            + "<" + REPLACE_PREFIX + REPLACE_ID + "> a <http://ontology.universaal.org/PhThing.owl#Device>,"
            + " <http://ontology.universaal.org/PhThing.owl#PhysicalThing> .\"\r\n";

    public static final String POST_SPACES_S_CONTEXT_PUBLISHERS_P_D = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n"
            + "@prefix : <http://ontology.universAAL.org/Context.owl#> .\r\n"
            + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\r\n"
            + "<urn:org.universAAL.middleware.context.rdf:ContextEvent#_:" + REPLACE_UUID + ">\r\n"
            + "  a :ContextEvent ;\r\n"
            + "  :hasProvider <" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + ">;\r\n"
            + "  :hasTimestamp \"" + REPLACE_TIME + "\"^^xsd:long ;\r\n"
            + "  rdf:subject <" + REPLACE_PREFIX + REPLACE_ID + "> ;\r\n"
            + "  rdf:predicate <http://ontology.universAAL.org/Device.owl#hasValue> ;\r\n"
            + "  rdf:object <" + REPLACE_OBJ + "> .\r\n"
            + "  <" + REPLACE_OBJ + "> a <" + REPLACE_TYPE_OBJ + "> .\r\n"
            + "<" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + "> a :ContextProvider ;\r\n"
            + "  :hasType :controller .\r\n"
            + ":controller a :ContextProviderType .\r\n"
            + "<" + REPLACE_PREFIX + REPLACE_ID + "> a <http://ontology.universaal.org/PhThing.owl#Device>,"
            + " <http://ontology.universaal.org/PhThing.owl#PhysicalThing> .\"\r\n";

    public static final String POST_SPACES_S_CONTEXT_PUBLISHERS_P_BP = "@prefix ns: <http://ontology.universaal.org/PhThing.owl#> .\r\n"
            + "@prefix ns1: <http://ontology.universAAL.org/Context.owl#> .\r\n"
            + "@prefix ns2: <http://ontology.universaal.org/Measurement.owl#> .\r\n"
            + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\r\n"
            + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\r\n"
            + "@prefix ns3: <http://ontology.universaal.org/HealthMeasurement.owl#> .\r\n"
            + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n"
            + "@prefix ns4: <http://ontology.universAAL.org/Device.owl#> .\r\n"
            + "@prefix : <http://ontology.universAAL.org/InterIoT.owl#> .\r\n"
            + "<urn:org.universAAL.middleware.context.rdf:ContextEvent#_:" + REPLACE_UUID + ">\r\n"
            + "ns1:hasProvider <" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + "> ;\r\n"
            + "a ns1:ContextEvent ;\r\n"
            + "rdf:subject <" + REPLACE_PREFIX + REPLACE_ID + "> ;\r\n"
            + "ns1:hasTimestamp \"" + REPLACE_TIME + "\"^^xsd:long ;\r\n"
            + "rdf:predicate ns4:hasValue ;\r\n"
            + "rdf:object :bp .\r\n"
            + ":bpsys a ns2:Measurement ;\r\n"
            + "ns2:value " + REPLACE_OBJ + "^^xsd:float .\r\n"
            + "ns1:gauge a ns1:ContextProviderType .\r\n"
            + "<" + REPLACE_PREFIX + REPLACE_ID + "> a <http://ontology.universAAL.org/PersonalHealthDevice.owl#BloodPressureSensor> ,\r\n"
            + "ns:Device ,\r\n"
            + "ns:PhysicalThing ;\r\n"
            + "ns4:hasValue :bp .\r\n"
            + "<" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + "> ns1:myClassesOfEvents (\\r\\n"
            + "    [\\r\\n"
            + "      a ns1:ContextEventPattern ;\\r\\n"
            + "      <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
            + "          owl:hasValue <" + REPLACE_PREFIX + REPLACE_ID + "> ;\\r\\n"
            + "          a owl:Restriction ;\\r\\n"
            + "          owl:onProperty rdf:subject\\r\\n"
            + "        ]\\r\\n"
            + "    ]\\r\\n"
            + "  ) ;\\r\\n"
            + "  a ns1:ContextProvider ;\\r\\n"
            + "  ns1:hasType ns1:controller .\\r\\n"
            + "ns1:controller a ns1:ContextProviderType .\\r\\n"
            + ":bpdias a ns2:Measurement ;\r\n"
            + "ns2:value " + REPLACE_OBJ_2 + "^^xsd:float .\r\n"
            + ":bp ns3:diastolicBloodPreassure :bpdias ;\r\n"
            + "a ns3:BloodPressure ,\r\n"
            + "ns3:HealthMeasurement ;\r\n"
            + "ns3:systolicBloodPreassure :bpsys .";

    public static final String POST_SPACES_S_CONTEXT_PUBLISHERS_P_W = "@prefix ns: <http://ontology.universaal.org/PhThing.owl#> .\r\n"
            + "@prefix ns1: <http://ontology.universAAL.org/InterIoT.owl#> .\r\n"
            + "@prefix ns2: <http://ontology.universaal.org/Measurement.owl#> .\r\n"
            + "@prefix ns3: <http://ontology.universaal.org/HealthMeasurement.owl#> .\r\n"
            + "@prefix ns4: <http://ontology.universAAL.org/Device.owl#> .\r\n"
            + "@prefix ns5: <http://ontology.universAAL.org/PersonalHealthDevice.owl#> .\r\n"
            + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\r\n"
            + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\r\n"
            + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n"
            + "@prefix : <http://ontology.universAAL.org/Context.owl#> .\r\n"
            + "<urn:org.universAAL.middleware.context.rdf:ContextEvent#_:" + REPLACE_UUID + ">\r\n"
            + ":hasProvider <" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + "> ;\r\n"
            + "a :ContextEvent ;\r\n"
            + "rdf:subject <" + REPLACE_PREFIX + REPLACE_ID + "> ;\r\n"
            + ":hasTimestamp \"" + REPLACE_TIME + "\"^^xsd:long ;\r\n"
            + "rdf:predicate ns4:hasValue ;\r\n"
            + "rdf:object ns1:weight .\r\n"
            + "<" + REPLACE_PREFIX + REPLACE_ID + "> a ns5:WeighingScale ,\r\n"
            + "ns:Device ,\r\n"
            + "ns:PhysicalThing ;\r\n"
            + "ns4:hasValue ns1:weight .\r\n"
            + "<" + REPLACE_PREFIX + "providerOf" + REPLACE_ID + "> :myClassesOfEvents (\\r\\n"
            + "    [\\r\\n"
            + "      a :ContextEventPattern ;\\r\\n"
            + "      <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
            + "          owl:hasValue <" + REPLACE_PREFIX + REPLACE_ID + "> ;\\r\\n"
            + "          a owl:Restriction ;\\r\\n"
            + "          owl:onProperty rdf:subject\\r\\n"
            + "        ]\\r\\n"
            + "    ]\\r\\n"
            + "  ) ;\\r\\n"
            + "  a :ContextProvider ;\\r\\n"
            + "  :hasType :controller .\\r\\n"
            + ":controller a :ContextProviderType .\\r\\n"
            + "ns1:weight a ns3:PersonWeight ,\r\n"
            + "ns3:HealthMeasurement ,\r\n"
            + "ns2:Measurement ;\r\n"
            + "ns2:value " + REPLACE_OBJ + "^^xsd:float .";

    public static final String POST_SPACES_S_CONTEXT_SUBSCRIBERS_1 = "{\r\n"
            + "  \"subscriber\": {\r\n"
            + "    \"@id\": \"" + REPLACE_SUBSCRIBER + "\",\r\n"
            + "     \"callback\": \"" + REPLACE_HOST + REPLACE_PORT + REPLACE_PATH_C + REPLACE_SUBSCRIBER + "\",\r\n"
            + "    \"pattern\": \""
            + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\r\\n"
            + "@prefix : <http://www.w3.org/2002/07/owl#> .\\r\\n"
            + "_:BN000000 a <http://ontology.universAAL.org/Context.owl#ContextEventPattern> ;\\r\\n"
            + "  <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
            + "      a :Restriction ;\\r\\n"
            + "      :hasValue <" + REPLACE_PREFIX + REPLACE_ID + "> ;\\r\\n"
            + "      :onProperty rdf:subject\\r\\n"
            + "    ] .\\r\\n"
            + "<" + REPLACE_PREFIX + REPLACE_ID + "> a <http://ontology.universaal.org/PhThing.owl#Device>,"
            + " <http://ontology.universaal.org/PhThing.owl#PhysicalThing> .\"\r\n"
            + "  }\r\n"
            + " }";
    public static final String POST_SPACES_S_CONTEXT_SUBSCRIBERS_2 = "{\r\n"
            + "  \"subscriber\": {\r\n"
            + "    \"@id\": \"" + REPLACE_SUBSCRIBER + "\",\r\n"
            + "     \"callback\": \"" + REPLACE_HOST + REPLACE_PORT + REPLACE_PATH_C + REPLACE_SUBSCRIBER + "\",\r\n"
            + "    \"pattern\": \""
            + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\r\\n"
            + "@prefix : <http://www.w3.org/2002/07/owl#> .\\r\\n"
            + "_:BN000000 a <http://ontology.universAAL.org/Context.owl#ContextEventPattern> ;\\r\\n"
            + "  <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
            + "      a :Restriction ;\\r\\n"
            + "      :allValuesFrom <http://ontology.universaal.org/PhThing.owl#Device> ;\\r\\n"
            + "      :onProperty rdf:subject\\r\\n"
            + "    ] .\\r\\n"
            + "<http://ontology.universaal.org/PhThing.owl#Device> a :Class .\"\r\n"
            + "  }\r\n"
            + " }";
    public static final String POST_SPACES_S_CONTEXT_SUBSCRIBERS_3 = "{\r\n"
            + "  \"subscriber\": {\r\n"
            + "    \"@id\": \"" + REPLACE_SUBSCRIBER + "\",\r\n"
            + "    \"pattern\": \""
            + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\r\\n"
            + "@prefix : <http://www.w3.org/2002/07/owl#> .\\r\\n"
            + "_:BN000000 a <http://ontology.universAAL.org/Context.owl#ContextEventPattern> ;\\r\\n"
            + "  <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
            + "      a :Restriction ;\\r\\n"
            + "      :allValuesFrom <http://ontology.universaal.org/PhThing.owl#Device> ;\\r\\n"
            + "      :onProperty rdf:subject\\r\\n"
            + "    ] .\\r\\n"
            + "<http://ontology.universaal.org/PhThing.owl#Device> a :Class .\"\r\n"
            + "  }\r\n"
            + " }";
//    public static final String POST_SPACES_S_CONTEXT_SUBSCRIBERS_4="{\r\n"
//		+ "  \"subscriber\": {\r\n"
//		+ "    \"@id\": \""+conversationId+"\",\r\n"
//		+ "    \"pattern\": \""
//		+ "@prefix ns: <http://ontology.universaal.org/PhThing.owl#> .\\r\\n"
//		+ "@prefix ns1: <http://ontology.universAAL.org/SimpleHWOclient.owl#> .\\r\\n"
//		+ "@prefix owl: <http://www.w3.org/2002/07/owl#> .\\r\\n"
//		+ "@prefix : <http://ontology.universAAL.org/Device.owl#> .\\r\\n"
//		+ "_:BN000000 a <http://ontology.universAAL.org/Context.owl#ContextEventPattern> ;\\r\\n"
//		+ "  <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
//		+ "      a owl:Restriction ;\\r\\n"
//		+ "      owl:allValuesFrom [\\r\\n"
//		+ "        a owl:Class ;\\r\\n"
//		+ "        owl:oneOf (\\r\\n"
//		+ "          ns1:mylight\\r\\n"
//		+ "          ns1:myswitch\\r\\n"
//		+ "        )\\r\\n"
//		+ "      ] ;\\r\\n"
//		+ "      owl:onProperty <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject>\\r\\n"
//		+ "    ] .\\r\\n"
//		+ "ns1:myswitch a :SwitchController ,\\r\\n"
//		+ "    :SwitchSensor ,\\r\\n"
//		+ "    :SwitchActuator ,\\r\\n"
//		+ "    ns:Device ,\\r\\n"
//		+ "    ns:PhysicalThing .\\r\\n"
//		+ "ns1:mylight a :LightController ,\\r\\n"
//		+ "    :LightSensor ,\\r\\n"
//		+ "    :LightActuator ,\\r\\n"
//		+ "    ns:Device ,\\r\\n"
//		+ "    ns:PhysicalThing .\"\r\n"
//		+ "  }\r\n"
//		+ " }";

}
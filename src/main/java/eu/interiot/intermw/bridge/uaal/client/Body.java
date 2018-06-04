package eu.interiot.intermw.bridge.uaal.client;

public class Body {
    private final static String PREFIX = "http://ontology.universAAL.org/InterIoT.owl#";
    public static final String SPACE="$%s";
    public static final String HOST="$%h";
    public static final String PORT="$%p";
    public static final String PATH="$%t";
    public static final String PATH_C="$%c";
    public static final String PATH_S="$%e";
    public static final String CALLER="$%a";
    public static final String SUBSCRIBER="$%u";
    public static final String ID="$%i";
//    public static final String TIME="$%m";
    public static final String TYPE="$%y";
//    public static final String UUID="$%d";
//    public static final String OBJ="$%o";
//    public static final String OBJ_2="$%b";
//    public static final String TYPE_OBJ="$%j";

    public static final String CREATE_SPACE="{\r\n"
	    + "   \"space\": {\r\n"
	    + "     \"@id\": \""+SPACE+"\",\r\n"
	    + "     \"callback\": \""+HOST+PORT+PATH+"\"\r\n"
	    + "   }\r\n"
	    + " }";
    public static final String CREATE_CALLER="{\r\n"
	    + "  \"caller\": {\r\n"
	    + "    \"@id\": \""+CALLER+"\"\r\n"
	    + "  }\r\n"
	    + " }";
    public static final String CREATE_PUBLISHER="{\r\n"
	    + "  \"publisher\": {\r\n"
	    + "    \"@id\": \""+ID+"\",\r\n"
	    + "    \"providerinfo\": \""
	    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\r\\n"
	    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\\r\\n"
	    + "@prefix : <http://ontology.universAAL.org/Context.owl#> .\\r\\n"
	    + "<"+PREFIX+"providerOf"+ID+"> :myClassesOfEvents (\\r\\n"
	    + "    [\\r\\n"
	    + "      a :ContextEventPattern ;\\r\\n"
	    + "      <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
	    + "          owl:hasValue <"+PREFIX+ID+"> ;\\r\\n"
	    + "          a owl:Restriction ;\\r\\n"
	    + "          owl:onProperty rdf:subject\\r\\n"
	    + "        ]\\r\\n"
	    + "    ]\\r\\n"
	    + "  ) ;\\r\\n"
	    + "  a :ContextProvider ;\\r\\n"
	    + "  :hasType :controller .\\r\\n"
	    + ":controller a :ContextProviderType .\\r\\n"
	    + "<"+PREFIX+ID+"> a <http://ontology.universaal.org/PhThing.owl#Device>,"
	    + " <http://ontology.universaal.org/PhThing.owl#PhysicalThing> .\"\r\n"
	    + "  } \r\n"
	    + " }";
    public static final String CREATE_CALLEE="{\r\n"
	    + "  \"callee\": {\r\n"
	    + "    \"@id\": \""+ID+"\",\r\n"
	    + "     \"callback\": \""+HOST+PORT+PATH_S+ID+"\",\r\n"
	    + "    \"profile\": \""
	    + "@prefix ns: <http://www.daml.org/services/owl-s/1.1/Service.owl#> .\\r\\n"
	    + "@prefix ns2: <http://www.daml.org/services/owl-s/1.1/Profile.owl#> .\\r\\n"
	    + "@prefix : <http://www.daml.org/services/owl-s/1.1/Process.owl#> .\\r\\n"
	    + "_:BN000000 ns:presentedBy <"+PREFIX+"serverOf"+ID+">;\\r\\n"
	    + "  a ns2:Profile ;\\r\\n"
	    + "  ns2:has_process <"+PREFIX+"serverOf"+ID+"Process> ;\\r\\n"
	    + "  ns2:hasResult [\\r\\n"
	    + "  a :Result;\\r\\n"
	    + "    :withOutput (\\r\\n"
	    + "      [\\r\\n"
	    + "        a :OutputBinding ;\\r\\n"
	    + "        :toParam <"+PREFIX+"output> ;\\r\\n"
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
	    + "    <"+PREFIX+"output>\\r\\n"
	    + "  ) .\\r\\n"
	    + "<"+PREFIX+"serverOf"+ID+"> a <"+TYPE+"> ;\\r\\n"
	    + "  ns:presents _:BN000000 .\\r\\n"
	    + "<"+PREFIX+"output> a :Output ;\\r\\n"
	    + "  :parameterType <"+TYPE+"> .\"\r\n"
	    + "  }\r\n"
	    + " }";    
    public static final String CREATE_SUBSCRIBER="{\r\n"
	    + "  \"subscriber\": {\r\n"
	    + "    \"@id\": \""+SUBSCRIBER+"\",\r\n"
	    + "     \"callback\": \""+HOST+PORT+PATH_C+SUBSCRIBER+"\",\r\n"
	    + "    \"pattern\": \""
	    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\r\\n"
	    + "@prefix : <http://www.w3.org/2002/07/owl#> .\\r\\n"
	    + "_:BN000000 a <http://ontology.universAAL.org/Context.owl#ContextEventPattern> ;\\r\\n"
	    + "  <http://www.w3.org/2000/01/rdf-schema#subClassOf> [\\r\\n"
	    + "      a :Restriction ;\\r\\n"
	    + "      :hasValue <"+PREFIX+ID+"> ;\\r\\n"
	    + "      :onProperty rdf:subject\\r\\n"
	    + "    ] .\\r\\n"
	    + "<"+PREFIX+ID+"> a <http://ontology.universaal.org/PhThing.owl#Device>,"
	    + " <http://ontology.universaal.org/PhThing.owl#PhysicalThing> .\"\r\n"
	    + "  }\r\n"
	    + " }";
}
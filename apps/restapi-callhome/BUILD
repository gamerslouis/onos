APPS = [
    "org.onosproject.netconf",
    "org.onosproject.drivers.netconf",
]

COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//utils/rest:onlab-rest"
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
    name = "Restnetconf",
    api_description = "RestAPI for reactive-callhome",
    api_package = "org.onosproject.rest.impl",
    api_title = "RestAPI for reactive-callhome",
    deps = COMPILE_DEPS
)

APPS = [
    "org.onosproject.netconf",
    "org.onosproject.drivers.netconf",
]

COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@jersey_server//jar",
    "@javax_ws_rs_api//jar",
    "@servlet_api//jar",
    "@javax_inject//jar",
    "//utils/rest:onlab-rest",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//drivers/netconf:onos-drivers-netconf",
    "//drivers/utilities:onos-drivers-utilities",

]

osgi_jar_with_tests(
    web_context = "/onos/netconfapi",
    api_description = "REST API for Callhome Device",
    api_package = "org.onosproject.netconfapi",
    api_title = "Callhome Device Management Rest API",
    api_version = "1.0",
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.netconfapi",
    category = "Utility",
    description = "API for Callhome Device Management",
    required_apps = APPS,
    title = "API for Callhome Device Management",
    url = "http://onosproject.org",
)

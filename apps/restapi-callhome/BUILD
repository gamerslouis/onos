APPS = [
    "org.onosproject.netconf",
    "org.onosproject.drivers.netconf",
]

COMPILE_DEPS = CORE_DEPS + [
    "//protocols/netconf/api:onos-protocols-netconf-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Utility",
    description = "RestAPI for reactive-callhome",
    required_apps = APPS,
    title = "RestAPI for reactive-callhome",
    url = "http://onosproject.org",
)

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
    description = "Reactively register netconf call home device to network configuration service " +
                  "when netconf device is connected and authenticate failed.",
    required_apps = APPS,
    title = "Reactive Netconf Call Home Configuration",
    url = "http://onosproject.org",
)

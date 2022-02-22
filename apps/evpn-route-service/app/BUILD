COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + CLI + [
    "@io_netty_netty//jar",
    "//core/store/serializers:onos-core-serializers",
    "//apps/routing/common:onos-apps-routing-common",
    "//apps/evpn-route-service/api:onos-apps-evpn-route-service-api",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.evpnrouteservice.cli"],
    deps = COMPILE_DEPS,
)

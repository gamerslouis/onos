BUNDLES = JACKSON + [
    "//apps/kubevirt-node/api:onos-apps-kubevirt-node-api",
    "//apps/kubevirt-node/app:onos-apps-kubevirt-node-app",
    "@json//jar",
    "@commons_net//jar",
    "@k8s_client//jar",
    "@k8s_model//jar",
    "@k8s_model_common//jar",
    "@k8s_model_core//jar",
    "@k8s_model_rbac//jar",
    "@k8s_model_apps//jar",
    "@k8s_model_admissionregistration//jar",
    "@k8s_model_apiextensions//jar",
    "@k8s_model_batch//jar",
    "@k8s_model_certificates//jar",
    "@k8s_model_coordination//jar",
    "@k8s_model_discovery//jar",
    "@k8s_model_flowcontrol//jar",
    "@k8s_model_metrics//jar",
    "@k8s_model_scheduling//jar",
    "@k8s_model_storageclass//jar",
    "@k8s_model_autoscaling//jar",
    "@k8s_model_events//jar",
    "@k8s_model_extensions//jar",
    "@k8s_model_networking//jar",
    "@k8s_model_node//jar",
    "@k8s_model_policy//jar",
    "@okhttp//jar",
    "@okio//jar",
    "@logging_interceptor//jar",
    "@jackson_dataformat_yaml//jar",
    "@jackson_datatype_jsr310//jar",
    "@snakeyaml//jar",
    "@dns_java//jar",
]

onos_app(
    category = "Integration",
    description = "SONA KubeVirt Node Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.ovsdb-base",
        "org.onosproject.drivers.ovsdb",
    ],
    title = "KubeVirt Node Application",
    url = "https://wiki.onosproject.org/display/ONOS/SONA%3A+DC+Network+Virtualization",
)

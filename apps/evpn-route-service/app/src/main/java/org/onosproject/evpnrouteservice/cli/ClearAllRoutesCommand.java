/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.evpnrouteservice.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.evpnrouteservice.EvpnRouteStore;
import org.onosproject.evpnrouteservice.EvpnRouteTableId;
import org.onosproject.evpnrouteservice.EvpnRouteSet;
import org.onosproject.evpnrouteservice.EvpnRoute;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Command to show the routes learned through BGP.
 */
@Service
@Command(scope = "evpn", name = "clear", description = "Clear all evpn routes")
public class ClearAllRoutesCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() {
        EvpnRouteStore service = AbstractShellCommand.get(EvpnRouteStore.class);

        service.getRouteTables().forEach(tableId -> {
            Collection<EvpnRouteSet> routeSets = service.getRoutes(tableId);
            routeSets.forEach(routeSet -> routeSet.routes().forEach(
                route -> service.removeRoute(route)));
        });
    }
}

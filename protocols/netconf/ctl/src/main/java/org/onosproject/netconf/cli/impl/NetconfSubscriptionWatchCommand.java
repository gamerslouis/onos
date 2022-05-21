/*
 * Copyright 2022-present Open Networking Foundation
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

package org.onosproject.netconf.cli.impl;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@Service
@Command(scope = "onos", name = "netconf-subscription-watch",
        description = "Watch netconf device notification events")
public class NetconfSubscriptionWatchCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Override
    protected void doExecute() {
        NetconfController controller = get(NetconfController.class);
        DeviceId did = DeviceId.deviceId(uri);

        NetconfDevice netconfDevice = controller.getNetconfDevice(did);
        if (netconfDevice == null) {
            print("%s not found or not connected to this node", did);
            return;
        }

        BlockingDeque<NetconfDeviceOutputEvent> q = new LinkedBlockingDeque<>();

        NetconfDeviceOutputEventListener l = event -> {

            if (event.type() == NetconfDeviceOutputEvent.Type.DEVICE_NOTIFICATION) {
                q.push(event);
            }
        };

        try {
            netconfDevice.getSession().addDeviceOutputListener(l);
            netconfDevice.getSession().startSubscription();
        } catch (NetconfException e) {
            e.printStackTrace();
        }
        try {
            while (true) {
                NetconfDeviceOutputEvent event = q.take();
                print("Device %s has notification: %s", netconfDevice.getDeviceInfo(), event.getMessagePayload());
            }
        } catch (InterruptedException ignored) {
        } finally {
            try {
                netconfDevice.getSession().removeDeviceOutputListener(l);
            } catch (NetconfException ignored) {
            }
        }
    }
}

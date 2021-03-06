/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.collection.impl.collection.client;

import com.hazelcast.client.ClientEndpoint;
import com.hazelcast.client.ClientEngine;
import com.hazelcast.client.impl.client.BaseClientAddListenerRequest;
import com.hazelcast.collection.common.DataAwareItemEvent;
import com.hazelcast.collection.impl.collection.CollectionEventFilter;
import com.hazelcast.collection.impl.collection.CollectionPortableHook;
import com.hazelcast.collection.impl.list.ListService;
import com.hazelcast.collection.impl.set.SetService;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.ListPermission;
import com.hazelcast.security.permission.SetPermission;
import com.hazelcast.spi.EventRegistration;
import com.hazelcast.spi.EventService;
import com.hazelcast.spi.impl.PortableItemEvent;

import java.io.IOException;
import java.security.Permission;

/**
 * this class is used to attach a listener to node for collections
 */
public class CollectionAddListenerRequest extends BaseClientAddListenerRequest {

    private String name;
    private boolean includeValue;
    private String serviceName;

    public CollectionAddListenerRequest() {
    }

    public CollectionAddListenerRequest(String name, boolean includeValue) {
        this.name = name;
        this.includeValue = includeValue;
    }

    @Override
    public Object call() throws Exception {
        ClientEndpoint endpoint = getEndpoint();
        ClientEngine clientEngine = getClientEngine();
        Data partitionKey = serializationService.toData(name);
        ItemListener listener = createItemListener(endpoint, partitionKey);
        EventService eventService = clientEngine.getEventService();
        CollectionEventFilter filter = new CollectionEventFilter(includeValue);

        EventRegistration registration;
        if (localOnly) {
            registration = eventService.registerLocalListener(getServiceName(), name, filter, listener);
        } else {
            registration = eventService.registerListener(getServiceName(), name, filter, listener);
        }

        String registrationId = registration.getId();
        endpoint.addListenerDestroyAction(getServiceName(), name, registrationId);
        return registrationId;
    }

    private ItemListener createItemListener(final ClientEndpoint endpoint, final Data partitionKey) {
        return new ItemListener() {

            @Override
            public void itemAdded(ItemEvent item) {
                send(item);
            }

            @Override
            public void itemRemoved(ItemEvent item) {
                send(item);
            }

            private void send(ItemEvent event) {
                if (endpoint.isAlive()) {
                    if (!(event instanceof DataAwareItemEvent)) {
                        throw new IllegalArgumentException("Expecting: DataAwareItemEvent, Found: "
                                + event.getClass().getSimpleName());
                    }

                    DataAwareItemEvent dataAwareItemEvent = (DataAwareItemEvent) event;
                    Data item = dataAwareItemEvent.getItemData();
                    PortableItemEvent portableItemEvent = new PortableItemEvent(item, event.getEventType(),
                            event.getMember().getUuid());
                    endpoint.sendEvent(partitionKey, portableItemEvent, getCallId());
                }
            }
        };
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public int getFactoryId() {
        return CollectionPortableHook.F_ID;
    }

    @Override
    public int getClassId() {
        return CollectionPortableHook.COLLECTION_ADD_LISTENER;
    }

    @Override
    public void write(PortableWriter writer) throws IOException {
        super.write(writer);
        writer.writeUTF("n", name);
        writer.writeBoolean("i", includeValue);
        writer.writeUTF("s", serviceName);
    }

    @Override
    public void read(PortableReader reader) throws IOException {
        super.read(reader);
        name = reader.readUTF("n");
        includeValue = reader.readBoolean("i");
        serviceName = reader.readUTF("s");
    }

    @Override
    public Permission getRequiredPermission() {
        if (ListService.SERVICE_NAME.equals(serviceName)) {
            return new ListPermission(name, ActionConstants.ACTION_LISTEN);
        } else if (SetService.SERVICE_NAME.equals(serviceName)) {
            return new SetPermission(name, ActionConstants.ACTION_LISTEN);
        }
        throw new IllegalArgumentException("No service matched!!!");
    }

    @Override
    public String getDistributedObjectName() {
        return name;
    }

    @Override
    public String getMethodName() {
        return "addItemListener";
    }

    @Override
    public Object[] getParameters() {
        return new Object[]{null, includeValue};
    }
}

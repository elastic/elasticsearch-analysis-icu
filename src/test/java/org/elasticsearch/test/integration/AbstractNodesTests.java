/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.test.integration;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import java.util.Map;

import static org.elasticsearch.common.collect.Maps.newHashMap;
import static org.elasticsearch.common.settings.ImmutableSettings.Builder.EMPTY_SETTINGS;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public abstract class AbstractNodesTests {

    protected final ESLogger logger = Loggers.getLogger(getClass());

    private Map<String, Node> nodes = newHashMap();

    private Map<String, Client> clients = newHashMap();

    private Settings defaultSettings = ImmutableSettings
            .settingsBuilder()
            .put("cluster.name", "test-cluster-" + NetworkUtils.getLocalAddress().getHostName())
            .build();

    public void putDefaultSettings(Settings.Builder settings) {
        putDefaultSettings(settings.build());
    }

    public void putDefaultSettings(Settings settings) {
        defaultSettings = ImmutableSettings.settingsBuilder().put(defaultSettings).put(settings).build();
    }

    public Node startNode(String id) {
        return buildNode(id).start();
    }

    public Node startNode(String id, Settings.Builder settings) {
        return startNode(id, settings.build());
    }

    public Node startNode(String id, Settings settings) {
        return buildNode(id, settings).start();
    }

    public Node buildNode(String id) {
        return buildNode(id, EMPTY_SETTINGS);
    }

    public Node buildNode(String id, Settings.Builder settings) {
        return buildNode(id, settings.build());
    }

    public Node buildNode(String id, Settings settings) {
        String settingsSource = getClass().getName().replace('.', '/') + ".yml";
        Settings finalSettings = settingsBuilder()
                .loadFromClasspath(settingsSource)
                .put(defaultSettings)
                .put(settings)
                .put("name", id)
                .build();

        if (finalSettings.get("gateway.type") == null) {
            // default to non gateway
            finalSettings = settingsBuilder().put(finalSettings).put("gateway.type", "none").build();
        }
        if (finalSettings.get("cluster.routing.schedule") != null) {
            // decrease the routing schedule so new nodes will be added quickly
            finalSettings = settingsBuilder().put(finalSettings).put("cluster.routing.schedule", "50ms").build();
        }

        Node node = nodeBuilder()
                .settings(finalSettings)
                .build();
        nodes.put(id, node);
        clients.put(id, node.client());
        return node;
    }

    public void closeNode(String id) {
        Client client = clients.remove(id);
        if (client != null) {
            client.close();
        }
        Node node = nodes.remove(id);
        if (node != null) {
            node.close();
        }
    }

    public Node node(String id) {
        return nodes.get(id);
    }

    public Client client(String id) {
        return clients.get(id);
    }

    public void closeAllNodes() {
        for (Client client : clients.values()) {
            client.close();
        }
        clients.clear();
        for (Node node : nodes.values()) {
            node.close();
        }
        nodes.clear();
    }
}
package com.sql2jsonfeed;

import java.util.HashMap;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * @author Take Moa
 *
 */
public class ESClientManager {

	private static HashMap<String, Node> esNodeMap = new HashMap<String, Node>();
	
	/**
	 * Retrieve a client for the input ES cluser
	 * @param esClusterName
	 * @return
	 */
	public static Client get(String esClusterName) {
		synchronized (esNodeMap) {
			Node node = esNodeMap.get(esClusterName);
			if (node == null) {
				node = createClientNode(esClusterName);
				esNodeMap.put(esClusterName, node);
			} else if (node.isClosed()) {
				node.start();
			}
			return node.client();
		}
	}
	
	public static void close(String esClusterName) {
		synchronized (esNodeMap) {
			Node node = esNodeMap.get(esClusterName);
			if (node != null) {
				node.close();
				esNodeMap.remove(esClusterName);
			}
		}
	}
	
	public static void closeAll() {
		synchronized (esNodeMap) {
			for (Node node: esNodeMap.values()) {
				node.close();
			}
			esNodeMap.clear();;
		}
	}

	private static Node createClientNode(String esClusterName) {
		// TODO can we set a specific name???
//		Node node = NodeBuilder.nodeBuilder().client(true)
//				.clusterName(esClusterName).node();

        // TODO to config file
		Node node = NodeBuilder.nodeBuilder().client(true).settings(ImmutableSettings.builder()
				.put("cluster.name", esClusterName)
				.put("discovery.zen.ping.multicast.enabled", false)
				.putArray("discovery.zen.ping.unicast.hosts", "localhost:9300", "localhost:9301"))
				.node();

		return node;
	}
}

package org.apache.zeppelin.ignite;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import static org.apache.ignite.IgniteSystemProperties.IGNITE_JETTY_PORT;
import static org.apache.ignite.internal.IgniteNodeAttributes.ATTR_REST_JETTY_ADDRS;
import static org.apache.ignite.internal.IgniteNodeAttributes.ATTR_REST_JETTY_PORT;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.console.agent.AgentLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IgniteWebConsoleAgent extends Thread{
	private static Logger logger = LoggerFactory.getLogger(IgniteWebConsoleAgent.class);
	private static final String TOKENS = "web-console.tokens";
	private static final String SERVER_URL = "web-console.server-uri";
	private static volatile IgniteWebConsoleAgent instance = null;
	
	Properties properties;
	
	public static IgniteWebConsoleAgent create(Properties property) {
		if(instance==null) {
			instance = new IgniteWebConsoleAgent(property);
			instance.start();
		}
		return instance;
	}
	
	private IgniteWebConsoleAgent(Properties property) {
		super("IgniteWebConsoleAgent");
		this.properties = property;
	}
	
	public void run() {
		
		List<String> argsList = new ArrayList<>(6);
		String serverUrl = properties.getProperty(SERVER_URL,"");
		argsList.add("-s");
		argsList.add(serverUrl);
		
		
		String srvPortStr = System.getProperty(IGNITE_JETTY_PORT, "8080");
		
		for(Ignite ignite: Ignition.allGrids()) {
			ClusterNode node = ignite.cluster().localNode();
			Integer jettyPort = node.attribute(ATTR_REST_JETTY_PORT);
			if(jettyPort==null) continue;
			srvPortStr = jettyPort.toString();
		}
		
		argsList.add("-n");
		argsList.add("http://127.0.0.1:"+srvPortStr);
		
		
		String tokens = properties.getProperty(TOKENS,"");
		String[] tokenList = tokens.split(",|\\s");
		
		if(tokenList.length>0 && !tokenList[0].isEmpty()) {
			tokens = String.join(",",tokenList);
			argsList.add("-t");
			argsList.add(tokens);
			
			AgentLauncher.main(argsList.toArray(new String[argsList.size()]));
			return ;
		}
		else {
			logger.warn(TOKENS+ " is not set, Token can see at http://localhost:3000/settings/profile!");
		}
		instance = null;
		return ;
	}
	
	
}

package org.apache.zeppelin.groovy;

import org.apache.zeppelin.interpreter.Constants;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NextflowServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteInterpreterServer.class);

	public static void main(String[] args) throws Exception {
		System.setProperty("NXF_HOME","~/.nextflow");
		String zeppelinServerHost = "127.0.0.1";
	    int port = 57182;//Constants.ZEPPELIN_INTERPRETER_DEFAUlT_PORT+1000; //30914
	    String portRange = "30914:30915";
	    String interpreterGroupId = "nextflow-shared_process";
	    if (args.length > 0) {
	      zeppelinServerHost = args[0];
	      port = Integer.parseInt(args[1]);
	      interpreterGroupId = args[2];
	      if (args.length > 3) {
	        portRange = args[3];
	      }
	    }
	    RemoteInterpreterServer remoteInterpreterServer =
	        new RemoteInterpreterServer(zeppelinServerHost, port, interpreterGroupId, portRange);
	    remoteInterpreterServer.start();

	    

	    remoteInterpreterServer.join();
	    LOGGER.info("RemoteInterpreterServer thread is finished");

	}

}

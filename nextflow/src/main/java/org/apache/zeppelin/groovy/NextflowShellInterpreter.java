/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.groovy;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import org.apache.zeppelin.interpreter.ZeppelinContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nextflow.cli.Launcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.KerberosInterpreter;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import org.codehaus.groovy.runtime.StringBufferWriter;


/**
 * Shell interpreter for Zeppelin.nextflow
 */
public class NextflowShellInterpreter extends KerberosInterpreter {
  private static final Logger LOGGER = LoggerFactory.getLogger(NextflowShellInterpreter.class);

  private static final String TIMEOUT_PROPERTY = "shell.command.timeout.millisecs";
  private static final String DEFAULT_TIMEOUT = "60000";
  private static final String DIRECTORY_USER_HOME = "shell.working.directory.user.home";

  private final boolean isWindows = System.getProperty("os.name").startsWith("Windows");
  private final String shell = isWindows ? "cmd /c" : "bash -c";
  ConcurrentHashMap<String, DefaultExecutor> executors;
  
  PrintStream console = System.out;
  PrintStream consoleError = System.err;
  
  public NextflowShellInterpreter(Properties property) {
    super(property);
  }

  @Override
  public void open() {
    super.open();   
    LOGGER.info("Command timeout property: {}", getProperty(TIMEOUT_PROPERTY));
    executors = new ConcurrentHashMap<>();
  }

  @Override
  public void close() {
    super.close();
    for (String executorKey : executors.keySet()) {
      DefaultExecutor executor = executors.remove(executorKey);
      if (executor != null) {
        try {
          executor.getWatchdog().destroyProcess();
        } catch (Exception e){
          LOGGER.error("error destroying executor for paragraphId: " + executorKey, e);
        }
      }
    }
  }

  @Override
  protected boolean isInterpolate() {
    return Boolean.parseBoolean(getProperty("zeppelin.shell.interpolation", "false"));
  }

  @Override
  public ZeppelinContext getZeppelinContext() {
    return null;
  }

  @Override
  public InterpreterResult internalInterpret(String cmd,
                                             InterpreterContext context) {
    LOGGER.debug("Run shell command '{}'", cmd);

    CommandLine cmdLine = CommandLine.parse(shell);
    
    Launcher launcher = new Launcher();
    launcher.getOptions().setLogFile("logs/nextflow-shell.log");

    String optionsString = context.getStringLocalProperty("options", "");
    
    String[] lines = cmd.split("\n");   
    
    try {
     
      DefaultExecutor executor = null;
      int exitVal = 0;
      //ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream out = new PrintStream(context.out);
      System.setOut(out);
      System.setErr(out);
      for(String line: lines) {
  	    String args = line.trim();   
  	    if(args.length()<1 || args.charAt(0)=='#') {
  	    	continue;
  	    }
  	    else if(args.length()>5 && (args.startsWith("exec ") || args.startsWith("nextflow "))) {
  	    	if(args.startsWith("exec ")) {
  	    	    cmdLine.addArgument(args.substring(5), false);
  	    	}
  	    	else {
  	    	    cmdLine.addArgument(args.substring(8), false);
  	    	}
  	    	if(executor==null) {
  	    		executor = this.getExecuter(context);
  	    	}
  	    	exitVal = executor.execute(cmdLine);
  	    	
  	    }
  	    else {
	  	    if(optionsString!=null && optionsString.length()>0) {
	  	    	args = line+ " "+  optionsString;
	  	    }	  	        
	  	    launcher.command(args.split(" "));
	  	   
	  	    exitVal = launcher.run();
  	    }
  	    LOGGER.info("Paragraph {} {} return with exit value: {}", context.getParagraphId(),args, exitVal);
      }
      
      if (exitVal == 0) {
        return new InterpreterResult(Code.SUCCESS);
      } else {
        return new InterpreterResult(Code.ERROR);
      }
    } catch (ExecuteException e) {
      int exitValue = e.getExitValue();
      LOGGER.error("Can not run command: " + cmd, e);
      Code code = Code.ERROR;
      StringBuilder messageBuilder = new StringBuilder();
      if (exitValue == 143) {
        code = Code.INCOMPLETE;
        messageBuilder.append("Paragraph received a SIGTERM\n");
        LOGGER.info("The paragraph {} stopped executing: {}",
                context.getParagraphId(), messageBuilder.toString());
      }
      messageBuilder.append("ExitValue: " + exitValue);
      return new InterpreterResult(code, messageBuilder.toString());
    } catch (IOException e) {
      LOGGER.error("Can not run command: " + cmd, e);
      return new InterpreterResult(Code.ERROR, e.getMessage());
    } finally {
      executors.remove(context.getParagraphId());      
      System.setOut(console);
      System.setErr(consoleError);
    }
  }
  
  private DefaultExecutor getExecuter(InterpreterContext context) {
	  DefaultExecutor executor = new DefaultExecutor();
      executor.setStreamHandler(new PumpStreamHandler(context.out, context.out));

      executor.setWatchdog(new ExecuteWatchdog(
          Long.valueOf(getProperty(TIMEOUT_PROPERTY, DEFAULT_TIMEOUT))));
      executors.put(context.getParagraphId(), executor);
      if (Boolean.valueOf(getProperty(DIRECTORY_USER_HOME))) {
        executor.setWorkingDirectory(new File(System.getProperty("user.home")));
      }
      return executor;
  }

  @Override
  public void cancel(InterpreterContext context) {
    DefaultExecutor executor = executors.remove(context.getParagraphId());
    if (executor != null) {
      try {
        executor.getWatchdog().destroyProcess();
      } catch (Exception e){
        LOGGER.error("error destroying executor for paragraphId: " + context.getParagraphId(), e);
      }
    }
  }

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override
  public int getProgress(InterpreterContext context) {
    return 0;
  }

  @Override
  public Scheduler getScheduler() {
    return SchedulerFactory.singleton().createOrGetParallelScheduler(
        NextflowShellInterpreter.class.getName() + this.hashCode(), 10);
  }

  @Override
  protected boolean runKerberosLogin() {
    try {
      createSecureConfiguration();
      return true;
    } catch (Exception e) {
      LOGGER.error("Unable to run kinit for zeppelin", e);
    }
    return false;
  }

  public void createSecureConfiguration() throws InterpreterException {
    Properties properties = getProperties();
    CommandLine cmdLine = CommandLine.parse(shell);
    cmdLine.addArgument("-c", false);
    String kinitCommand = String.format("kinit -k -t %s %s",
        properties.getProperty("zeppelin.shell.keytab.location"),
        properties.getProperty("zeppelin.shell.principal"));
    cmdLine.addArgument(kinitCommand, false);
    DefaultExecutor executor = new DefaultExecutor();
    try {
      executor.execute(cmdLine);
    } catch (Exception e) {
      LOGGER.error("Unable to run kinit for zeppelin user " + kinitCommand, e);
      throw new InterpreterException(e);
    }
  }

  @Override
  protected boolean isKerboseEnabled() {
    if (getProperty("zeppelin.shell.auth.type")!=null && getProperty(
        "zeppelin.shell.auth.type").equalsIgnoreCase("kerberos")) {
      return true;
    }
    return false;
  }

}

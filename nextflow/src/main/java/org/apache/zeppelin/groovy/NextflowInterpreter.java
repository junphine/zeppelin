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

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.jsoup.helper.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovyx.gpars.dataflow.DataflowWriteChannel;
import nextflow.cli.CmdRun;
import nextflow.cli.Launcher;
import nextflow.script.ChannelOut;
import nextflow.script.ScriptBinding;
import nextflow.script.ScriptRunner;
import nextflow.util.HistoryFile;

import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;

import org.apache.zeppelin.scheduler.Job;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.scheduler.SchedulerFactory;

/**
 * Groovy interpreter for Zeppelin.
 */
public class NextflowInterpreter extends Interpreter {
  Logger log = LoggerFactory.getLogger(NextflowInterpreter.class);
  
 
  //here we will store Interpreters shared variables. concurrent just in case.
  Map<String, Object> sharedBindings = new ConcurrentHashMap<String, Object>();
  String processExecutor = "local";
  String profile = "";
  String options = "";
  
  PrintStream console = System.out;
  PrintStream consoleError = System.err;

  public NextflowInterpreter(Properties property) {
    super(property);
  }

  @Override
  public void open() {   
    processExecutor = getProperty("process.executor");
    profile = getProperty("profile");
    options = getProperty("options");
   
    log.info("nextflow process executor: " + processExecutor);
   
    if (profile != null && profile.length() > 0) {
      log.info("nextflow process profile: " + profile);      
    }
    if(options !=null && options.length()>0) {
      log.info("nextflow process options: " + options);
    }  
    
    File scriptFiles = new File("/tmp/nextflow");
    scriptFiles.mkdir();
    
  }

  @Override
  public void close() {
	  getScheduler().stop();
  }

  @Override
  public FormType getFormType() {
    return FormType.NATIVE;
  }

  @Override
  public int getProgress(InterpreterContext context) {
    return 0;
  }

  @Override
  public Scheduler getScheduler() {
    return SchedulerFactory.singleton()
        .createOrGetParallelScheduler(NextflowInterpreter.class.getName() + this.hashCode(), 10);
  }

  private Job getRunningJob(String paragraphId) {
    return getScheduler().getJob(paragraphId);
  }
 

  @Override
  public List<InterpreterCompletion> completion(String buf, int cursor,
                                                InterpreterContext interpreterContext)
      throws InterpreterException {
    List<InterpreterCompletion> results = new LinkedList<>();
    
    return results;
  }




  private static Set<String> predefinedBindings = new HashSet<String>();

  static {
    predefinedBindings.add("g");
    predefinedBindings.add("out");
  }

  @Override
  @SuppressWarnings("unchecked")
  public InterpreterResult interpret(String cmd, InterpreterContext contextInterpreter) {
	  String pipeName = contextInterpreter.getParagraphId();
	  if(!StringUtil.isBlank(contextInterpreter.getParagraphTitle())) {
		  pipeName=contextInterpreter.getParagraphTitle();
		  pipeName = pipeName.replace(' ', '_');
		  pipeName = pipeName.replace(',', '，');  
	  }
      
	
    try {
      File scriptFile = new File("/tmp/nextflow",contextInterpreter.getParagraphId()+".nf");
      FileWriter writer = new FileWriter(scriptFile);
      writer.write(cmd);
      writer.close();
      
      String nextFlowCmd = "run "+ scriptFile.getPath() + " -lib interpreter/nextflow ";
      String optionsString = contextInterpreter.getStringLocalProperty("options", "");
      if(this.options!=null && !this.options.isEmpty()) {
    	  nextFlowCmd +=  " " + options;
      }
      if(optionsString!=null && optionsString.length()>0) {    	  
    	  nextFlowCmd +=  " " + optionsString;
      }
      
      String [] strArgs = nextFlowCmd.split(" ");
      List<String> args = new ArrayList<>();       
      args.addAll(Arrays.asList(strArgs).subList(2, strArgs.length));
      
      Launcher launcher = new Launcher(true);     
      launcher.getOptions().setLogFile("logs/nextflow.log");
      launcher.command(strArgs);
      
      
      CmdRun cmdRun = (CmdRun) launcher.findCommand("run");
      
      Date now = new Date(); 
      int lenOffset = "paragraph_".length();
      DateFormat format = new SimpleDateFormat("MMdd'T'HHmmss");
      String runName = contextInterpreter.getNoteName();
      if(!StringUtil.isBlank(contextInterpreter.getParagraphTitle())) {
    	  runName+="-"+contextInterpreter.getParagraphTitle()+"_"+format.format(now);
      }
      else {
    	  runName+="-"+contextInterpreter.getParagraphId().substring(lenOffset)+"_"+format.format(now);
      }
      runName = runName.replace(' ', '_');
      runName = runName.replace(',', '，');     
      
      cmdRun.setRunName(runName);
      
      if (profile != null && profile.length() > 0) {
          log.info("nextflow process profile: " + profile);
          cmdRun.setProfile(profile);
      }
      
      ScriptRunner runner = cmdRun.runJob(scriptFile.getPath(),args);      
      
      Job runningJob = getRunningJob(contextInterpreter.getParagraphId());
      runningJob.info().put("CURRENT_THREAD", Thread.currentThread()); //to be able to terminate thread
      runningJob.info().put("CURRENT_RUNNER", runner); //to be able to terminate thread
      ScriptBinding bindings = runner.getSession().getBinding();      
     
      StringWriter out = new StringWriter();      
      PrintStream zout = new PrintStream(contextInterpreter.out);     //
      System.setOut(zout);
      System.setErr(zout);
      
      //put shared bindings evaluated in this interpreter
      for(Map.Entry<String,Object> ent: sharedBindings.entrySet()) {
         bindings.setVariable(ent.getKey(),ent.getValue());
      }
      //put predefined bindings
      bindings.setVariable("g", new GObject(log, out, this.getProperties(), contextInterpreter, sharedBindings));
      bindings.setVariable("out", new PrintWriter(out, true));

      
	  // -- run it!
      runner.execute(args, cmdRun.getEntryName());
      //let's get shared variables defined in current script and store them in shared map
      for (Map.Entry<String,Object> e : ((Map<String,Object>)bindings.getVariables()).entrySet()) {
        if (!predefinedBindings.contains(e.getKey())) {
          if (log.isTraceEnabled()) {
            log.trace("groovy script variable " + e);  //let's see what we have...
          }
          sharedBindings.put(e.getKey(), e.getValue());
        }
      }
      
      Object jobResult = runner.getResult();
      InterpreterResult result;
      if(jobResult instanceof ChannelOut) {
		  ChannelOut channelOut = (ChannelOut) jobResult;
		  result = new InterpreterResult(Code.SUCCESS);
		  if(!channelOut.isEmpty()) {
			  contextInterpreter.out.clear();
			  for(DataflowWriteChannel data: channelOut) {
				  result.add(data.toString());
			  }
		  }
		  for(String name: channelOut.getNames()) {
			  Object row = channelOut.getProperty(name);
		  }
	  }
      else if(jobResult==null || !(jobResult instanceof CharSequence)) { //not string    	  
    	  result = new InterpreterResult(Code.SUCCESS);
      }
      else {
    	  contextInterpreter.out.clear();
    	  result = new InterpreterResult(Code.SUCCESS, jobResult.toString());
      }
      if(out.getBuffer()!=null) {
    	  result.add(out.getBuffer().toString());
      }
      return result;
    } catch (Throwable t) {
      t = StackTraceUtils.deepSanitize(t);
      String msg = t.toString() + "\n at " + t.getStackTrace()[0];
      log.error("Failed to run script: " + t + "\n" + cmd + "\n", t);
      return new InterpreterResult(Code.ERROR, msg);
    }
    finally {          
      System.setOut(console);
      System.setErr(consoleError);
    }
  }

  @Override
  public void cancel(InterpreterContext context) {
    Job runningJob = getRunningJob(context.getParagraphId());
    if (runningJob != null) {
      Map<String, Object> info = runningJob.info();
      ScriptRunner runner = (ScriptRunner)info.get("CURRENT_RUNNER");
      if(runner!=null) {
    	  runner.getSession().abort();    	  
      }
      Object object = info.get("CURRENT_THREAD");
      if (object instanceof Thread) {
        try {
          Thread t = (Thread) object;
          Thread.sleep(10);
          t.dumpStack();          
          t.interrupt();
          //t.stop(); //TODO(dlukyanov): need some way to terminate maybe through GObject..
        } catch (Throwable t) {
          log.error("Failed to cancel script: " + t, t);
        }
      }
    }
  }
}

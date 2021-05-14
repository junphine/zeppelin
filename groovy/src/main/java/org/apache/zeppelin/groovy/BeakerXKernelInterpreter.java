/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.groovy;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.AbstractInterpreter;
import org.apache.zeppelin.interpreter.ZeppelinContext;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterResult;

import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.apache.zeppelin.interpreter.util.InterpreterOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twosigma.beakerx.AutotranslationServiceImpl;
import com.twosigma.beakerx.BeakerXCommRepository;
import com.twosigma.beakerx.DefaultBeakerXJsonSerializer;
import com.twosigma.beakerx.NamespaceClient;
import com.twosigma.beakerx.TryResult;
import com.twosigma.beakerx.autocomplete.AutocompleteResult;
import com.twosigma.beakerx.autocomplete.AutocompleteService;
import com.twosigma.beakerx.evaluator.BaseEvaluator;
import com.twosigma.beakerx.evaluator.ClasspathScannerImpl;
import com.twosigma.beakerx.groovy.evaluator.GroovyEvaluator;
import com.twosigma.beakerx.groovy.kernel.GroovyDefaultVariables;
import com.twosigma.beakerx.javash.evaluator.JavaEvaluator;
import com.twosigma.beakerx.jvm.object.Configuration;
import com.twosigma.beakerx.jvm.object.ConfigurationFactory;
import com.twosigma.beakerx.jvm.object.SimpleEvaluationObject;
import com.twosigma.beakerx.jvm.threads.BeakerInputHandler;
import com.twosigma.beakerx.jvm.threads.BeakerOutputHandler;
import com.twosigma.beakerx.jvm.threads.BxInputStream;
import com.twosigma.beakerx.jvm.threads.InputRequestMessageFactoryImpl;
import com.twosigma.beakerx.kernel.EvaluatorParameters;
import com.twosigma.beakerx.kernel.ExecutionOptions;
import com.twosigma.beakerx.kernel.GroupName;
import com.twosigma.beakerx.kernel.KernelConfigurationFile;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.kernel.SocketEnum;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandConfiguration;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandConfigurationImpl;
import com.twosigma.beakerx.kernel.msg.MessageCreator;
import com.twosigma.beakerx.kernel.msg.MessageHolder;
import com.twosigma.beakerx.kernel.threads.ResultSender;
import com.twosigma.beakerx.message.Header;
import com.twosigma.beakerx.message.Message;

import static com.twosigma.beakerx.DefaultJVMVariables.IMPORTS;
import static com.twosigma.beakerx.kernel.Utils.uuid;
import static com.twosigma.beakerx.kernel.msg.JupyterMessages.*;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Jupyter Kernel Interpreter for Zeppelin. One instance of this class represents one
 * Jupyter Kernel. You can enhance the jupyter kernel by extending this class.
 * e.g. IPythonInterpreter.
 */
public class BeakerXKernelInterpreter extends AbstractInterpreter {

  private static final Logger LOGGER = LoggerFactory.getLogger(BeakerXKernelInterpreter.class);
  
  public static  DefaultBeakerXJsonSerializer serializer = new DefaultBeakerXJsonSerializer();
  protected BaseEvaluator evaluator;  
  protected ZeppelinContext z;

  private String kernel;
  // working directory of jupyter kernel
  protected File kernelWorkDir;
  
  ConfigurationFactoryImpl configurationFactoryImpl;
  
  int executionCount;
 

  private InterpreterOutputStream interpreterOutput = new InterpreterOutputStream(LOGGER);

  public BeakerXKernelInterpreter(String kernel, Properties properties) {
    this(properties);
    this.kernel = kernel;
  }

  public BeakerXKernelInterpreter(Properties properties) {
    super(properties);
    kernel = this.getProperty("beakerx.kernel","groovy");
  }

  public String getKernelName() {
    return this.kernel;
  }

 

  protected ZeppelinContext buildZeppelinContext() {
    return new GroovyZeppelinContext(null, 1000);
  }

  @Override
  public void open() throws InterpreterException {
    try {
      if (evaluator != null) {
        // JupyterKernelInterpreter might already been opened
        return;
      }
     
      String checkPrerequisiteResult = checkKernelPrerequisite(kernel);
      if (!StringUtils.isEmpty(checkPrerequisiteResult)) {
        throw new InterpreterException("Kernel prerequisite is not meet: " +
                checkPrerequisiteResult);
      }
     
      this.z = buildZeppelinContext();
      
      String id = uuid();
      
      BeakerXCommRepository beakerXCommRepository = new BeakerXCommRepository();
     
      NamespaceClient namespaceClient = new NamespaceClient(AutotranslationServiceImpl.createAsMainKernel(id), serializer, beakerXCommRepository);
      //NamespaceClient namespaceClient = NamespaceClient.create(id, configurationFile, beakerXCommRepository);
      MagicCommandConfiguration magicCommandTypesFactory = new MagicCommandConfigurationImpl();
      if(kernel.equalsIgnoreCase("groovy")){
	      GroovyEvaluator groovyEvaluator = new GroovyEvaluator(id,
	              id,
	              getEvaluatorParameters(),
	              namespaceClient,
	              magicCommandTypesFactory.patterns(),
	              new ClasspathScannerImpl());
	      
	      this.evaluator = groovyEvaluator;  
      }
      else {
    	  JavaEvaluator groovyEvaluator = new JavaEvaluator(id,
	              id,
	              getEvaluatorParameters(),
	              namespaceClient,
	              magicCommandTypesFactory.patterns(),
	              new ClasspathScannerImpl());
	      
	      this.evaluator = groovyEvaluator;  
      }
      
      Message message = new Message(new Header(EXECUTE_REQUEST, id));
      
      configurationFactoryImpl = new ConfigurationFactoryImpl(message,executionCount);
     
    } catch (Exception e) {
      throw new InterpreterException("Fail to open JupyterKernelInterpreter:\n" +
              ExceptionUtils.getStackTrace(e), e);
    }
  }

  /**
   * non-empty return value mean the errors when checking kernel prerequisite.
   * empty value mean kernel prerequisite is met.
   *
   * @return check result of checking kernel prerequisite.
   */
  public String checkKernelPrerequisite(String pythonExec) {
   
    File stderrFile = null;
    File stdoutFile = null;
    try {
      stderrFile = File.createTempFile("zeppelin", ".txt");
      
    } catch (Exception e) {
      LOGGER.warn("Fail to checkKernelPrerequisite", e);
      return "Fail to checkKernelPrerequisite: " + ExceptionUtils.getStackTrace(e);
    } finally {
      FileUtils.deleteQuietly(stderrFile);
      FileUtils.deleteQuietly(stdoutFile);
    }
    return "";
  }

  

  protected Map<String, String> setupKernelEnv() throws IOException {
    return EnvironmentUtils.getProcEnvironment();
  }

  

  @Override
  public void close() throws InterpreterException {	  
	  evaluator.exit();    
  }

  @Override
  public InterpreterResult internalInterpret(String st,
                                     InterpreterContext context) throws InterpreterException {
    
    interpreterOutput.setInterpreterOutput(context.out);
    executionCount++;
   
    try {
      SimpleEvaluationObject seo = new SimpleEvaluationObject(st, configurationFactoryImpl);
      ExecutionOptions executionOptions  = new ExecutionOptions(GroupName.of(context.getParagraphId()));
      TryResult result = evaluator.evaluate(seo, st, executionOptions);
     
      interpreterOutput.getInterpreterOutput().flush();
    
      if (result.isError()) {
        return new InterpreterResult(
                InterpreterResult.Code.ERROR,result.error());
      } else {
       if(result.result() instanceof String) {
    	   return new InterpreterResult(InterpreterResult.Code.SUCCESS,result.result().toString());
       }
       else {
           return new InterpreterResult(InterpreterResult.Code.SUCCESS,result.result().toString());
       }
      }
    } catch (Exception e) {
      return new InterpreterResult(InterpreterResult.Code.ERROR,
                "BeakerX kernel is abnormally exited, please check your code and log. "+ e.getMessage());
    }
  }

  @Override
  public void cancel(InterpreterContext context) throws InterpreterException {  
    evaluator.cancelExecution(GroupName.of(context.getParagraphId()));
  }

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override
  public int getProgress(InterpreterContext context) throws InterpreterException {
    return 0;
  }

  @Override
  public List<InterpreterCompletion> completion(String buf, int cursor,
                                                InterpreterContext interpreterContext) {
    
    AutocompleteResult response = this.evaluator.autocomplete(buf,cursor);
    
    List<InterpreterCompletion> completions = new ArrayList<>();
   
    for (int i = 0; i < response.getMatches().size(); i++) {
      String match = response.getMatches().get(i);
      int lastIndexOfDot = match.lastIndexOf(".");
      if (lastIndexOfDot != -1) {
        match = match.substring(lastIndexOfDot + 1);
      }      
      completions.add(new InterpreterCompletion(match, match, ""));
    }
    return completions;
  }

  @Override
  public ZeppelinContext getZeppelinContext() {
    return z;
  }

  public static EvaluatorParameters getEvaluatorParameters() {
    HashMap<String, Object> kernelParameters = new HashMap<>();
    kernelParameters.put(IMPORTS, new GroovyDefaultVariables().getImports());
    return new EvaluatorParameters(kernelParameters);
  }  
  
  	public class ZeppelinResultSender implements ResultSender {
  		
  		
		@Override
		public void update(SimpleEvaluationObject seo) {
			if (seo != null) {
			      List<MessageHolder> message = MessageCreator.createMessage(seo);
			      message.forEach(job -> {
			        if (SocketEnum.IOPUB_SOCKET.equals(job.getSocketType())) {
			         
			        } else if (SocketEnum.SHELL_SOCKET.equals(job.getSocketType())) {
			        	try {
							interpreterOutput.write(job.getMessage().toString().getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			        }
			      });
			    }
		}

		@Override
		public void exit() {
			
		}
  		
  	}
  	
	public class ConfigurationFactoryImpl implements ConfigurationFactory {
	 
	  private Message message;
	  private int executionCount;
	
	  public ConfigurationFactoryImpl(Message message, int executionCount) {	   
	    this.message = message;
	    this.executionCount = executionCount;
	  }
	
	  @Override
	  public Configuration create(SimpleEvaluationObject seo) {
		ZeppelinResultSender resultSender = new ZeppelinResultSender();
		
		BeakerInputHandler stdin = new BeakerInputHandler() {
			@Override
			public int read() {				
				return 0;
			}			
		};
	    
	    BeakerOutputHandler stdout = new SimpleEvaluationObject.SimpleOutputHandler(false, resultSender, seo);
	    BeakerOutputHandler stderr = new SimpleEvaluationObject.SimpleOutputHandler(true, resultSender, seo);
	    return new Configuration(stdin, stdout, stderr, resultSender, message, executionCount);
	  }
	}
 
}

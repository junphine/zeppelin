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
package org.apache.zeppelin.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.zeppelin.groovy.GroovyInterpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.scheduler.SchedulerFactory;

/**
 * Apache Ignite interpreter (http://ignite.incubator.apache.org/).
 *
 * Use the following properties for interpreter configuration:
 *
 * <ul>
 * <li>{@code ignite.addresses} - coma separated list of hosts in form
 * {@code <host>:<port>} or {@code <host>:<port_1>..<port_n>}</li>
 * <li>{@code ignite.clientMode} - indicates that Ignite interpreter should
 * start node in client mode ({@code true} or {@code false}).</li>
 * <li>{@code ignite.peerClassLoadingEnabled} - enables/disables peer class
 * loading ({@code true} or {@code false}).</li>
 * <li>{@code ignite.config.url} - URL for Ignite configuration. If this URL
 * specified then all aforementioned properties will not be taken in
 * account.</li>
 * </ul>
 */
public class IgniteInterpreter extends GroovyInterpreter {
	static final String IGNITE_ADDRESSES = "ignite.addresses";

	static final String IGNITE_CLIENT_MODE = "ignite.clientMode";

	static final String IGNITE_PEER_CLASS_LOADING_ENABLED = "ignite.peerClassLoadingEnabled";

	static final String IGNITE_CFG_URL = "ignite.config.url";

	private Logger logger = LoggerFactory.getLogger(IgniteInterpreter.class);
	private Ignite ignite;

	private Throwable initEx;

	public IgniteInterpreter(Properties property) {
		super(property);
	}

	@Override
	public void open() {
		super.open();

		URL[] urls = getClassloaderUrls();

		// set classpath

		StringBuilder sb = new StringBuilder();

		for (File f : currentClassPath()) {
			if (sb.length() > 0) {
				sb.append(File.pathSeparator);
			}
			sb.append(f.getAbsolutePath());
		}

		if (urls != null) {
			for (URL u : urls) {
				if (sb.length() > 0) {
					sb.append(File.pathSeparator);
				}
				sb.append(u.getFile());
			}
		}

		initIgnite();
	}

	private List<File> currentClassPath() {
		List<File> paths = classPath(Thread.currentThread().getContextClassLoader());
		String[] cps = System.getProperty("java.class.path").split(File.pathSeparator);

		for (String cp : cps) {
			paths.add(new File(cp));
		}

		return paths;
	}

	private List<File> classPath(ClassLoader cl) {
		List<File> paths = new LinkedList<>();

		if (cl == null) {
			return paths;
		}

		if (cl instanceof URLClassLoader) {
			URLClassLoader ucl = (URLClassLoader) cl;
			URL[] urls = ucl.getURLs();
			if (urls != null) {
				for (URL url : urls) {
					paths.add(new File(url.getFile()));
				}
			}
		}

		return paths;
	}

	private Ignite getIgnite() {
		if (ignite == null) {
			try {
				String cfgUrl = getProperty(IGNITE_CFG_URL);

				if (cfgUrl != null && !cfgUrl.isEmpty()) {
					ignite = Ignition.start(new URL(cfgUrl));
				} else {
					IgniteConfiguration conf = new IgniteConfiguration();

					conf.setClientMode(Boolean.parseBoolean(getProperty(IGNITE_CLIENT_MODE)));

					TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
					ipFinder.setAddresses(getAddresses());

					TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
					discoSpi.setIpFinder(ipFinder);
					conf.setDiscoverySpi(discoSpi);

					conf.setPeerClassLoadingEnabled(
							Boolean.parseBoolean(getProperty(IGNITE_PEER_CLASS_LOADING_ENABLED)));

					ignite = Ignition.start(conf);
				}

				initEx = null;
			} catch (Exception e) {
				logger.error("Error in IgniteInterpreter while getIgnite: ", e);
				initEx = e;
			}
		}
		return ignite;
	}

	private void initIgnite() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try {

			Map<String, Object> binder = super.sharedBindings;

			if (getIgnite() != null) {
				binder.put("ignite", ignite);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	@Override
	public void close() {
		initEx = null;

		if (ignite != null) {
			ignite.close();
			ignite = null;
		}

		super.close();
	}

	private List<String> getAddresses() {
		String prop = getProperty(IGNITE_ADDRESSES);

		if (prop == null || prop.isEmpty()) {
			return Collections.emptyList();
		}

		String[] tokens = prop.split(",");
		List<String> addresses = new ArrayList<>(tokens.length);
		Collections.addAll(addresses, tokens);

		return addresses;
	}

	@Override
	public InterpreterResult interpret(String line, InterpreterContext context) {
		if (initEx != null) {
			return IgniteInterpreterUtils.buildErrorResult(initEx);
		}

		if (line == null || line.trim().length() == 0) {
			return new InterpreterResult(Code.SUCCESS);
		}

		return super.interpret(line, context);
	}

	@Override
	public void cancel(InterpreterContext context) {
		super.cancel(context);
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
	public List<InterpreterCompletion> completion(String buf, int cursor, InterpreterContext interpreterContext) {
		List<InterpreterCompletion> results = new LinkedList<>();
		groovyCompleter.completion(buf, cursor, results);
		return results;
	}

	@Override
	public Scheduler getScheduler() {
		return SchedulerFactory.singleton()
				.createOrGetFIFOScheduler(IgniteInterpreter.class.getName() + this.hashCode());
	}
}

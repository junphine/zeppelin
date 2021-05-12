package org.apache.zeppelin.ml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.zeppelin.display.AngularObject;
import org.apache.zeppelin.display.AngularObjectWatcher;
import org.apache.zeppelin.helium.Application;
import org.apache.zeppelin.helium.ApplicationContext;
import org.apache.zeppelin.helium.ApplicationException;
import org.apache.zeppelin.helium.ZeppelinApplicationDevServer;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;
import org.apache.zeppelin.resource.LocalResourcePool;
import org.apache.zeppelin.resource.Resource;
import org.apache.zeppelin.resource.ResourceSet;
import org.apache.zeppelin.tabledata.ColumnDef;
import org.apache.zeppelin.tabledata.InterpreterResultTableData;
import org.apache.zeppelin.tabledata.Row;
import org.apache.zeppelin.tabledata.TableData;
import org.apache.zeppelin.tabledata.TableDataProxy;
import org.codehaus.plexus.util.IOUtil;

import jsat.classifiers.CategoricalData;
import jsat.classifiers.DataPoint;
import jsat.classifiers.trees.DecisionStump;
import jsat.linear.Vec;
import jsat.regression.MultipleLinearRegression;
import jsat.regression.RegressionDataSet;
import jsat.regression.Regressor;
import jsat.regression.StochasticGradientBoosting;

public class DeepLearning extends Application {

	private ApplicationContext context;
	private Regressor model;
	private TableData tableData;
	private int labelCol;
	private List<Integer> valueCols = new ArrayList<>();

	public DeepLearning(ApplicationContext context) {
		super(context);
		this.context = context;
	}

	class ButtonWatcher extends AngularObjectWatcher {

		private String name;

		public ButtonWatcher(InterpreterContext context, String name) {
			super(context);
			this.name = name;
		}

		@Override
		public void watch(Object oldObject, Object newObject, InterpreterContext ctx) {

			System.err.println("Change " + name + " : " + oldObject + " -> " + newObject);
			if (name.equals("run") && newObject.equals("running")) {
				int numIterations = Integer.parseInt(get(context, "iteration").toString());

				String algorithm = get(context,"algorithm").toString();
				
				LinkedList<DataPoint> vectors = new LinkedList<DataPoint>();
				Iterator<Row> rows = tableData.rows();
				RegressionDataSet dataset = new RegressionDataSet(valueCols.size(), CategoricalData.EmptyDatas);
				while (rows.hasNext()) {
					Row row = rows.next();
					Object[] objects = row.get();
					double label = Double.parseDouble(objects[labelCol].toString());
					DataPoint dp = buildDataPoint(objects);
					dataset.addDataPoint(dp, label);
				}

				
				
				if(algorithm.equals("mlr")) {
					MultipleLinearRegression mlr = new MultipleLinearRegression(false);					
					model = mlr;
				}
				else {
					StochasticGradientBoosting sgb = new StochasticGradientBoosting(new DecisionStump(), numIterations);
					
					model = sgb;
				}
				try {
					model.train(dataset);
				}
				catch(Exception e) {
					put(context, "errorMessage", e.getMessage());
					e.printStackTrace();
					return;
				}
				
				put(context, "run", "idle");
				put(context, "predictBtn", 1);
				DeepLearning.this.watch(context, "predictBtn");
			}
			if (name.equals("predictBtn")) {
				String predictInput = "0,"+get(context,"predictInput").toString();
				String[] objects = predictInput.split(",|\\s");
				DataPoint dp = buildDataPoint(objects);
				double label = model.regress(dp);
				put(context, "predictedValue", Math.round(label*100)/100.0);
			}
		}

	}

	public DataPoint buildDataPoint(Object[] objects) {
		Vec vec = Vec.zeros(valueCols.size());
		int i = 0;
		for (int c : this.valueCols) {
			double val = Double.parseDouble(objects[c].toString());
			vec.set(i, val);
			i++;
		}
		DataPoint dp = new DataPoint(vec);
		return dp;
	}

	public AngularObject put(ApplicationContext context, String name, Object v) {
		AngularObject angular = context.getAngularObjectRegistry().add(name, v);
		return angular;
	}

	public Object get(ApplicationContext context, String name) {
		AngularObject angular = context.getAngularObjectRegistry().get(name);
		if (angular != null) {
			return angular.get();
		}
		return null;
	}

	public void watch(ApplicationContext context, String name) {
		InterpreterContext ctx = InterpreterContext.get();
		ButtonWatcher watcher = new ButtonWatcher(ctx, name);
		AngularObject angular = context.getAngularObjectRegistry().get(name);
		angular.addWatcher(watcher);
	}

	@Override
	public void run(ResourceSet resources) throws ApplicationException, IOException {

		// load resource from classpath
		context.out.writeResource("dl/DeepLearning.html");

		this.put(context, "run", "idle");
		this.put(context, "iteration", 100);
		this.put(context, "algorithm", "sgb");
		this.watch(context, "run");

		this.put(context, "predictBtn", 0);
		this.watch(context, "predictBtn");
		this.put(context, "predictInput", 0);
		this.put(context, "predictedValue", 0);

		// get TableData
		String className = InterpreterResultTableData.class.getName();
		ResourceSet tableResource = resources.filterByClassname(className);
		tableData = (TableData)tableResource.getFirst().get();

		Iterator<Resource> it = resources.iterator();
		while (it.hasNext()) {
			Resource info = it.next();

		}

		labelCol = -1;
		valueCols.clear();
		// find first numeric column
		ColumnDef[] columnDef = tableData.columns();

		Iterator<Row> rows = tableData.rows();
		Object[] row = rows.next().get();
		for (int c = 0; c < columnDef.length; c++) {
			try {
				Double.parseDouble(row[c].toString());
				if (labelCol == -1) {
					labelCol = c;
					continue;
				} else {
					valueCols.add(c);
					
				}
			} catch (Exception e) {
				continue;
			}
		}

		if (labelCol == -1 || valueCols.size() == 0) {
			throw new ApplicationException("Numeric column not found");
		}
	}

	@Override
	public void unload() throws ApplicationException {
		if (model != null) {
			model = null;
		}
	}

	private static InterpreterResultMessage generateData() throws IOException {
		InputStream ins = ClassLoader.getSystemResourceAsStream("dl/mockdata.tsv");
		String data = IOUtil.toString(ins);
		return new InterpreterResultMessage(InterpreterResult.Type.TABLE, data);
	}

	/**
	 * Development mode
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		LocalResourcePool pool = new LocalResourcePool("dev");
		TableData tableData = new InterpreterResultTableData(generateData());

		pool.put("tabledata", tableData);

		pool.put("date", new Date());

		// create development server
		ZeppelinApplicationDevServer devServer = new ZeppelinApplicationDevServer(DeepLearning.class.getName(),
				pool.getAll());

		devServer.start();
		devServer.join();
	}

}

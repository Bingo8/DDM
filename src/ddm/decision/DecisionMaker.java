// Distributed Decision making system framework 
// Copyright (c) 2014, Jordi Coll Corbilla
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// - Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
// - Redistributions in binary form must reproduce the above copyright notice,
// this list of conditions and the following disclaimer in the documentation
// and/or other materials provided with the distribution.
// - Neither the name of this library nor the names of its contributors may be
// used to endorse or promote products derived from this software without
// specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package ddm.decision;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import ddm.configuration.ManagerConfiguration;
import ddm.ontology.ClassificationResult;
import ddm.ontology.DataInstance;

/**
 * 
 * @author jordi Corbilla
 * Decision making class. This class gathers all the information that each agent sends,
 * sends the information through the aggregation operator and returns a value that can be
 * contrasted against a stored image (value). Then the decision is made according to a range.
 * The decision is saved with the original data in a new file that has been configured previously
 * in the configuration section.
 */
public class DecisionMaker {

	private File file = null;
	private BufferedWriter bw = null;
	private FileWriter fw = null;
	private OutputImagesList Classification = new OutputImagesList();
	long startTest = 0;
	long endTest = 0;

	public DecisionMaker(ManagerConfiguration conf) {
		// Create the file where all the data will be stored
		file = new File(conf.getOutputFileLocation());

		try {
			startTest = System.currentTimeMillis();
			file.createNewFile();
			fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);

			bw.write("***************************************************************************"
					+ "\r\n");
			bw.write("This file has been auto generated by DDM Distributed Decision Making System"
					+ "\r\n");
			bw.write("***************************************************************************"
					+ "\r\n");
			bw.write("" + "\r\n");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Final decision using Weighted Arithmetic Mean
	 * @param dataToPredict
	 * @param decisionResult
	 * @param trainingSize
	 */
	private void MakeWAM(DataInstance dataToPredict,
			HashMap<String, ClassificationResult> decisionResult,
			int trainingSize) {
		long startTimeMillis = System.currentTimeMillis();
		DecisionRange decisionRange = new DecisionRange();
		double accumulated = 0.0;
		double percentageAccumulated = 0.0;
		String description = "";
		for (int i = 1; i <= decisionResult.size(); i++) {
			ClassificationResult cr = decisionResult.get("Classifier" + i);
			double percentage = cr.getTrainingSize() / (double) trainingSize;
			double prediction = cr.getInstancePredictedValue();
			decisionRange.AddItem(cr.getPredictedInstanceValue(), prediction);
			Classification.AddItem(cr.getPredictedInstanceValue(), prediction);
			accumulated = accumulated + (percentage * prediction);
			percentageAccumulated = percentageAccumulated + percentage;

			java.text.DecimalFormat percentageFormatter = new java.text.DecimalFormat(
					"#0.00");
			String text = percentageFormatter.format(percentage);

			if (decisionResult.size() > 1) // There is no need to generate
											// output for only 1 classifier.
				description = description + "\tClassifier" + i + " ("
						+ cr.getType() + ") decision:"
						+ cr.getPredictedInstanceValue() + "(" + text + "%) "
						+ cr.getDuration() + "ms \r\n";
			else
				description = description + "\tClassifier" + i + " ("
						+ cr.getType() + ") " + cr.getDuration() + "ms \r\n";
		}

		double WeightedMean = accumulated / percentageAccumulated;
		String value = decisionRange.testValue(WeightedMean);

		long finishTimeMillis = System.currentTimeMillis();
		long duration = (finishTimeMillis - startTimeMillis);

		try {
			java.text.DecimalFormat percentageFormatter = new java.text.DecimalFormat(
					"#0.00");
			String text = percentageFormatter.format(WeightedMean);
			String disagree = "";
			if (!dataToPredict.getValue().contains(value))
				disagree = " **disagreed";
			bw.write(dataToPredict.getValue().toString() + " \r\n"
					+ description + "\t\tFinal Decision using WAW (" + value
					+ ") value:" + text + " in " + duration + "ms" + disagree
					+ "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
	}

	/**
	 * Decision using the OWA operator
	 * @param dataToPredict
	 * @param decisionResult
	 * @param trainingSize
	 */
	private void MakeOWA(DataInstance dataToPredict,
			HashMap<String, ClassificationResult> decisionResult,
			int trainingSize) {
		long startTimeMillis = System.currentTimeMillis();

		// First we need to get the total of the weights
		double accumulated = 0.0;
		DecisionRange decisionRange = new DecisionRange();
		for (int i = 1; i <= decisionResult.size(); i++) {
			ClassificationResult cr = decisionResult.get("Classifier" + i);
			double percentage = cr.getTrainingSize() / (double) trainingSize;
			double prediction = cr.getInstancePredictedValue();
			decisionRange.AddItem(cr.getPredictedInstanceValue(), prediction);
			Classification.AddItem(cr.getPredictedInstanceValue(), prediction);
			accumulated = accumulated + (percentage);
		}

		// Create vector W
		Vector<Double> W = new Vector<Double>(decisionResult.size());
		// Create vector A
		Vector<Double> A = new Vector<Double>(decisionResult.size());

		String description = "";

		for (int i = 1; i <= decisionResult.size(); i++) {
			ClassificationResult cr = decisionResult.get("Classifier" + i);
			double percentage = cr.getTrainingSize() / (double) trainingSize;
			W.addElement(new Double(percentage / accumulated));
			double prediction = cr.getInstancePredictedValue();
			A.addElement(new Double(prediction));

			java.text.DecimalFormat percentageFormatter = new java.text.DecimalFormat(
					"#0.00");
			String text = percentageFormatter.format(percentage * 100.0);

			if (decisionResult.size() > 1) // There is no need to generate
											// output for only 1 classifier.
				description = description + "\tClassifier" + i + " ("
						+ cr.getType() + ") decision:"
						+ cr.getPredictedInstanceValue() + "(" + text + "%) "
						+ cr.getDuration() + "ms \r\n";
			else
				description = description + "\tClassifier" + i + " ("
						+ cr.getType() + ") " + cr.getDuration() + "ms \r\n";
		}

		AggregationOperator ao = new AggregationOperator();
		double owaResult = ao.owa(W, A);
		String value = decisionRange.testValue(owaResult);

		long finishTimeMillis = System.currentTimeMillis();
		long duration = (finishTimeMillis - startTimeMillis);
		try {
			java.text.DecimalFormat percentageFormatter = new java.text.DecimalFormat(
					"#0.00");
			String text = percentageFormatter.format(owaResult);
			String disagree = "";
			if (!dataToPredict.getValue().contains(value))
				disagree = " **disagreed";
			bw.write(dataToPredict.getValue().toString() + "\r\n" + description
					+ "\t\tFinal Decision using OWA (" + value + ") value:"
					+ text + " in " + duration + "ms" + disagree + "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
	}

	/**
	 * Method that generates the decision output
	 * @param type
	 * @param dataToPredict
	 * @param decisionResult
	 * @param trainingSize
	 */
	public void Make(DecisionType type, DataInstance dataToPredict,
			HashMap<String, ClassificationResult> decisionResult,
			int trainingSize) {
		switch (type) {
		case WeightedArithmeticMean:
			MakeWAM(dataToPredict, decisionResult, trainingSize);
			break;
		case OrderedWeightedAggregation:
			MakeOWA(dataToPredict, decisionResult, trainingSize);
			break;
		}
	}

	/**
	 * Method to close the file. 
	 * THe file remains open during the entire processing of the information sent by the agents.
	 */
	public void CloseFile() {
		try {
			endTest = System.currentTimeMillis();
			long duration = (endTest - startTest);
			bw.write("\r\n\r\n\r\n***************************************************************************"
					+ "\r\n");
			bw.write(this.Classification.ToString());
			bw.write("***************************************************************************"
					+ "\r\n");
			bw.write("Duration " + duration + "ms\r\n");
			bw.close();
		} catch (Exception e) {
		}
	}
}

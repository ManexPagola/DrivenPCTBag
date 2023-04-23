/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    J48Consolidated.java
 *    Copyright (C) 2021 ALDAPA Team (http://www.aldapa.eus)
 *    Faculty of Informatics, Donostia, 20018
 *    University of the Basque Country (UPV/EHU), Basque Country
 *    
 */

package weka.classifiers.trees;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.Sourcable;
import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45ModelSelection;
import weka.classifiers.trees.j48.ModelSelection;
import weka.classifiers.trees.j48.PruneableClassifierTree;
import weka.classifiers.trees.j48It.C45PruneableClassifierTreeIt;
import weka.core.AdditionalMeasureProducer;
import weka.core.Drawable;
import weka.core.Instances;
import weka.core.Matchable;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Summarizable;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

public class J48It extends J48 implements OptionHandler, Drawable, Matchable, Sourcable, WeightedInstancesHandler,
		Summarizable, AdditionalMeasureProducer, TechnicalInformationHandler {

	private static final long serialVersionUID = 5936918151210707000L;

	/** Build the tree level by level, rather than in pre-order */
	protected int m_ITmaximumLevel = 0;

	/**
	 * Generates the classifier.
	 * 
	 * @param instances the data to train the classifier with
	 * @throws Exception if classifier can't be built successfully
	 */
	@Override
	public void buildClassifier(Instances instances) throws Exception {

		if ((m_unpruned) && (!m_subtreeRaising)) {
			throw new Exception("Subtree raising does not need to be unset for unpruned trees!");
		}
		if ((m_unpruned) && (m_reducedErrorPruning)) {
			throw new Exception("Unpruned tree and reduced error pruning cannot be selected simultaneously!");
		}
		if ((m_unpruned) && (m_CF != 0.25f)) {
			throw new Exception("It does not make sense to change the confidence for an unpruned tree!");
		}
		if ((m_reducedErrorPruning) && (m_CF != 0.25f)) {
			throw new Exception("Changing the confidence does not make sense for reduced error pruning.");
		}
		if ((!m_reducedErrorPruning) && (m_numFolds != 3)) {
			throw new Exception(
					"Changing the number of folds does not make sense if" + " reduced error pruning is not selected.");
		}
		if ((!m_reducedErrorPruning) && (m_Seed != 1)) {
			throw new Exception("Changing the seed does not make sense if" + " reduced error pruning is not selected.");
		}
		if ((m_CF <= 0) || (m_CF >= 1)) {
			throw new Exception("Confidence has to be greater than zero and smaller than one!");
		}
		getCapabilities().testWithFail(instances);

		ModelSelection modSelection;

		if (m_binarySplits) {
			modSelection = new BinC45ModelSelection(m_minNumObj, instances, m_useMDLcorrection,
					m_doNotMakeSplitPointActualValue);
		} else {
			modSelection = new C45ModelSelection(m_minNumObj, instances, m_useMDLcorrection,
					m_doNotMakeSplitPointActualValue);
		}
		if (!m_reducedErrorPruning) {
			m_root = new C45PruneableClassifierTreeIt(modSelection, !m_unpruned, m_CF, m_subtreeRaising, !m_noCleanup,
					m_collapseTree, m_ITmaximumLevel);
		} else {
			m_root = new PruneableClassifierTree(modSelection, !m_unpruned, m_numFolds, !m_noCleanup, m_Seed);
		}
		m_root.buildClassifier(instances);
		if (m_binarySplits) {
			((BinC45ModelSelection) modSelection).cleanup();
		} else {
			((C45ModelSelection) modSelection).cleanup();
		}
	}

	/**
	 * Returns an enumeration describing the available options.
	 * 
	 * Valid options are:
	 * <p>
	 * 
	 * J48 options<br/>
	 * =============<br/>
	 *
	 * Options to build the tree partially (the most important change is that it is
	 * run iteratively (IT) instead of recursively).
	 * ============================================================================
	 * -IT-ML <br>
	 * Build the tree with a maximum number of levels.
	 * <p>
	 * 
	 * @return an enumeration of all the available options.
	 */
	@Override
	public Enumeration<Option> listOptions() {
		Vector<Option> newVector = new Vector<Option>(1);

		newVector.addElement(new Option("\tBuild the tree with a maximum number of levels.", "IT-ML", 0, "-IT-ML"));

		newVector.addAll(Collections.list(super.listOptions()));
		return newVector.elements();
	}

	/**
	 * Returns an enumeration describing the available options.
	 * 
	 * Valid options are:
	 * <p>
	 * 
	 * J48 options<br/>
	 * =============<br/>
	 *
	 * Options to build the tree partially (the most important change is that it is
	 * run iteratively (IT) instead of recursively).
	 * ============================================================================
	 * -IT-ML <br>
	 * Build the tree with a maximum number of levels.
	 * <p>
	 * 
	 * @return an enumeration of all the available options.
	 */
	@Override
	public void setOptions(String[] options) throws Exception {
		String m_ITmaximumLevelString = Utils.getOption("IT-ML", options);

		if (m_ITmaximumLevelString.length() != 0) {
			m_ITmaximumLevel = Integer.parseInt(m_ITmaximumLevelString);
		} else {
			m_ITmaximumLevel = 0;
		}
		super.setOptions(options);
	}

	/**
	 * Gets the current settings of the Classifier.
	 * 
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override
	public String[] getOptions() {

		Vector<String> options = new Vector<String>();
		Collections.addAll(options, super.getOptions());

		options.add("-IT-ML");
	    options.add("" + m_ITmaximumLevel);
		return options.toArray(new String[0]);
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String ITmaximumLevelTipText() {
		return "Build the tree with a maximum number of levels, if it is 0 the default levels are built";
	}

	/**
	 * Get the value of ITmaximumLevel.
	 * 
	 * @return Value of ITmaximumLevel.
	 */
	public int getITmaximumLevel() {
		return m_ITmaximumLevel;
	}

	/**
	 * Set the value of ITmaximumLevel.
	 * 
	 * @param v Value to assign to ITmaximumLevel.
	 */
	public void setITmaximumLevel(int ITmaximumLevel) {
		this.m_ITmaximumLevel = ITmaximumLevel;
	}



}
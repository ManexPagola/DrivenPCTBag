package weka.classifiers.trees.j48It;

import java.util.ArrayList;

import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.classifiers.trees.j48.ModelSelection;
import weka.core.Instances;
import weka.core.Utils;

/**
 * Class for handling a consolidated tree structure that can
 * be pruned using C4.5 procedures.
 * *************************************************************************************
 * 
 * @author Jes&uacute;s M. P&eacute;rez (txus.perez@ehu.eus) 
 * @version $Revision: 1.2 $
 */
public class C45PruneableClassifierTreeIt extends
		C45PruneableClassifierTree {

	/** for serialization */
	private static final long serialVersionUID = 2660972525647728377L;
	
	/** Indicates the order in which the node was treated */
	private int m_order;
	
	/** Build the tree level by level up to a maximum of depth levels. 
	 * Set m_levelByLevel_growth to 0 to use the optimal number of levels.
	 */
	private int m_levelByLevel_growth = 3;
	
	/** All possible priorities */
	enum priorities {LEVELBYLEVEL,PREORDER,SIZE}
	
	/** Indicates the criteria that should be used to build the tree */
	private priorities priority_criteria = priorities.SIZE;
		

	/**
	 * Constructor for pruneable consolidated tree structure. Calls
	 * the superclass constructor.
	 *
	 * @param toSelectLocModel selection method for local splitting model
	 * @param pruneTree true if the tree is to be pruned
	 * @param cf the confidence factor for pruning
	 * @param raiseTree true if subtree raising has to be performed
	 * @param cleanup true if cleanup has to be done
	 * @param collapseTree true if collapse has to be done
	 * @throws Exception if something goes wrong
	 */
	public C45PruneableClassifierTreeIt(
			ModelSelection toSelectLocModel, boolean pruneTree, float cf,
			boolean raiseTree, boolean cleanup, boolean collapseTree) throws Exception {
		super(toSelectLocModel, pruneTree, cf, raiseTree, cleanup, collapseTree);
	}


	/**
	 * Builds the consolidated tree structure.
	 * (based on the method buildTree() of the class 'ClassifierTree')
	 *
	 * @param data the data for pruning the consolidated tree
	 * @param samplesVector the vector of samples used for consolidation
	 * @param keepData is training data to be kept?
	 * @throws Exception if something goes wrong
	 */
	public void buildTree(Instances data, boolean keepData) throws Exception {
		
        int currentLevel = 0;
	    ArrayList<Object[]> list = new ArrayList<>();
	    list.add(new Object[] {data, this});
        Instances[] localInstances;

	    int index = 0;
	    while (list.size() > 0) {
	        Object[] current = list.get(0);
	        list.set(0, null); // Null to free up memory
	        list.remove(0);

	        Instances currentData = (Instances) current[0];
	        C45PruneableClassifierTreeIt currentTree = (C45PruneableClassifierTreeIt) current[1];
	        currentTree.m_order = index;

	        if (keepData) {
	            currentTree.m_train = currentData;
	        }
	        currentTree.m_test = null;
	        currentTree.m_isLeaf = false;
	        currentTree.m_isEmpty = false;
	        currentTree.m_sons = null;
	        currentTree.m_localModel = currentTree.m_toSelectModel.selectModel(currentData);
	        
	        if (currentTree.m_localModel.numSubsets() > 1 && (m_levelByLevel_growth == 0 || currentTree.m_order < m_levelByLevel_growth - 1)) {
	    	    ArrayList<Object[]> listSons = new ArrayList<>();
	    	    localInstances = currentTree.m_localModel.split(currentData);
	            currentData = null;
	            currentTree.m_sons = new ClassifierTree[currentTree.m_localModel.numSubsets()];
	            for (int i = 0; i < currentTree.m_sons.length; i++) {
	                ClassifierTree newTree = new C45PruneableClassifierTreeIt(currentTree.m_toSelectModel, m_pruneTheTree, m_CF,
	                            m_subtreeRaising, m_cleanup, m_collapseTheTree);
	                listSons.add(new Object[] {localInstances[i], newTree});
	                currentTree.m_sons[i] = newTree;
	            
	                localInstances[i] = null;
	            }

	            if (priority_criteria == priorities.LEVELBYLEVEL) { // level by level
	            	list.addAll(listSons);
	            }

	         
	            else if (priority_criteria == priorities.PREORDER){ //preorder
	            	listSons.addAll(list);
		            list = listSons;
	            }
	            else if(priority_criteria == priorities.SIZE) // Added by size, largest to smallest
	            {
	            	addOrderedBySize(list, listSons);
	            }
	            
	            listSons = null;
	        } else {
	            currentTree.m_isLeaf = true;
	            if (Utils.eq(currentData.sumOfWeights(), 0)) {
	                currentTree.m_isEmpty = true;
	            }
	            currentData = null;
	        }

	        index++; // Indizea inkrementatzen da
	    }
	}
	
	  /**
	   * Help method for printing tree structure.
	   * 
	   * @param depth the current depth
	   * @param text for outputting the structure
	   * @throws Exception if something goes wrong
	   */
	  public void dumpTree(int depth, StringBuffer text) throws Exception {

		  int i, j;

		  for (i = 0; i < m_sons.length; i++) {
			  text.append("\n");
			  ;
			  for (j = 0; j < depth; j++) {
				  text.append("|   ");
			  }
			  text.append("[" + m_order + "] ");
			  text.append(m_isEmpty);
			  text.append(m_localModel.leftSide(m_train));
			  text.append(m_localModel.rightSide(i, m_train));
			  if (m_sons[i].isLeaf()) {
				  text.append(": ");
				  text.append("[" + ((C45PruneableClassifierTreeIt)m_sons[i]).m_order + "] ");
				  text.append(m_localModel.dumpLabel(i, m_train));
			  } else {
				  m_sons[i].dumpTree(depth + 1, text);
			  }
		  }
	  }
	  

	  
	  public void addOrderedBySize(ArrayList<Object[]> list, ArrayList<Object[]> listSons) {
		    if (list.size() == 0) 
		    {
		        list.add(0, listSons.get(0));
		    }
		    if (list.size() > 0 && list.get(0)[0].equals(listSons.get(0)[0])) {
		        listSons.remove(0); // Remove the first element from the list listSons
		    }
		    
		    int sonInstances = 0;
		    int parentInstances = 0;
		    
	        for (int i = 0; i < listSons.size(); i++) 
	        {
	            Instances sonData = (Instances) listSons.get(i)[0];
	            sonInstances = sonData.numInstances();
	            	          
	            for (int j = 0; j < list.size(); j++) 
	            {
	                Instances data = (Instances) list.get(j)[0];
		            		            	
		            parentInstances = data.numInstances();
		            		            
	                if (parentInstances < sonInstances) {
	                    list.add(j, listSons.get(i));
	                    break;
	                }
	                
	                if (j == list.size() - 1) {
	                    list.add(listSons.get(i));
	                    break;
	                }
	            }
	        }
		   
	  }
}
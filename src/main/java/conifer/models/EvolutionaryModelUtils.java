package conifer.models;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import bayonet.marginal.FactorGraph;
import bayonet.marginal.algo.EdgeSorter;

import com.google.common.collect.Lists;

import conifer.TreeNode;
import conifer.UnrootedTree;
import conifer.io.TreeObservations;



public class EvolutionaryModelUtils
{
  public static List<FactorGraph<TreeNode>> buildFactorGraphs( 
      EvolutionaryModel evolutionaryModel,
      UnrootedTree tree,
      TreeNode root,
      TreeObservations observations)
  {
    List<FactorGraph<TreeNode>> result = Lists.newArrayList();
    List<Pair<TreeNode,TreeNode>> orientedEdges = EdgeSorter.newEdgeSorter(tree.getTopology(), root).backwardMessages();
    
    for (int i = 0; i < evolutionaryModel.nFactorGraphs(); i++)
    {
      FactorGraph<TreeNode> factorGraph = evolutionaryModel.newFactorGraph(tree.getTopology());
      LikelihoodFactoryContext context = new LikelihoodFactoryContext(factorGraph, tree, observations, i);
      
      // add observations
      for (TreeNode observedNode : observations.getObservedTreeNodes())
        evolutionaryModel.buildObservation(observedNode, context);
        
      // initial distribution
      evolutionaryModel.buildInitialDistribution(root, context);
      
      // add transitions
      for (Pair<TreeNode,TreeNode> edge : orientedEdges)
        evolutionaryModel.buildTransition(edge.getLeft(), edge.getRight(), context);
      
      result.add(factorGraph);
    }
    
    return result;
  }
}

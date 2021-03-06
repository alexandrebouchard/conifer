package conifer.ctmc;

import org.ejml.data.Eigenpair;
import org.ejml.ops.EigenOps;
import org.ejml.simple.SimpleMatrix;

import bayonet.distributions.Multinomial;
import bayonet.math.EJMLUtils;
import bayonet.math.NumericalUtils;
import bayonet.math.EJMLUtils.SimpleEigenDecomposition;


/**
 * A continuous time Markov chain. The main functionalities consists
 * in computing a marginal transition probability and a stationary distibution
 * (see below).
 * 
 * This implementation is based on caching the eigendecomposition
 * of the provided rate matrix.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class EigenCTMC implements CTMC
{
  private final SimpleEigenDecomposition eigenDecomp;
  private final double [] stationaryDistribution;
  private final double [][] rates;

  /**
   * Note: if the RateMatrix is changed in place,
   * these changes will not be mirrored by this class.
   * 
   * It should be recreated each time a likelihood 
   * calculation is performed.
   * @param rateMatrix
   */
  public EigenCTMC(double [][] rates)
  {
    RateMatrixUtils.checkValidRateMatrix(rates);
    this.rates = rates;
    this.eigenDecomp = EJMLUtils.simpleEigenDecomposition(new SimpleMatrix(rates));
    this.stationaryDistribution = computeStationary();
  }
  
  /**
   * Compute and cache the stationary (called by the constructor)
   * @return
   */
  private double[] computeStationary()
  {
    SimpleMatrix marginal = new SimpleMatrix(marginalTransitionProbability(1.0));
    SimpleMatrix exponentiatedTranspose = marginal.transpose();
    Eigenpair eigenPair = EigenOps.computeEigenVector(exponentiatedTranspose.getMatrix(), 1.0);
    double [] result = eigenPair.vector.data;
    double sum = NumericalUtils.getNormalization(result);
    if (sum < 0.0)
      for (int i = 0; i < result.length; i++)
        result[i] *= -1;
    Multinomial.normalize(result);
    return result;
  }
  
  public double [][] getRateMatrix() 
  {
    return rates;
  }
  
  public double [][] marginalTransitionProbability(double branchLength)
  {
    return marginalTransitionProbability(eigenDecomp, branchLength);
  }
  
  public static double [][] marginalTransitionProbability(SimpleEigenDecomposition eigenDecomp, double branchLength)
  {
    double [][] result =  EJMLUtils.copyMatrixToArray(EJMLUtils.matrixExponential(eigenDecomp, branchLength));
    RateMatrixUtils.removeSmallNegativeEntries(result); // TODO: do the same with the JBLAS stuff in RateMatrixUtils (which needs to be cleaned up)
    NumericalUtils.checkIsTransitionMatrix(result);
    for (int row = 0; row < result.length; row++)
      Multinomial.normalize(result[row]);
    return result;
  }

  public double [] stationaryDistribution()
  {
    return stationaryDistribution;
  }
  
}
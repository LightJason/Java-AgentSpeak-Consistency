/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason                                                #
 * # Copyright (c) 2015-19, LightJason (info@lightjason.org)                            #
 * # This program is free software: you can redistribute it and/or modify               #
 * # it under the terms of the GNU Lesser General Public License as                     #
 * # published by the Free Software Foundation, either version 3 of the                 #
 * # License, or (at your option) any later version.                                    #
 * #                                                                                    #
 * # This program is distributed in the hope that it will be useful,                    #
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of                     #
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                      #
 * # GNU Lesser General Public License for more details.                                #
 * #                                                                                    #
 * # You should have received a copy of the GNU Lesser General Public License           #
 * # along with this program. If not, see http://www.gnu.org/licenses/                  #
 * ######################################################################################
 * @endcond
 */

package org.lightjason.agentspeak.consistency;

import cern.colt.function.tdouble.DoubleFunction;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleEigenvalueDecomposition;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import cern.jet.math.tdouble.DoubleMult;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.consistency.filter.IFilter;
import org.lightjason.agentspeak.consistency.metric.IMetric;
import org.lightjason.agentspeak.error.CEnumConstantNotPresentException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * layer with consistency data based a markov-chain
 */
public final class CConsistency implements IConsistency
{
    /**
     * default value on non-existing objects
     */
    private static final Map.Entry<Double, Double> DEFAULTNONEXISTING = new AbstractMap.SimpleImmutableEntry<>( 1.0, 0.0 );
    /**
     * algebra
     */
    private static final DenseDoubleAlgebra ALGEBRA = DenseDoubleAlgebra.DEFAULT;
    /**
     * function for inverting probability
     */
    private static final DoubleFunction PROBABILITYINVERT = p_value -> 1 - p_value;
    /**
     * algorithm to calculate stationary probability
     **/
    private final EAlgorithm m_algorithm;
    /**
     * map with object and consistency & inconsistency value
     **/
    private final Map<IAgent<?>, Map.Entry<Double, Double>> m_data = new ConcurrentHashMap<>();
    /**
     * descriptive statistic
     */
    private final DescriptiveStatistics m_statistic = new SynchronizedDescriptiveStatistics();
    /**
     * metric filter
     */
    private final IFilter m_filter;
    /**
     * metric object to create the consistency of two objects
     **/
    private final IMetric m_metric;
    /**
     * epsilon consistency to create an aperiodic markow-chain
     **/
    private final double m_epsilon;
    /**
     * number of iterations of the stochastic algorithm
     **/
    private final int m_iteration;


    /**
     * ctor
     *
     * @param p_algorithm algorithm
     * @param p_filter metric filter
     * @param p_metric object metric
     * @param p_iteration iterations
     * @param p_epsilon epsilon consistency
     */
    private CConsistency( @Nonnull final EAlgorithm p_algorithm, @Nonnull final IFilter p_filter, @Nonnull final IMetric p_metric,
                          final int p_iteration, final double p_epsilon
    )
    {
        m_filter = p_filter;
        m_metric = p_metric;
        m_algorithm = p_algorithm;
        m_iteration = p_iteration;
        m_epsilon = p_epsilon;
    }

    @Nonnull
    @Override
    public DescriptiveStatistics statistic()
    {
        return m_statistic;
    }

    @Nonnull
    @Override
    public IConsistency add( @Nonnull final IAgent<?> p_object )
    {
        m_data.putIfAbsent( p_object, DEFAULTNONEXISTING );
        return this;
    }

    @Override
    public IConsistency call() throws Exception
    {
        if ( m_data.size() < 2 )
            return this;

        // get key list of map for addressing elements in the correct order
        final ArrayList<IAgent<?>> l_keys = new ArrayList<>( m_data.keySet() );

        // calculate markov chain transition matrix
        final DoubleMatrix2D l_matrix = new DenseDoubleMatrix2D( m_data.size(), m_data.size() );
        IntStream.range( 0, l_keys.size() )
                 .parallel()
                 .boxed()
                 .forEach( i ->
                 {
                     final IAgent<?> l_item = l_keys.get( i );
                     IntStream.range( i + 1, l_keys.size() )
                              .boxed()
                              .forEach( j ->
                              {
                                  final double l_value = this.getMetricValue( l_item, l_keys.get( j ) );
                                  l_matrix.setQuick( i, j, l_value );
                                  l_matrix.setQuick( j, i, l_value );
                              } );

                     // row-wise normalization for getting probabilities
                     final double l_norm = ALGEBRA.norm1( l_matrix.viewRow( i ) );
                     if ( l_norm != 0 )
                         l_matrix.viewRow( i ).assign( DoubleMult.div( l_norm ) );

                     // set epsilon slope for preventing periodic markov chains
                     l_matrix.setQuick( i, i, m_epsilon );
                 } );

        // check for a zero-matrix
        final DoubleMatrix1D l_eigenvector = l_matrix.zSum() <= m_data.size() * m_epsilon
                                             ? new DenseDoubleMatrix1D( m_data.size() )
                                             : m_algorithm.getStationaryDistribution( m_iteration, l_matrix );

        // calculate the inverted probability and normalize with 1-norm
        final DoubleMatrix1D l_invertedeigenvector = new DenseDoubleMatrix1D( l_eigenvector.toArray() );
        l_invertedeigenvector.assign( PROBABILITYINVERT );
        l_invertedeigenvector.assign( DoubleFunctions.div( ALGEBRA.norm1( l_eigenvector ) ) );

        // set consistency for each entry and update statistic
        m_statistic.clear();
        IntStream.range( 0, l_keys.size() )
                 .boxed()
                 .forEach( i ->
                 {
                     m_statistic.addValue( l_eigenvector.get( i ) );
                     m_data.put( l_keys.get( i ), new AbstractMap.SimpleImmutableEntry<>( l_invertedeigenvector.get( i ), l_eigenvector.get( i ) ) );
                 } );

        return this;
    }

    @Nonnull
    @Override
    public IConsistency remove( @Nonnull final IAgent<?> p_object )
    {
        m_data.remove( p_object );
        return this;
    }

    @Nonnull
    @Override
    public IConsistency clear()
    {
        m_statistic.clear();
        m_data.clear();
        return this;
    }

    @Nonnull
    @Override
    public IMetric metric()
    {
        return m_metric;
    }

    @Nonnull
    @Override
    public IFilter filter()
    {
        return m_filter;
    }

    @Nonnull
    @Override
    public Stream<Map.Entry<IAgent<?>, Double>> consistency()
    {
        return m_data.entrySet().stream().map( i -> new AbstractMap.SimpleImmutableEntry<>( i.getKey(), i.getValue().getKey() ) );
    }

    @Nonnegative
    @Override
    public double consistency( @Nonnull final IAgent<?> p_object )
    {
        return m_data.getOrDefault( p_object, DEFAULTNONEXISTING ).getKey();
    }

    @Nonnegative
    @Override
    public double inconsistency( @Nonnull final IAgent<?> p_object )
    {
        return m_data.getOrDefault( p_object, DEFAULTNONEXISTING ).getValue();
    }

    @Nonnull
    @Override
    public Stream<Map.Entry<IAgent<?>, Double>> inconsistency()
    {
        return m_data.entrySet().stream().map( i -> new AbstractMap.SimpleImmutableEntry<>( i.getKey(), i.getValue().getValue() ) );
    }

    @Override
    public String toString()
    {
        return MessageFormat.format( "{0}{1}", super.toString(), m_data );
    }

    /**
     * returns metric consistency
     *
     * @param p_first first element
     * @param p_second secend element
     * @return metric consistency
     */
    private double getMetricValue( final IAgent<?> p_first, final IAgent<?> p_second )
    {
        if ( p_first.equals( p_second ) )
            return 0;

        return m_metric.apply(
            m_filter.apply( p_first ),
            m_filter.apply( p_second )
        );
    }

    /**
     * factory numerical algorithm
     *
     * @param p_filter metric filter
     * @param p_metric object metric
     * @return consistency
     */
    public static IConsistency numeric( final IFilter p_filter, final IMetric p_metric )
    {
        return new CConsistency( EAlgorithm.NUMERICAL, p_filter, p_metric, 0, 0.001 );
    }

    /**
     * factory heuristic algorithm
     *
     * @param p_filter metric filter
     * @param p_metric object metric
     * @return consistency
     */
    public static IConsistency heuristic( final IFilter p_filter, final IMetric p_metric )
    {
        return new CConsistency( EAlgorithm.FIXPOINT, p_filter, p_metric, 8, 0.001 );
    }

    /**
     * factory heuristic algorithm
     *
     * @param p_filter metric filter
     * @param p_metric object metric
     * @param p_iteration number of iterations
     * @return consistency
     */
    public static IConsistency heuristic( final IFilter p_filter, final IMetric p_metric, final int p_iteration )
    {
        return new CConsistency( EAlgorithm.FIXPOINT, p_filter, p_metric, p_iteration, 0.001 );
    }

    /**
     * factory numerical algorithm
     *
     * @param p_filter metric filter
     * @param p_metric object metric
     * @param p_iteration number of iterations
     * @param p_epsilon epsilon
     * @return consistency
     */
    public static IConsistency heuristic( final IFilter p_filter, final IMetric p_metric, final int p_iteration, final double p_epsilon )
    {
        return new CConsistency( EAlgorithm.FIXPOINT, p_filter, p_metric, p_iteration, p_epsilon );
    }



    /**
     * numeric algorithm structure
     */
    private enum EAlgorithm
    {
        /**
         * use numeric algorithm (QR decomposition)
         **/
        NUMERICAL,
        /**
         * use stochastic algorithm (fixpoint iteration)
         **/
        FIXPOINT;



        /**
         * calculates the stationary distribution
         *
         * @param p_iteration number of iteration
         * @param p_matrix transition matrix
         * @return stationary distribution
         */
        public DoubleMatrix1D getStationaryDistribution( final int p_iteration, final DoubleMatrix2D p_matrix )
        {
            final DoubleMatrix1D l_eigenvector;
            switch ( this )
            {
                case FIXPOINT:
                    l_eigenvector = getLargestEigenvector( p_matrix, p_iteration );
                    break;

                case NUMERICAL:
                    l_eigenvector = getLargestEigenvector( p_matrix );
                    break;

                default:
                    throw new CEnumConstantNotPresentException( this.getClass(), this.toString() );
            }

            // normalize eigenvector and create positiv oriantation
            l_eigenvector.assign( DoubleMult.div( ALGEBRA.norm1( l_eigenvector ) ) );
            l_eigenvector.assign( DoubleFunctions.abs );

            return l_eigenvector;
        }

        /**
         * get the largest eigen vector based on the perron-frobenius theorem
         *
         * @param p_matrix matrix
         * @param p_iteration number of iterations
         * @return largest eigenvector (not normalized)
         *
         * @see http://en.wikipedia.org/wiki/Perron%E2%80%93Frobenius_theorem
         */
        private static DoubleMatrix1D getLargestEigenvector( final DoubleMatrix2D p_matrix, final int p_iteration )
        {
            final DoubleMatrix1D l_probability = DoubleFactory1D.dense.random( p_matrix.rows() );
            IntStream.range( 0, p_iteration )
                     .forEach( i ->
                     {
                         l_probability.assign( ALGEBRA.mult( p_matrix, l_probability ) );
                         l_probability.assign( DoubleMult.div( ALGEBRA.norm2( l_probability ) ) );
                     } );
            return l_probability;
        }

        /**
         * get the largest eigen vector with QR decomposition
         *
         * @param p_matrix matrix
         * @return largest eigenvector (not normalized)
         */
        private static DoubleMatrix1D getLargestEigenvector( final DoubleMatrix2D p_matrix )
        {
            final DenseDoubleEigenvalueDecomposition l_eigen = new DenseDoubleEigenvalueDecomposition( p_matrix );

            // gets the position of the largest eigenvalue in parallel and returns the eigenvector
            final double[] l_eigenvalues = l_eigen.getRealEigenvalues().toArray();
            return l_eigen.getV().viewColumn(
                IntStream.range( 0, l_eigenvalues.length - 1 ).parallel()
                         .reduce( ( i, j ) -> l_eigenvalues[i] < l_eigenvalues[j] ? j : i ).orElse( 0 )
            );
        }

    }

}

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
import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix1D;
import cern.jet.math.tdouble.DoubleFunctions;
import cern.jet.math.tdouble.DoubleMult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.consistency.filter.CBeliefFilter;
import org.lightjason.agentspeak.consistency.filter.IFilter;
import org.lightjason.agentspeak.consistency.metric.CNCD;
import org.lightjason.agentspeak.consistency.metric.IMetric;
import org.lightjason.agentspeak.language.CCommon;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * consistency algorithm based on a markov-chain
 */
public final class CMarkowChainConsistency implements IConsistency
{
    /**
     * default metric
     */
    public static final IMetric DEFAULTMETRIC = new CNCD();
    /**
     * default filter
     */
    public static final IFilter DEFAULTFILTER = new CBeliefFilter();
    /**
     * default iteration
     */
    public static final int DEFAULTITERATION = 5;
    /**
     * default epsilon
     */
    public static final double DEFAULTEPSILON = CCommon.FLOATINGPRECISION.doubleValue();
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
    public CMarkowChainConsistency( @Nonnull final EAlgorithm p_algorithm, @Nonnull final IFilter p_filter,
                                    @Nonnull final IMetric p_metric, final int p_iteration, final double p_epsilon
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

    @Override
    public IConsistency call() throws Exception
    {
        if ( m_data.size() < 2 )
            return this;

        // get key list of map for addressing elements in the correct order
        final IAgent<?>[] l_keys = m_data.keySet().toArray( new IAgent<?>[m_data.size()] );

        // create symmatric matrix
        final DoubleMatrix2D l_matrix = new DenseDoubleMatrix2D( l_keys.length, l_keys.length );

        // calculate markov chain transition matrix
        generateindex( l_keys.length ).forEach( i ->
        {
            final double l_value = this.getMetricValue( l_keys[i.getLeft()], l_keys[i.getRight()] );
            l_matrix.setQuick( i.getLeft(), i.getRight(), l_value );
            l_matrix.setQuick( i.getRight(), i.getLeft(), l_value );
        } );

        // row-wise normalization for getting probabilities
        IntStream.range( 0, l_keys.length )
                 .boxed()
                 .forEach( i ->
                 {
                     final double l_norm = ALGEBRA.norm1( l_matrix.viewRow( i ) );
                     if ( CCommon.floatingequal( l_norm, 0, m_epsilon ) )
                         l_matrix.viewRow( i ).assign( DoubleMult.div( l_norm ) );

                     // set epsilon slope for preventing periodic markov chains
                     l_matrix.setQuick( i, i, m_epsilon );
                 } );

        // check for a zero-matrix
        final DoubleMatrix1D l_eigenvector = l_matrix.zSum() <= l_keys.length * m_epsilon
                                             ? new SparseDoubleMatrix1D( l_keys.length )
                                             : m_algorithm.apply( m_iteration, l_matrix );

        // calculate the inverted probability and normalize with 1-norm
        final DoubleMatrix1D l_invertedeigenvector = new DenseDoubleMatrix1D( l_eigenvector.toArray() );
        l_invertedeigenvector.assign( PROBABILITYINVERT );
        l_invertedeigenvector.assign( DoubleFunctions.div( ALGEBRA.norm1( l_eigenvector ) ) );

        // set consistency for each entry and update statistic
        m_statistic.clear();
        IntStream.range( 0, l_keys.length )
                 .boxed()
                 .peek( i -> m_statistic.addValue( l_eigenvector.get( i ) ) )
                 .forEach( i -> m_data.put( l_keys[i], new AbstractMap.SimpleImmutableEntry<>( l_invertedeigenvector.get( i ), l_eigenvector.get( i ) ) ) );

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
    public IConsistency add( @Nonnull final IAgent<?>... p_agents )
    {
        return this.add( Arrays.stream( p_agents ) );
    }

    @Nonnull
    @Override
    public IConsistency add( @Nonnull final Stream<IAgent<?>> p_agents )
    {
        p_agents.forEach( i -> m_data.putIfAbsent( i, DEFAULTNONEXISTING ) );
        return this;
    }

    @Nonnull
    @Override
    public IConsistency remove( @Nonnull final IAgent<?>... p_agents )
    {
        return this.remove( Arrays.stream( p_agents ) );
    }

    @Nonnull
    @Override
    public IConsistency remove( @Nonnull final Stream<IAgent<?>> p_agents )
    {
        p_agents.forEach( m_data::remove );
        return this;
    }

    @Nonnull
    @Override
    public Stream<Map.Entry<IAgent<?>, Double>> consistency()
    {
        return m_data.entrySet().stream().map( i -> new AbstractMap.SimpleImmutableEntry<>( i.getKey(), i.getValue().getKey() ) );
    }

    @Nonnegative
    @Override
    public double consistency( @Nonnull final IAgent<?> p_agent )
    {
        return m_data.getOrDefault( p_agent, DEFAULTNONEXISTING ).getKey();
    }

    @Nonnegative
    @Override
    public double inconsistency( @Nonnull final IAgent<?> p_agent )
    {
        return m_data.getOrDefault( p_agent, DEFAULTNONEXISTING ).getValue();
    }

    @Nonnull
    @Override
    public Stream<Map.Entry<IAgent<?>, Double>> inconsistency()
    {
        return m_data.entrySet().stream().map( i -> new AbstractMap.SimpleImmutableEntry<>( i.getKey(), i.getValue().getValue() ) );
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
        ).doubleValue();
    }

    /**
     * generates the index pairs or a matrix
     *
     * @param p_size size
     * @return stream with index pairs
     */
    private static Stream<Pair<Integer, Integer>> generateindex( final int p_size )
    {
        return IntStream.range( 0, p_size ).boxed().flatMap( i -> IntStream.range( i, p_size ).filter( j -> i != j ).boxed().map( j -> new ImmutablePair<>( i, j ) ) );
    }



    /**
     * numeric algorithm structure
     */
    public enum EAlgorithm implements BiFunction<Integer, DoubleMatrix2D, DoubleMatrix1D>
    {
        /**
         * use numeric algorithm (QR decomposition)
         **/
        NUMERICAL
        {
            @Override
            public DoubleMatrix1D apply( final Integer p_iteration, final DoubleMatrix2D p_matrix )
            {
                return normalize( getLargestEigenvector( p_matrix ) );
            }
        },
        /**
         * use stochastic algorithm (fixpoint iteration)
         **/
        FIXPOINT
        {
            @Override
            public DoubleMatrix1D apply( final Integer p_iteration, final DoubleMatrix2D p_matrix )
            {
                return normalize( getLargestEigenvector( p_matrix, p_iteration ) );
            }
        };


        /**
         * normalize eigenvector and create positiv oriantation
         *
         * @param p_eigenvector eigen vector
         * @return normalized eigen vector
         */
        private static DoubleMatrix1D normalize( @Nonnull  final DoubleMatrix1D p_eigenvector )
        {
            p_eigenvector.assign( DoubleMult.div( ALGEBRA.norm1( p_eigenvector ) ) );
            p_eigenvector.assign( DoubleFunctions.abs );
            return p_eigenvector;
        }

        /**
         * get the largest eigen vector based on the perron-frobenius theorem
         *
         * @param p_matrix matrix
         * @param p_iteration number of iterations
         * @return largest eigenvector (not normalized)
         *
         * @see <a href="http://en.wikipedia.org/wiki/Perron%E2%80%93Frobenius_theorem"></a>
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

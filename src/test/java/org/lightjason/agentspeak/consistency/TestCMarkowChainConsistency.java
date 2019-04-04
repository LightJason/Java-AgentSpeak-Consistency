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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.consistency.metric.CDiscreteDistance;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.testing.IBaseTest;

import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * test consistency
 */
public final class TestCMarkowChainConsistency extends IBaseTest
{
    /**
     * agent generator
     */
    private CAgentGenerator m_agentgenerator;

    /**
     * test initialize
     *
     * @throws Exception on any parsing error
     */
    @Before
    public void initialize() throws Exception
    {
        m_agentgenerator = new CAgentGenerator();
    }


    /**
     * test numeric consistency
     *
     * @throws Exception is thrown on agent generating error
     */
    @Test
    public void numeric() throws Exception
    {
        Assume.assumeNotNull( m_agentgenerator );


        final IAgent<?> l_agent1 = m_agentgenerator.generatesingle();
        l_agent1.beliefbase()
                .add(
                    CLiteral.of( "foo" ),
                    CLiteral.of( "xxx" ),
                    CLiteral.of( "bar" ) );

        final IAgent<?> l_agent2 = m_agentgenerator.generatesingle();
        l_agent2.beliefbase()
                .add(
                    CLiteral.of( "foo" ),
                    CLiteral.of( "xxx" ),
                    CLiteral.of( "bar" ),
                    CLiteral.of( "hello" ) );

        final IAgent<?> l_agent3 = m_agentgenerator.generatesingle();
        IntStream.range( 0, 1500 )
                 .boxed()
                 .forEach( i -> l_agent3.beliefbase().add( CLiteral.of( RandomStringUtils.random( 12, "abcdefghijklmnopqrstuvwxyz".toCharArray() ) ) ) );


        final IConsistency l_consistency = new CMarkowChainConsistency(
            CMarkowChainConsistency.EAlgorithm.NUMERICAL,
            CMarkowChainConsistency.DEFAULTFILTER,
            new CDiscreteDistance(),
            CMarkowChainConsistency.DEFAULTITERATION,
            CMarkowChainConsistency.DEFAULTEPSILON
        ).add( l_agent1, l_agent2, l_agent3 ).call();


        Assert.assertArrayEquals(
            Stream.of( 0.5, 0.7240092377397959, 0.775990762260204 ).mapToDouble( i -> i ).toArray(),
            l_consistency.consistency().mapToDouble( Map.Entry::getValue ).sorted().toArray(),
            0.01
        );

        Assert.assertArrayEquals(
            Stream.of( 0.5, 0.7240092377397959, 0.775990762260204 ).mapToDouble( i -> i ).toArray(),
            Stream.of(
                l_consistency.consistency( l_agent1 ),
                l_consistency.consistency( l_agent2 ),
                l_consistency.consistency( l_agent3 )
            ).mapToDouble( i -> i ).sorted().toArray(),
            0.01
        );


        Assert.assertArrayEquals(
            Stream.of( 0.22400923773979595, 0.27599076226020414, 0.5 ).mapToDouble( i -> i ).toArray(),
            l_consistency.inconsistency().mapToDouble( Map.Entry::getValue ).sorted().toArray(),
            0.01
        );

        Assert.assertArrayEquals(
            Stream.of( 0.22400923773979595, 0.27599076226020414, 0.5 ).mapToDouble( i -> i ).toArray(),
            Stream.of(
                l_consistency.inconsistency( l_agent1 ),
                l_consistency.inconsistency( l_agent2 ),
                l_consistency.inconsistency( l_agent3 )
            ).mapToDouble( i -> i ).sorted().toArray(),
            0.01
        );
    }


    /**
     * test fixpoint consistency
     *
     * @throws Exception is thrown on agent generating error
     */
    @Test
    public void fixpoint() throws Exception
    {
        Assume.assumeNotNull( m_agentgenerator );


        final IAgent<?> l_agent1 = m_agentgenerator.generatesingle();
        l_agent1.beliefbase()
                .add(
                    CLiteral.of( "foo" ),
                    CLiteral.of( "xxx" ),
                    CLiteral.of( "bar" ) );

        final IAgent<?> l_agent2 = m_agentgenerator.generatesingle();
        l_agent2.beliefbase()
                .add(
                    CLiteral.of( "foo" ),
                    CLiteral.of( "xxx" ),
                    CLiteral.of( "bar" ),
                    CLiteral.of( "hello" ) );

        final IAgent<?> l_agent3 = m_agentgenerator.generatesingle();
        IntStream.range( 0, 1500 )
                 .boxed()
                 .forEach( i -> l_agent3.beliefbase().add( CLiteral.of( RandomStringUtils.random( 12, "abcdefghijklmnopqrstuvwxyz".toCharArray() ) ) ) );


        final IConsistency l_consistency = new CMarkowChainConsistency(
            CMarkowChainConsistency.EAlgorithm.FIXPOINT,
            CMarkowChainConsistency.DEFAULTFILTER,
            new CDiscreteDistance(),
            CMarkowChainConsistency.DEFAULTITERATION,
            CMarkowChainConsistency.DEFAULTEPSILON
        ).add( l_agent1, l_agent2, l_agent3 ).call();


        Assert.assertArrayEquals(
            Stream.of( 0.6566054547259637, 0.6702854287912592, 0.673109116482777 ).mapToDouble( i -> i ).toArray(),
            l_consistency.consistency().mapToDouble( Map.Entry::getValue ).sorted().toArray(),
            0.01
        );

        Assert.assertArrayEquals(
            Stream.of( 0.6566054547259637, 0.6702854287912592, 0.673109116482777 ).mapToDouble( i -> i ).toArray(),
            Stream.of(
                l_consistency.consistency( l_agent1 ),
                l_consistency.consistency( l_agent2 ),
                l_consistency.consistency( l_agent3 )
            ).mapToDouble( i -> i ).sorted().toArray(),
            0.01
        );


        Assert.assertArrayEquals(
            Stream.of( 0.33225070359075737, 0.3324919942364805, 0.33525730217276223 ).mapToDouble( i -> i ).toArray(),
            l_consistency.inconsistency().mapToDouble( Map.Entry::getValue ).sorted().toArray(),
            0.01
        );

        Assert.assertArrayEquals(
            Stream.of( 0.33225070359075737, 0.3324919942364805, 0.33525730217276223 ).mapToDouble( i -> i ).toArray(),
            Stream.of(
                l_consistency.inconsistency( l_agent1 ),
                l_consistency.inconsistency( l_agent2 ),
                l_consistency.inconsistency( l_agent3 )
            ).mapToDouble( i -> i ).sorted().toArray(),
            0.01
        );
    }

}

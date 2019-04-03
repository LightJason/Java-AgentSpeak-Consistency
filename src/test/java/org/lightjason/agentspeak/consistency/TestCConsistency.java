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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.testing.IBaseTest;

import java.util.stream.Collectors;


/**
 * test consistency
 */
public final class TestCConsistency extends IBaseTest
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
        l_agent1.beliefbase()
                .add(
                    CLiteral.of( "aaa" ),
                    CLiteral.of( "bbb" ),
                    CLiteral.of( "ccc" ),
                    CLiteral.of( "ddd" ),
                    CLiteral.of( "eee" ) );

        final IConsistency l_consistency = new CConsistency(
            CConsistency.EAlgorithm.NUMERICAL,
            CConsistency.DEFAULTFILTER,
            CConsistency.DEFAULTMETRIC,
            CConsistency.DEFAULTITERATION,
            CConsistency.DEFAULTEPSILON
        ).add( l_agent1, l_agent2, l_agent3 ).call();

        System.out.println( l_consistency.consistency().map( i -> i.getValue() ).collect( Collectors.toList() ) );
        Assert.assertEquals( 0, l_consistency.consistency( l_agent1 ) - l_consistency.consistency( l_agent2 ), 0.01  );
        Assert.assertEquals( 0.08, l_consistency.consistency( l_agent1 ) - l_consistency.consistency( l_agent3 ), 0.01  );
        Assert.assertEquals( 0.08, l_consistency.consistency( l_agent2 ) - l_consistency.consistency( l_agent3 ), 0.01  );
    }

}

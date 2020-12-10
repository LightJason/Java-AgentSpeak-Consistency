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
import org.junit.jupiter.api.Assumptions;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.beliefbase.CBeliefbase;
import org.lightjason.agentspeak.beliefbase.storage.CMultiStorage;
import org.lightjason.agentspeak.beliefbase.view.IView;
import org.lightjason.agentspeak.beliefbase.view.IViewGenerator;
import org.lightjason.agentspeak.common.CPath;
import org.lightjason.agentspeak.consistency.filter.CAllFilter;
import org.lightjason.agentspeak.consistency.filter.CBeliefFilter;
import org.lightjason.agentspeak.consistency.filter.CPlanFilter;
import org.lightjason.agentspeak.consistency.filter.IFilter;
import org.lightjason.agentspeak.consistency.metric.CDiscreteDistance;
import org.lightjason.agentspeak.consistency.metric.CLevenshteinDistance;
import org.lightjason.agentspeak.consistency.metric.CNCD;
import org.lightjason.agentspeak.consistency.metric.CSymmetricDifference;
import org.lightjason.agentspeak.consistency.metric.CWeightedDifference;
import org.lightjason.agentspeak.consistency.metric.IMetric;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ILiteral;
import org.lightjason.agentspeak.language.execution.IExecution;
import org.lightjason.agentspeak.language.execution.instantiable.plan.CPlan;
import org.lightjason.agentspeak.language.execution.instantiable.plan.annotation.IAnnotation;
import org.lightjason.agentspeak.language.execution.instantiable.plan.statistic.CPlanStatistic;
import org.lightjason.agentspeak.language.execution.instantiable.plan.trigger.CTrigger;
import org.lightjason.agentspeak.language.execution.instantiable.plan.trigger.ITrigger;
import org.lightjason.agentspeak.testing.IBaseTest;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * metric & filter tests
 */
public final class TestCMetricFilter extends IBaseTest
{
    /**
     * assume message
     */
    private static final String ASSUMEMESSAGE = "testing literals are empty";
    /**
     * literal functor
     */
    private static final String FIRSTSUB1 = "first/sub1";
    /**
     * agent generator
     */
    private CAgentGenerator m_agentgenerator;
    /**
     * literal view generator
     */
    private IViewGenerator m_viewgenerator;
    /**
     * set with testing literals
     */
    private Set<ILiteral> m_literals;

    /**
     * test initialize
     *
     * @throws Exception on any parsing error
     */
    @Before
    public void initialize() throws Exception
    {
        m_viewgenerator = new CGenerator();
        m_agentgenerator = new CAgentGenerator();

        m_literals = Stream.of(
            CLiteral.of( "toplevel" ),
            CLiteral.of( FIRSTSUB1 ),
            CLiteral.of( "first/sub2" ),
            CLiteral.of( "second/sub3" ),
            CLiteral.of( "second/sub4" ),
            CLiteral.of( "second/sub/sub5" )
        ).collect( Collectors.toSet() );
    }



    /**
     * test symmetric weight metric equality
     */
    @Test
    public void symmetricweightequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
            "symmetric difference equality",
            new CAllFilter(),
            new CSymmetricDifference(),
            m_literals,
            m_literals,
            0, 0
        );
    }


    /**
     * test symmetric weight metric inequality
     */
    @Test
    public void symmetricweightinequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
            "symmetric difference inequality",
            new CAllFilter(),
            new CSymmetricDifference(),
            m_literals,
            Stream.concat( m_literals.stream(), Stream.of( CLiteral.of( "diff" ) ) ).collect( Collectors.toSet() ),
            1, 0
        );
    }


    /**
     * test symmetric metric equality
     */
    @Test
    public void weightequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
            "weight difference equality",
            new CAllFilter(),
            new CWeightedDifference(),
            m_literals,
            m_literals,
            24, 0
        );
    }


    /**
     * test symmetric metric equality
     */
    @Test
    public void weightinequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
            "weight difference inequality",
            new CAllFilter(),
            new CWeightedDifference(),
            m_literals,
            Stream.concat( m_literals.stream(), Stream.of( CLiteral.of( "diff" ) ) ).collect( Collectors.toSet() ),
            28 + 1.0 / 6, 0
        );
    }


    /**
     * test discrete metric equality
     */
    @Test
    public void discreteequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
                "discrete difference equality",
                new CAllFilter(),
                new CDiscreteDistance(),
                m_literals,
                m_literals,
                0, 0
        );
    }


    /**
     * test discrete metric equality
     */
    @Test
    public void discreteinequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
                "weight difference inequality",
                new CAllFilter(),
                new CDiscreteDistance(),
                m_literals,
                Stream.concat( m_literals.stream(), Stream.of( CLiteral.of( "discrete" ) ) ).collect( Collectors.toSet() ),
                1, 0
        );
    }


    /**
     * test ncd metric equality
     */
    @Test
    public void ncdequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
            "ncd difference equality",
            new CAllFilter(),
            new CNCD(),
            m_literals,
            m_literals,
            0, 0
        );
    }

    /**
     * test symmetric metric inequality
     */
    @Test
    public void ncdinequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
            "ncd difference inequality",
            new CAllFilter(),
            new CNCD(),
            m_literals,
            Stream.of(
                CLiteral.of( "ncd" ),
                CLiteral.of( "xxx" ),
                CLiteral.of( "opq" )
            ).collect( Collectors.toSet() ),
            0.52, 0.01
        );
    }


    /**
     * test levenshtein metric equality
     */
    @Test
    public void levenshteinequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
            "levenshtein difference equality",
            new CAllFilter(),
            new CLevenshteinDistance(),
            m_literals,
            m_literals,
            0, 0
        );
    }


    /**
     * test levenshtein metric equality
     */
    @Test
    public void levenshteininequality()
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        this.check(
            "levenshtein difference inequality",
            new CAllFilter(),
            new CLevenshteinDistance(),
            m_literals,
            Stream.concat( m_literals.stream(), Stream.of( CLiteral.of( "levenshtein" ) ) ).collect( Collectors.toSet() ),
            13, 0
        );
    }

    /**
     * test filter
     *
     * @throws Exception on agent execution
     */
    @Test
    public void filter() throws Exception
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        final IAgent<?> l_agent = m_agentgenerator.generatesingle();
        m_literals.forEach( i -> l_agent.beliefbase().generate( m_viewgenerator, i.functorpath() ).add( i ) );

        final ITrigger l_trigger = CTrigger.of( ITrigger.EType.ADDGOAL, CLiteral.of( "myplan" ) );
        l_agent.plans().put( l_trigger, CPlanStatistic.of( new CPlan( new IAnnotation<?>[]{}, l_trigger, new IExecution[]{} ) ) );
        l_agent.trigger( l_trigger );
        l_agent.call();


        Assert.assertArrayEquals(
            Stream.of( "myplan[]", "toplevel[]", FIRSTSUB1 + "[]", "first/sub2[]", "second/sub3[]", "second/sub4[]", "second/second/sub/sub5[]" ).toArray(),
            new CAllFilter().apply( l_agent ).map( Object::toString ).toArray()
        );

        Assert.assertArrayEquals(
            Stream.of( "myplan[]" ).toArray(),
            new CPlanFilter().apply( l_agent ).map( Object::toString ).toArray()
        );

        Assert.assertArrayEquals(
            Stream.of( "toplevel[]", FIRSTSUB1 + "[]", "first/sub2[]", "second/sub3[]", "second/sub4[]", "second/second/sub/sub5[]" ).toArray(),
            new CBeliefFilter().apply( l_agent ).map( Object::toString ).toArray()
        );
    }

    /**
     * test filter with path
     *
     * @throws Exception on agent execution
     */
    @Test
    public void filterwithpath() throws Exception
    {
        Assumptions.assumeTrue( Objects.nonNull( m_agentgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_viewgenerator ) );
        Assumptions.assumeTrue( Objects.nonNull( m_literals ) );
        Assumptions.assumeFalse( m_literals.isEmpty(), ASSUMEMESSAGE );

        final IAgent<?> l_agent = m_agentgenerator.generatesingle();
        Stream.concat(
            m_literals.stream(),
            Stream.of(
                CLiteral.of( FIRSTSUB1, CRawTerm.of( 1 ) ),
                CLiteral.of( FIRSTSUB1, CRawTerm.of( 2 ) ),
                CLiteral.of( FIRSTSUB1, CRawTerm.of( 3 ) )
            )
        ).forEach( i -> l_agent.beliefbase().generate( m_viewgenerator, i.functorpath() ).add( i ) );


        Assert.assertArrayEquals(
            Stream.of( "sub1[]", "sub1[1]", "sub1[2]", "sub1[3]" ).toArray(),
            new CAllFilter( CPath.of( FIRSTSUB1 ) ).apply( l_agent ).map( Object::toString ).toArray()
        );

        Assert.assertArrayEquals(
            Stream.of( "sub1[]", "sub1[1]", "sub1[2]", "sub1[3]" ).toArray(),
            new CAllFilter( CPath.of( FIRSTSUB1 ) ).apply( l_agent ).map( Object::toString ).toArray()
        );


        Assert.assertArrayEquals(
            Stream.of( "sub1[]", "sub1[1]", "sub1[2]", "sub1[3]" ).toArray(),
            new CAllFilter( Stream.of( CPath.of( FIRSTSUB1 ) ) ).apply( l_agent ).map( Object::toString ).toArray()
        );

        Assert.assertArrayEquals(
            Stream.of( "sub1[]", "sub1[1]", "sub1[2]", "sub1[3]" ).toArray(),
            new CAllFilter( Stream.of( CPath.of( FIRSTSUB1 ) ) ).apply( l_agent ).map( Object::toString ).toArray()
        );
    }


    /**
     * runs the check
     *
     * @param p_message error / successful message
     * @param p_filter agent filter
     * @param p_metric metric value
     * @param p_belief1 belief set 1
     * @param p_belief2 belief set 2
     * @param p_excepted expected value
     * @param p_delta delta
     */
    private void check( final String p_message, final IFilter p_filter, final IMetric p_metric, final Collection<ILiteral> p_belief1,
                        final Collection<ILiteral> p_belief2, final double p_excepted, final double p_delta )
    {
        final double l_value = p_metric.apply(
            p_filter.apply( this.agent( p_belief1 ) ),
            p_filter.apply( this.agent( p_belief2 ) )
        ).doubleValue();
        Assert.assertEquals( p_message, p_excepted, l_value, p_delta );
    }


    /**
     * generates an agent
     *
     * @param p_literals literal collection
     * @return agent
     */
    private IAgent<?> agent( final Collection<ILiteral> p_literals )
    {
        Assume.assumeNotNull( m_viewgenerator );

        final IAgent<?> l_agent = m_agentgenerator.generatesingle();
        p_literals.forEach( i -> l_agent.beliefbase().generate( m_viewgenerator, i.functorpath() ).add( i ) );
        return l_agent;
    }


    /**
     * test belief generator
     */
    private static final class CGenerator implements IViewGenerator
    {
        @Override
        public IView apply( final String p_name, final IView p_parent )
        {
            return new CBeliefbase( new CMultiStorage<>() ).create( p_name, p_parent );
        }
    }

}

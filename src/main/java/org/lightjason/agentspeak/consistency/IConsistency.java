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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.lightjason.agentspeak.agent.IAgent;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;


/**
 * consistency interface
 */
public interface IConsistency extends Callable<IConsistency>
{

    /**
     * returns the consistency of an agent
     *
     * @param p_agent agent
     * @return consistency or default consistency
     */
    @Nonnegative
    double consistency( @Nonnull final IAgent<?> p_agent );

    /**
     * stream over all data
     *
     * @return entry with agent and consistency
     */
    @Nonnull
    Stream<Map.Entry<IAgent<?>, Double>> consistency();

    /**
     * returns the inconsistency of an agent
     *
     * @param p_agent agent
     * @return consistency or default consistency
     */
    @Nonnegative
    double inconsistency( @Nonnull final IAgent<?> p_agent );

    /**
     * stream over all data
     *
     * @return entry with agent and inconsistency
     */
    @Nonnull
    Stream<Map.Entry<IAgent<?>, Double>> inconsistency();

    /**
     * returns statistic data of the consistency values
     *
     * @return statistic
     */
    @Nonnull
    DescriptiveStatistics statistic();

    /**
     * adds agents
     *
     * @param p_agents agents
     * @return self reference
     */
    @Nonnull
    IConsistency add( @Nonnull final IAgent<?>... p_agents );

    /**
     * adds agents
     *
     * @param p_agents agent stream
     * @return self reference
     */
    @Nonnull
    IConsistency add( @Nonnull final Stream<IAgent<?>> p_agents );

    /**
     * removes agents
     *
     * @param p_agents removing agents
     * @return self reference
     */
    @Nonnull
    IConsistency remove( @Nonnull final IAgent<?>... p_agents );

    /**
     * removes agents
     *
     * @param p_agents agent stream
     * @return self reference
     */
    @Nonnull
    IConsistency remove( @Nonnull final Stream<IAgent<?>> p_agents );

    /**
     * clear
     *
     * @return self reference
     */
    @Nonnull
    IConsistency clear();

}

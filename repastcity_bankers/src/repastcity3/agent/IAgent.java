/*ęCopyright 2019 Lukasz Kowalski
This file is part of Location Analysis ABM, which was built based on RepastCity software.
More information about the model can be found here:
https://github.com/LukaszKowalski2013/location_analysis_ABM
Polish readers can read my PhD thesis about it, that is available on my Research Gate profile: https://goo.gl/TViW89

ęCopyright 2012 Nick Malleson
RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.*/

package repastcity3.agent;

import java.util.List;

import repastcity3.environment.Building;

/**
 * All agents must implement this interface so that it the simulation knows how
 * to step them.
 * 
 * @author Nick Malleson
 * 
 */
public interface IAgent {
	





	/**
	 * Controls the agent. This method will be called by the scheduler once per
	 * iteration.
	 */
	 void step() throws Exception;

	/**
	 * Used by Agents as a means of stating whether or not they can be
	 * run in parallel (i.e. there is no inter-agent communication which will
	 * make parallelisation non-trivial). If all the agents in a simulation
	 * return true, and the computer running the simulation has
	 * more than one core, it is possible to step agents simultaneously.
	 * 
	 * @author Nick Malleson
	 */
	boolean isThreadable();
	
	/**
	 * Set where the agent lives.
	 */
	void setHome(Building home);
	
	/**
	 * Get the agent's home.
	 */
	
	Building getHome();
	
	/**
	 * (Optional). Add objects to the agents memory. Used to keep a record of all the
	 * buildings that they have passed.
	 * @param <T>
	 * @param objects The objects to add to the memory.
	 * @param clazz The type of object.
	 */
	<T> void addToMemory(List<T> objects, Class<T> clazz);
	
	/**
	 * (Optional). Get the transport options available to this agent. E.g.
	 * an agent with a car who also could use public transport would return
	 * <code>{"bus", "car"}</code>. If null then it is assumed that the agent
	 * walks (the slowest of all transport methods). 
	 */
	
	void setTransport(String string); // it is for my new method of creating agents
	
	List<String> getTransportAvailable();
	
	boolean isAvailableTransport(String transport);
	
	
	void setPracticedToday(int b); //Lukasz Kowalski comment:

	void setCrowdAwareness(double[][] clubCrowdMatrix, double hourOfResetEntries);

	void setHeCanGo();

	double[][] getAgentsSRanking();

	int getAgentsFavouriteClub();

	double getZONE_ID();

	void setZONE_ID(double getZONE_ID);

	double getMyTime1();

	double getMyTime2();

	double getMinRank();
	void setMinRank(double AgentsMinRank);

	void setCoolingVariable(double experiencePT);
	
	String getFavClubStrefa_moj();
//	practicedToday
	
}

/*©Copyright 2019 Lukasz Kowalski
This file is part of Location Analysis ABM, which was built based on RepastCity software.
More information about the model can be found here:
https://github.com/LukaszKowalski2013/location_analysis_ABM
Polish readers can read my PhD thesis about it, that is available on my Research Gate profile: https://goo.gl/TViW89

©Copyright 2012 Nick Malleson
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

package repastcity3.environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import com.vividsolutions.jts.geom.Coordinate;

import repastcity3.agent.IAgent;
import repastcity3.agent.DefaultAgent; //Lukasz Kowalski comment: I added this
import repastcity3.exceptions.NoIdentifierException;
import repastcity3.main.ContextManager;
import repastcity3.main.GlobalVars;

public class Building implements FixedGeography, Identified {

	private static Logger LOGGER = Logger.getLogger(Building.class.getName());
	
	/** The type of this building. 1 means a normal house, 2 means a bank.*/
	private int type = 1;
	
	/** Number of times this house has been burgled */
	private int numBurglaries = 0;
	
	/** A list of agents who live here */
	private List<IAgent> agents;

	/**
	 * A unique identifier for buildings, usually set from the 'identifier' column in a shapefile
	 */
	private String identifier;

	/**
	 * The coordinates of the Building. This is also stored by the projection that contains this Building but it is
	 * useful to have it here too. As they will never change (buildings don't move) we don't need to worry about keeping
	 * them in sync with the projection.
	 */
	private Coordinate coords;

	// my variables:
	private int swimming; //Lukasz Kowalski comment:  added from shapefile

	private double clubID; //Lukasz Kowalski comment:  added from shapefile

	private int fitness; //Lukasz Kowalski comment:  added from shapefile
	
	private int clientEntries;
	
	private double crowdByHour;
	
	private double capacity; //it changes over time based on timeArray
	
	private double[][] clubTimeArrayFIT=new double[12][2]; //Lukasz Kowalski comment: new capacity taken from csv double[][]
								//1st row is clubID, 1st col are times of entry, 2nd is number of rooms or swimming lanes
	private double[][] clubTimeArraySWIM=new double[12][2];
		
	private double accessCar; //Lukasz Kowalski comment:  added from shapefile// it should imitate access time for car drivers, which means congestion and parking time
	
	private double accessBus; //Lukasz Kowalski comment:  added from shapefile // it should imitate access time for passengers of public transport
	
	private int sumOfClients;
	
	private int ZONE_ID; //Lukasz Kowalski comment: attribute added from shapefile ////WATCH@ TYPE - here we have int, but everywhere else there is double
	
	public int agentsCarS;  //it is needed for model build, it says how many agents of my type will apear in simulation
	public int agentsBusS;
	public int agentsCarF;
	public int agentsBusF;

	private String strefa_moj;
	
	//end of variables
	
	public Building() {
		this.agents = new ArrayList<IAgent>();
		
		}

	@Override
	public Coordinate getCoords() {
		return this.coords;
	}

	@Override
	public void setCoords(Coordinate c) {
		this.coords = c;
	}

	public String getIdentifier() throws NoIdentifierException {
		if (this.identifier == null) {
			throw new NoIdentifierException("This building has no identifier. This can happen "   
					+ "when roads are not initialised correctly (e.g. there is no attribute "
					+ "called 'identifier' present in the shapefile used to create this Road)");
		} else {
			return identifier;
		}
	}

	public void setIdentifier(String id) {
		this.identifier = id;
	}

	public void addAgent(IAgent a) {
		this.agents.add(a);
	}

	public List<IAgent> getAgents() {
		return this.agents;
	}
	
	//my "sport" methods for counting agents who get inside and go out
	public void removeAgent(IAgent a){ //Lukasz Kowalski comment: agents who finished sport practice 4 today
		this.agents.remove(a); 
	}
	
	

	@Override
	public String toString() {
		return "building: " + this.identifier;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Building))
			return false;
		Building b = (Building) obj;
		return this.identifier.equals(b.identifier);
	}

	/**
	 * Returns the hash code of this <code>Building</code>'s identifier string. 
	 */
	@Override
	public int hashCode() {
		if (this.identifier==null) {
			LOGGER.severe("hashCode called but this object's identifier has not been set. It is likely that you're " +
					"reading a shapefile that doesn't have a string column called 'identifier'");
		}

		return this.identifier.hashCode();
	}
	
	/**
	 * Find the type of this building, represented as an integer. 1 means a normal house, 2 means a bank.
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Set the type of this building, represented as an integer. 1 means a normal house, 2 means a work, 3 means club
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Find the number of times that this house has been burgled.
	 */
	public int getNumBurglaries() {
		return this.numBurglaries;
	}
	
	/**
	 * Tell the house it has been burgled (increase it's burglary counter).
	 */
	public synchronized void burgled() {
		this.numBurglaries++;
	}
	
	//Lukasz Kowalski comment: my getters&setters & others
	public void clientEntry() {
		this.clientEntries++;
		}
	
	public int getNumOfClientEntries(){ //TEMP this should give, for my report, number of clients of whole last day
		return this.sumOfClients;	}
	
	public double howCrowdedWasIt(){
		if (capacity!=0){
			return (double)crowdByHour/capacity; //it should be double anyway, but I get doubles above 1 like int values
		}
		else {
			return 0; 
		} // this -1 means that capacity is 0 =>error in data loading
	}
	public double getCrowdByHour(){
		return crowdByHour;
	}
	
	public int getRandomAgentIndex(int arraySize) {
		int rnd = new Random().nextInt(arraySize);
		return rnd;
		}
	
	public void resetClubEntries(double resetTime){
//		int numberOfAgentsWhoPracticed=clubCapacity;
		crowdByHour=agents.size();
		sumOfClients+=clientEntries;
		clientEntries=0;
		resetTime=Math.round(resetTime); //second step, 1st was at ContextManager
		
		//check&set capacity for this hour
		
		for (int f=1; f<clubTimeArrayFIT.length;f++){ //it matters if its clubTimeArrayFIT or clubTimeArraySWIM, because 
			if(resetTime  == clubTimeArraySWIM[f][0]){ //I can do this shortcut, because if GlobalVars.mySport=="fitness" this array is full of zeros 
				this.capacity=clubTimeArraySWIM[f][1]* GlobalVars.placesPerLaneAtPool;	
//				System.out.println( resetTime +" = " + clubTimeArraySWIM[f][0] + " capacity : " + capacity );
				break;
			}
			else if (resetTime == clubTimeArrayFIT[f][0]){
				this.capacity = clubTimeArrayFIT[f][1] * GlobalVars.placesPerRoomAtFitness;
//				System.out.println( resetTime +" = " + clubTimeArrayFIT[f][0] + " capacity : " + capacity );
				break;
			}
		}
		
//		System.out.println(getCrowdByHour());
		int agentToRemove; //index of this agent
//		int noProblem=0;
//		int luckers=0;
//		int loosers=0;
		if (capacity>=agents.size()){ // there are more places than agents = every1 practice sport
			for (int i=0;i<agents.size(); i++){
				agents.get(i).setPracticedToday(1); //practiced sport
				//				agents.remove(i);

//				noProblem++;
				//System.out.print("tiroram! ");
			}
			agents.clear();
		}
		else {
			for (int i=0;i<capacity; i++){//here we want to remove all agents who practiced sport today
				agentToRemove=getRandomAgentIndex(agents.size());
				agents.get(agentToRemove).setPracticedToday(1); //practiced sport
				agents.remove(agentToRemove);
//				luckers++;
			}

			for (IAgent a: agents){//here we want to remove 'loosers' :)
				a.setPracticedToday(2);
				//					System.out.print(" BUUU! "+agents.size());
				//					System.out.println(2);// temp to check if it works
//				loosers++;
			}
			agents.clear();
		}
		
//		System.out.println("end,"+ this.clubID +","+ ContextManager.realTime+ "," + crowdByHour + ","+capacity + ","+(double)crowdByHour/capacity +","+  agents.size());
//		System.out.println(" noProblems:"+ noProblem+  " luckers:" +luckers + " loosers:" + loosers);
	}



	public void setSwimming(int swimming){
		this.swimming = swimming;	}
	public int getSwimming(){
		return this.swimming;}
	
	public void setFitness(int fitness){
		this.fitness = fitness;	}
	public int getFitness() {
		return this.fitness;}

	public void setClubID(double clubID){
		this.clubID = clubID;}
	public double getClubID(){
		return clubID;	}

	public void setAccessCar(double accessCar){
		this.accessCar = accessCar;}
	public double GetAccessCar(){
		return accessCar;	}
	
	public void setAccessBus(double accessBus){
		this.accessBus = accessBus;}
	public double GetAccessBus(){
		return accessBus;	} // 
	
	
	public void setAgentsCarS(int agentsCarS){
		this.agentsCarS = agentsCarS;}
	public int getAgentsCarS(){
		return agentsCarS;	}
	
	public void setAgentsBusS(int agentsBusS){
		this.agentsBusS = agentsBusS;}
	public int getAgentsBusS(){
		return agentsBusS;	}
	
	public void setAgentsCarF(int agentsCarF){
		this.agentsCarF = agentsCarF;}
	public int getAgentsCarF(){
		return agentsCarF;	}
	
	public void setAgentsBusF(int agentsBusF){
		this.agentsBusF = agentsBusF;}
	public int getAgentsBusF(){
		return agentsBusF;	}
	
	public void setZONE_ID(int ZONE_ID){ //WATCH@ TYPE - here we have int, but everywhere else there is double
		this.ZONE_ID = ZONE_ID;}
	public int GetZONE_ID(){
		return ZONE_ID;	}
	
	
	public void setStrefa_moj(String strefa_moj){
		this.strefa_moj = strefa_moj;}
	public String getStrefa_moj(){
		return strefa_moj;	}
	
	public double getCapacityAtTime(double time){
		double capacityAtTime = -1;
		for (int f=1; f<clubTimeArrayFIT.length;f++){ //it matters if its clubTimeArrayFIT or clubTimeArraySWIM, because they are same size
			if(time  == clubTimeArraySWIM[f][0]){ //I can do this shortcut, because if GlobalVars.mySport=="fitness" this array is full of zeros 
				capacityAtTime=clubTimeArraySWIM[f][1];
				break;
			}
			else if (time  == clubTimeArrayFIT[f][0]){
				capacityAtTime=clubTimeArrayFIT[f][1];
				break;
			}
		}
		if (capacityAtTime== -1){ //check temp
			System.out.print (" ups -1 at clubID: "+ this.clubID + "and time: " + time);
		}
		return capacityAtTime;
	}


	public void setTimeArray(double[][] timeArray){ //timeArray should be taken from MyMatrixes.java according to club type set at GlobalVars
		//find col with this building clubID
		int col=0;
		int c=1; //clubID starts here
		boolean found=false;
		while (!found && c< timeArray[0].length){ 
			if(timeArray[0][c]==this.clubID){
				found=true;
				col=c;//sRanking[c][6]=inputArray[r][1];
				break;
			}
			else{
				c++;
			}
		}
		if (GlobalVars.mySport=="swimming"){
			//set time Array for this club
			for( int i =0; i<timeArray.length-1; i++){ // 1st row is clubID, length-1 because last row is howManyDays it is opened
				clubTimeArraySWIM[i][1]=timeArray[i][col];
				clubTimeArraySWIM[i][0]=timeArray[i][0]; //rows names
			}
			capacity=clubTimeArraySWIM[1][1]* GlobalVars.placesPerLaneAtPool; //initial capacity at 6 a.m.
		}
		else if (GlobalVars.mySport=="fitness"){
			//set time Array for this club
			for( int i =0; i<timeArray.length-1; i++){ // 1st row is clubID, length-1 because last row is howManyDays it is opened
				clubTimeArrayFIT[i][1]=timeArray[i][col];
				clubTimeArrayFIT[i][0]=timeArray[i][0]; //rows names
			}
			capacity=clubTimeArrayFIT[1][1]* GlobalVars.placesPerRoomAtFitness; //initial capacity at 6 a.m.
		}
	}
	
	
	//  this method can check if agent who want to get in did not come too early in that case club should tell the agent to
	// go back home and update myHour time, but not resigning from the favourite club 
	public boolean mayIenter(){
		//	return this.agentsInside<1;
		return true; //TEMP there is no limit by now
	}
	
	public boolean isAgentAtThisClub(IAgent a){
		return this.agents.contains(a);
	}

	
	
}

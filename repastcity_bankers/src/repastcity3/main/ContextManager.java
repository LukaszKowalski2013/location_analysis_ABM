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

package repastcity3.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger; //Lukasz Kowalski comment: if (GlobalVars.loggerOn) {

import au.com.bytecode.opencsv.CSVWriter;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.SimpleAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repastcity3.agent.AgentFactory;
import repastcity3.agent.IAgent;
import repastcity3.agent.ThreadedAgentScheduler;
import repastcity3.environment.Building;
import repastcity3.environment.GISFunctions;
import repastcity3.environment.Junction;
import repastcity3.environment.NetworkEdge;
import repastcity3.environment.NetworkEdgeCreator;
import repastcity3.environment.Road;
import repastcity3.environment.SpatialIndexManager;
import repastcity3.environment.contexts.AgentContext;
import repastcity3.environment.contexts.BuildingContext;
import repastcity3.environment.contexts.JunctionContext;
import repastcity3.environment.contexts.RoadContext;
import repastcity3.exceptions.AgentCreationException;
import repastcity3.exceptions.EnvironmentError;
import repastcity3.exceptions.NoIdentifierException;
import repastcity3.exceptions.ParameterNotFoundException;

public class ContextManager implements ContextBuilder<Object> {

	/*
	 * A logger for this class. Note that there is a static block that is used to configure all logging for the model
	 * (at the bottom of this file).
	 */

	private static Logger LOGGER = Logger.getLogger(ContextManager.class.getName());		


	// Optionally force agent threading off (good for debugging)
	private static final boolean TURN_OFF_THREADING = false;

	private static Properties properties;

	/** A lock used to make <code>RandomHelper</code> thread safe. Classes should ensure they
	 * obtain this object before calling RandomHelper methods.
	 */
	public static Object randomLock = new Object();

	/*
	 * Pointers to contexts and projections (for convenience). Most of these can be made public, but the agent ones
	 * can't be because multi-threaded agents will simultaneously try to call 'move()' and interfere with each other. So
	 * methods like 'moveAgent()' are provided by ContextManager.
	 */

	private static Context<Object> mainContext;

	// building context and projection cab be public (thread safe) because buildings only queried
	public static Context<Building> buildingContext;
	public static Geography<Building> buildingProjection;

	public static Context<Road> roadContext;
	public static Geography<Road> roadProjection;

	public static Context<Junction> junctionContext;
	public static Geography<Junction> junctionGeography;
	public static Network<Junction> roadNetwork;

	private static Context<IAgent> agentContext;
	private static Geography<IAgent> agentGeography;

	//my variables
	public static double hourOfResetEntries; //this variable is to control saying agents how crowded are all clubs
	//	they may be interested in in a specific hour
	//	public double[][] clubCrowdMatrix =new double[GlobalVars.numberOfPools][2];//(clubID, crowd) //FOR FUTURE
	String crowdInClubs="";
	String headers4crowdaAtSwimmingPools="day, hour, ";
	String headers4crowdaAtFitness="day, hour, ";
	boolean headers4crowdInClubsAreDone=false;
	String check; //variable to check crowd
	int numberOfSadAgents=0; //agent's who did not find a club, which would fulfill their expectation - has ranking value higher than  

	String resultsPath="../repastcity_bankers/data/gis_data/myTM/results/";

	int numberOfPools=0;
	int numberOfFitness=0;
	int carDrivers=0;
	int busPassengers=0;
	int recordRuns=1;
	int myEndTime;

	public static double chosenLocationIdClub=42;//if this number is <3000 (e.g. 42) we don't check any new location. Otherwise we check  certain club with idClub indicated by this number.

	double[][] myTimesReport= {{6, 7, 8, 9, 10, 16, 17, 18, 19, 20, 21}, //indexes 0-10
								{0, 0, 0, 0,  0, 0,   0,  0,  0,  0, 0 }};



	@Override
	public Context<Object> build(Context<Object> con) {



		RepastCityLogging.init();


		// Keep a useful static link to the main context
		mainContext = con;

		// This is the name of the 'root'context
		mainContext.setId(GlobalVars.CONTEXT_NAMES.MAIN_CONTEXT);

		// Read in the model properties
		try {
			readProperties();
		} catch (IOException ex) {
			throw new RuntimeException("Could not read model properties,  reason: " + ex.toString(), ex);
		}
		
		try {
			setMyParameters();
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (ParameterNotFoundException e1) {
			e1.printStackTrace();
		}// here we set all parameters in GlobalVars
		
		// Configure the environment
		String gisDataDir = ContextManager.getProperty(GlobalVars.GISDataDirectory);
		if (GlobalVars.loggerOn) {
			LOGGER.log(Level.FINE, "Configuring the environment with data from " + gisDataDir);
		}
		

		
		try {

			// Create the buildings - context and geography projection
			buildingContext = new BuildingContext();
			buildingProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
					GlobalVars.CONTEXT_NAMES.BUILDING_GEOGRAPHY, buildingContext,
					new GeographyParameters<Building>(new SimpleAdder<Building>()));
			String buildingFile = gisDataDir + getProperty(GlobalVars.BuildingShapefile);
			GISFunctions.readShapefile(Building.class, buildingFile, buildingProjection, buildingContext);
			mainContext.addSubContext(buildingContext);
			SpatialIndexManager.createIndex(buildingProjection, Building.class);
			if (GlobalVars.loggerOn) {
				LOGGER.log(Level.FINER, "Read " + buildingContext.getObjects(Building.class).size() + " buildings from "
						+ buildingFile);
			}


			// Create the Roads - context and geography
			roadContext = new RoadContext();
			roadProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
					GlobalVars.CONTEXT_NAMES.ROAD_GEOGRAPHY, roadContext,
					new GeographyParameters<Road>(new SimpleAdder<Road>()));
			String roadFile = gisDataDir + getProperty(GlobalVars.RoadShapefile);
			GISFunctions.readShapefile(Road.class, roadFile, roadProjection, roadContext);
			mainContext.addSubContext(roadContext);
			SpatialIndexManager.createIndex(roadProjection, Road.class);
			if (GlobalVars.loggerOn) {
				LOGGER.log(Level.FINER, "Read " + roadContext.getObjects(Road.class).size() + " roads from " + roadFile);
			}

			// Create road network

			// 1.junctionContext and junctionGeography
			junctionContext = new JunctionContext();
			mainContext.addSubContext(junctionContext);
			junctionGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
					GlobalVars.CONTEXT_NAMES.JUNCTION_GEOGRAPHY, junctionContext,
					new GeographyParameters<Junction>(new SimpleAdder<Junction>()));

			// 2. roadNetwork
			NetworkBuilder<Junction> builder = new NetworkBuilder<Junction>(GlobalVars.CONTEXT_NAMES.ROAD_NETWORK,
					junctionContext, false);
			builder.setEdgeCreator(new NetworkEdgeCreator<Junction>());
			roadNetwork = builder.buildNetwork();
			GISFunctions.buildGISRoadNetwork(roadProjection, junctionContext, junctionGeography, roadNetwork);

			// Add the junctions to a spatial index (couldn't do this until the road network had been created).
			SpatialIndexManager.createIndex(junctionGeography, Junction.class);

			testEnvironment();

		} catch (MalformedURLException e) {
			LOGGER.log(Level.SEVERE, "", e);
			return null;
		} catch (EnvironmentError e) {
			LOGGER.log(Level.SEVERE, "There is an eror with the environment, cannot start simulation", e);
			return null;
		} catch (NoIdentifierException e) {
			LOGGER.log(Level.SEVERE, "One of the input buildings had no identifier (this should be read"
					+ "from the 'identifier' column in an input GIS file)", e);
			return null;
		} catch (FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, "Could not find an input shapefile to read objects from.", e);
			return null;
		}

		// Now create the agents (note that their step methods are scheduled later
		try {

			agentContext = new AgentContext();
			mainContext.addSubContext(agentContext);
			agentGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
					GlobalVars.CONTEXT_NAMES.AGENT_GEOGRAPHY, agentContext,
					new GeographyParameters<IAgent>(new SimpleAdder<IAgent>()));

			String agentDefn = ContextManager.getParameter(MODEL_PARAMETERS.AGENT_DEFINITION.toString());

			if (GlobalVars.loggerOn) {
				LOGGER.log(Level.INFO, "Creating agents with the agent definition: '" + agentDefn + "'");
			}
			AgentFactory agentFactory = new AgentFactory(agentDefn);
			agentFactory.createAgents(agentContext);

		} catch (ParameterNotFoundException e) {
			LOGGER.log(Level.SEVERE, "Could not find the parameter which defines how agents should be "
					+ "created. The parameter is called " + MODEL_PARAMETERS.AGENT_DEFINITION
					+ " and should be added to the parameters.xml file.", e);
			return null;
		} catch (AgentCreationException e) {
			LOGGER.log(Level.SEVERE, "", e);
			return null;
		}

		// Create the schedule
		try {
			createSchedule();
		} catch (ParameterNotFoundException e) {
			LOGGER.log(Level.SEVERE, "Could not find a parameter required to create the schedule.", e);
			return null;
		}
		// create distance matrixes //Lukasz Kowalski comment:
		MyMatrixes.parseAllMatrixes();

		numberOfPools=0;
		numberOfFitness=0;
		carDrivers=0;
		busPassengers=0;
		// here we want to count number of swimming pools and fitness centres and pass this information to GlobalVars //Lukasz Kowalski comment: check
		for (Building b : ContextManager.buildingContext.getObjects(Building.class)) {
			if (b.getType()==3){

				if (GlobalVars.mySport=="swimming" && b.getSwimming()==1){
					b.setTimeArray(MyMatrixes.timeArraySWIM);
					numberOfPools++; 
				}
				if (GlobalVars.mySport=="fitness" && b.getFitness()==1){
					b.setTimeArray(MyMatrixes.timeArrayFIT);
					numberOfFitness++; 
				}
			}

			
		}

		//here we find out if we use 32 or 64 bit JVM
		System.out.println("java version is: " + System.getProperty("sun.arch.data.model")) ; //here we find out if we use 32 or 64 bit JVM
		System.out.println("heap space available: " + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
		System.out.println();
		System.out.println("ContextManager.number of pools: " +numberOfPools + " and numberOfFitness:"+numberOfFitness);

//		setMyParameters();


		for (IAgent a: agentContext.getObjects(IAgent.class)){
			if(a.isAvailableTransport("car")){
				carDrivers++;
				a.setMinRank(GlobalVars.minRankCar);
				a.setCoolingVariable(GlobalVars.experienceCar);
				//				System.out.print(GlobalVars.minRankCar);
			}
			else if(a.isAvailableTransport("bus")){
				busPassengers++;
				a.setMinRank(GlobalVars.minRankBus);
				a.setCoolingVariable(GlobalVars.experiencePT);
				//				System.out.print(GlobalVars.minRankBus);
			}
		}
		System.out.println("ContextManager.number of car drivers: " +carDrivers + " and bus passengers:"+busPassengers);		
		
		System.out.println(" Model is running for service type: " +  GlobalVars.mySport + "\n"+ 
							" Display clients movement or turn on teleportation(faster): " + GlobalVars.displayMovement);

		try {
			setchosenLocationIdClub(); //here we set new location that we want to check
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ParameterNotFoundException e) {
			e.printStackTrace();
		}

		resultsPath="./data/gis_data/myTM/results/"+"/"+(int)chosenLocationIdClub+"/";

		return mainContext;

	} // end of build() function



	/** here we set chosen location, by default it doesn't check any new 1, but at sweep mode it should check different numbers
	 @param newLocationsIdClub
	 * @throws ParameterNotFoundException 
	 * @throws NumberFormatException */
	void setchosenLocationIdClub() throws NumberFormatException, ParameterNotFoundException{
		int newLocationsIdClub = ContextManager.getParameter("newLocationsIdClub"); //Lukasz Kowalski comment:
		chosenLocationIdClub= (double)newLocationsIdClub;
	}

	/** here we set my parameters for GlobalVars
	 @param newLocationsIdClub
	 * @throws ParameterNotFoundException 
	 * @throws NumberFormatException */
	void setMyParameters() throws NumberFormatException, ParameterNotFoundException{
		GlobalVars.experienceCar= Double.parseDouble(ContextManager.getParameter("experienceCar").toString());
		GlobalVars.experiencePT= Double.parseDouble(ContextManager.getParameter("experiencePT").toString());		
		GlobalVars.minRankCar= Double.parseDouble(ContextManager.getParameter("MINRANKCAR").toString());
		GlobalVars.minRankBus= Double.parseDouble(ContextManager.getParameter("MINRANKBUS").toString());
		GlobalVars.trafficImpact= Double.parseDouble(ContextManager.getParameter("TRAFFICIMPACT").toString());
		GlobalVars.busStopDistanceImpact= Double.parseDouble(ContextManager.getParameter("BUSSTOPDISTANCEIMPACT").toString());
		GlobalVars.selectedFacilityType= ContextManager.getParameter("SELECTED_FACILITY_TYPE");
		GlobalVars.displayMovement= ContextManager.getParameter("DISPLAY_MOVEMENT");
		myEndTime=Integer.parseInt(	ContextManager.getParameter("END_TIME").toString() );
		
		GlobalVars.setMySport(GlobalVars.selectedFacilityType.toString());
		GlobalVars.setTeleportationOn(GlobalVars.displayMovement.toString());
		
	}

	/** This function runs through each agent in the model and make some statistics about their favourite club: 1) distance traveled by ranges, 2)clubID x agents' ZONE_ID 
	 * @param numberOfRuns */
	public void outputAgentsData(int numberOfRuns) throws NoIdentifierException, IOException { //IT WAS 
		//old outputBurglaryData
		numberOfRuns=recordRuns;
		recordRuns++; 
		StringBuilder dataToWrite = new StringBuilder(); // Build a string so all data can be written at once.
		StringBuilder dataToWrite1 = new StringBuilder(); // Build a string so all data can be written at once.
		StringBuilder dataToWrite2 = new StringBuilder();
		StringBuilder dataToWrite3 = new StringBuilder();
		StringBuilder dataToWrite4 = new StringBuilder();
		StringBuilder dataToWrite5 = new StringBuilder();
		StringBuilder dataToWrite6 = new StringBuilder();

		String currentParameters="";

		currentParameters =   " chosenLocation, " + chosenLocationIdClub + "\n"
				+ " numberOfRuns, " + numberOfRuns  + "\n"
				+ " experienceCar, " + GlobalVars.experienceCar  + "\n"
				+ " experiencePT, " + GlobalVars.experiencePT             + "\n"
				+ " minRankCar, "+GlobalVars.minRankCar +"\n"+ " minRankBus, "+GlobalVars.minRankBus + "\n"
				+ " trafficImpact, " + GlobalVars.trafficImpact + "\n"
				+ " busStopDistanceImpact, " + GlobalVars.busStopDistanceImpact + "\n";

		dataToWrite.append(currentParameters);

		
		//set variables for SPORT SPLIT
		double[][] AUTO=null; 
		double[][] MPK=null;
		String outputName = null;
		numberOfSadAgents=0; //we have to make it 0 here
		long[] simChi2 = {0,	0,	0,	0,	0,	0}; // it must be zero here otherwise it will not restart by itself after every simulation in batch

		//SPORT SPLIT
		if (GlobalVars.mySport=="swimming"){
			//make a copy of the array same as in MyMatrixes.java - later on we will fill it with 0 or with number of agents
			AUTO= MyMatrixes.SWIM_AUTOinput;
			MPK= MyMatrixes.SWIM_MPKinput; //we take 2 separate files, because I don't want to link them with arrays
			outputName = "SWIM_";
		}
		else if(GlobalVars.mySport=="fitness"){
			AUTO= MyMatrixes.FIT_AUTOinput;
			MPK= MyMatrixes.FIT_MPKinput;
			outputName = "FIT_";
		}


		
		//	        make an array for distance decay function
		int interval=2;//in km 
		int rangesNumber=16; //0-30+
		int lastRangeEnd=100;
		double[][] distanceDecay=new double[3][rangesNumber]; //1st row is for the end of interval, 2nd for car drivers, 3rd for passengers
		for (int i=0;i<rangesNumber;i++){ //here we make an array - 1st row shows the end of interval to count kilometers
			if (i==0){
				distanceDecay[0][i]=interval; //1st value
			}
			else if(i==rangesNumber-1){
				distanceDecay[0][i]=lastRangeEnd; //last value - it will be 100 km to catch all values
			}
			else{
				distanceDecay[0][i]=distanceDecay[0][i-1]+interval;
			}
		}

		// Now iterate over all the agents	        
		for (IAgent a: agentContext.getObjects(IAgent.class)){

			// 1st for loop
			// sum up agents 'my times'
			// values in 1st row of myTimesReport: {6, 7, 8, 9, 10, 16, 17, 18, 19, 20, 21}
			for (int i=0; i< myTimesReport[0].length ; i++){
				if (a.getMyTime1()==myTimesReport[0][i]){
					myTimesReport[1][i]+=1;
				}
			}
			for (int i=0; i< myTimesReport[0].length ; i++){
				if (a.getMyTime2()==myTimesReport[0][i]){
					myTimesReport[1][i]+=1;
				}
			}

			//2nd for loop
			double[][]myRanking = a.getAgentsSRanking();

			//check if agent's best club is above minRank, if it is we can count this agent, unless - he didn't go anywhere
			if (myRanking[a.getAgentsFavouriteClub()][3]>= a.getMinRank() ){

				// 1 - populate zone x club matrixes. Add +1 to the right cell => where agent's best club and his home ZONE_ID match

				//get right ZONE_ID index from matrix
				int r=0; //row index
				while(AUTO[r][0]!=a.getZONE_ID() ){ //&& r<AUTO.length //it doesn't matter if its AUTO OR MPK - length and headers stay the same
					r++;
				}
				//get right clubID's index from matrix
				int c=1; //col index
				while(AUTO[0][c]!=myRanking[a.getAgentsFavouriteClub()][1] && c< AUTO[0].length){ //&& c< AUTO[0].length
					c++;
				}

				//note down drivers
				if(a.isAvailableTransport("car")){
					AUTO[r][c]+=1; // for result matrix
				}
				//note down passengers
				if(a.isAvailableTransport("bus")){
					MPK[r][c]+=1; // for result matrix
				}
				//note down every1
				//	        	SWIM[r][c]+=1;
				
				// 2 - make a csv file where we have table with number of trips at certain distance, according to ranges set before
				double myDistanceInKm = myRanking[a.getAgentsFavouriteClub()][4];
				for (int f=0; f<distanceDecay[0].length;f++) {
					if(f==0){
						if(myDistanceInKm>0 && myDistanceInKm <= distanceDecay[0][f]){
							if(a.isAvailableTransport("car")){
								distanceDecay[1][f]+=1;
								break;
							}
							else if(a.isAvailableTransport("bus")){
								distanceDecay[2][f]+=1;
								break;
							}
						}
					}
					else{
						if(myDistanceInKm > distanceDecay[0][f-1] && myDistanceInKm <= distanceDecay[0][f]){
							if(a.isAvailableTransport("car")){
								distanceDecay[1][f]+=1;
								break;		        				
							}
							else if(a.isAvailableTransport("bus")){
								distanceDecay[2][f]+=1;
								break;	
							}
						}
					}
				}
				

				
				// 3 - populate simChi2[]= {CARcentre, CARkrk-rest,CARoutside, BUScentre,BUSkrk-rest,BUSoutside}
				
				if(a.getFavClubStrefa_moj().contains("centre")){
					if(a.isAvailableTransport("car")){
						simChi2[0]++;
					}
					if(a.isAvailableTransport("bus")){
						simChi2[3]++;
					}					
				}
				else if(a.getFavClubStrefa_moj().contains("KRKrest")){
					if(a.isAvailableTransport("car")){
						simChi2[1]++;
					}
					if(a.isAvailableTransport("bus")){
						simChi2[4]++;
					}
				}
				else if(a.getFavClubStrefa_moj().contains("KRKoutside")){
					if(a.isAvailableTransport("car")){
						simChi2[2]++;
					}
					if(a.isAvailableTransport("bus")){
						simChi2[5]++;
					}	
				}
				else{
					System.out.print("upsssss");
				}
				
				//	dataToWrite.append(myRanking[a.getAgentsFavouriteClub()][4] + " "); //HERE we should 
			} //end of if, which control if agent's have clubs above minRank
			else{
				//count agents, who did not practiced sport and had best club below minRank
				numberOfSadAgents++;
			}

		} //end of for, which iterate through all agents
		
		//now we can count sad agents and add them
		currentParameters = "Happyagents, " + (agentContext.size()-numberOfSadAgents) + "\n"+ " sadOnes, "+ numberOfSadAgents + "\n"
				+ "END_TIME, " + myEndTime;
		dataToWrite.append(currentParameters);
				
		//now we can count correlation between distance matrixes and add it to results.csv - here we have different sizes. Check funkcja oporu odleglosci dla tzones xls file for same size.

		double[] realDistancesSwimCar = {5.3,	14.4,	26.3,	17.6,	11.7,	6.4,	5.9,	4.8,	3.9,	1.1,	1.1};
		double[] realDistancesSwimBus = {6.3,	20.3,	31.6,	17.7,	16.5,	1.3,	5.1,	1.3};
		double[] realDistancesFitCar = {24.7,	32.9,	13.9,	11.4,	5.1,	3.8,	2.5,	1.9,	1.3,	1.9};
		double[] realDistancesFitBus= {18.3,	28.5,	26.3,	14,	7.5,	2.7,	2.2};		

		double[] realDistancesCar = null;
		double[] realDistancesBus=null;
		// chi2fileS // structure      {CARcentre, CARkrk-rest,CARoutside, BUScentre,BUSkrk-rest,BUSoutside}
		long[] chi2swimmingReal = {49,	223,	165,	32,	45,	3}; //SWIMMING REAL
		long[] chi2fitnessReal = {24,	98,	    36,	   118,	68,	1}; //FITNESS REAL
		long[] realChi2=null;

		if (GlobalVars.mySport=="swimming"){
			realDistancesCar=realDistancesSwimCar;
			realDistancesBus=realDistancesSwimBus;
			realChi2 = chi2swimmingReal;
		}
		else if (GlobalVars.mySport=="fitness"){
			realDistancesCar=realDistancesFitCar;
			realDistancesBus=realDistancesFitBus;
			realChi2=chi2fitnessReal;
		}

		//here we want that current distance are as long as realDistancesX (see above)
		double[] currentDistancesCar = new double[realDistancesCar.length];
		double[] currentDistancesBus = new double[realDistancesBus.length];
		for(int i=0;i<realDistancesCar.length;i++){
			currentDistancesCar[i]=distanceDecay[1][i];
		}
		for(int i=0;i<realDistancesBus.length;i++){
			currentDistancesBus[i]=distanceDecay[2][i];
		}
       

		double carCorr = new PearsonsCorrelation().correlation(realDistancesCar, currentDistancesCar);
		double busCorr = new PearsonsCorrelation().correlation(realDistancesBus, currentDistancesBus);	        


		//CHI2 - SAME AS @ http://www.naukowiec.org/wzory/statystyka/test-niezaleznosci-chi-kwadrat_16.html
		double ChiSquareTest = new ChiSquareTest().chiSquareDataSetsComparison(simChi2, realChi2);
//		System.out.println(Arrays.toString(simChi2));

		double carCorrSpearman = new SpearmansCorrelation().correlation(realDistancesCar, currentDistancesCar);
		double busCorrSpearman = new SpearmansCorrelation().correlation(realDistancesBus, currentDistancesBus);

		dataToWrite.append("\n"+" car correlation, " + carCorr  + "\n"
				+" bus correlation, "+	busCorr  + "\n"
				+ "ChiSquareTest, " + ChiSquareTest + "\n"
				+ "carCorrSpearman, " + carCorrSpearman + "\n"
				+ "busCorrSpearman, " + busCorrSpearman + "\n"
				+ "simChi2: " + Arrays.toString(simChi2)
				);

		// Now write this data to a file
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(resultsPath+"results" + numberOfRuns + ".csv")));
		BufferedWriter bw1 = new BufferedWriter(new FileWriter(new File(resultsPath+outputName+"AUTO"+numberOfRuns+".csv")));
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File(resultsPath+outputName+"MPK"+numberOfRuns+".csv")));
		BufferedWriter bw3 = new BufferedWriter(new FileWriter(new File(resultsPath+outputName+"distanceDecay"+numberOfRuns+".csv")));
		BufferedWriter bw4 = new BufferedWriter(new FileWriter(new File(resultsPath+outputName+"crowdInClubs"+numberOfRuns+".csv")));
		BufferedWriter bw5 = new BufferedWriter(new FileWriter(new File(resultsPath+outputName+"myTimesReport"+numberOfRuns+".csv")));
		BufferedWriter bw6 = new BufferedWriter(new FileWriter(new File(resultsPath+outputName+"chi2_"+numberOfRuns+".csv")));

		
		
		//here we change our arrays to text files
		//1 auto&mpk x club results
		for (int i = 0; i < AUTO.length; i++){
			for (int j = 0; j < AUTO[0].length; j++){
				dataToWrite1.append((int)AUTO[i][j]+", ");//RECENTLY we use commas to have nice excel files.
				dataToWrite2.append((int)MPK[i][j]+", ");
			}
			dataToWrite1.append("\n");
			dataToWrite2.append("\n");
		}
		//2 distance decay
		for (int i = 0; i < distanceDecay.length; i++){
			for (int j = 0; j < distanceDecay[0].length; j++){
				dataToWrite3.append((int)distanceDecay[i][j]+", ");
			}
			dataToWrite3.append("\n");
		}

		//3 crowdInClubs

		if(GlobalVars.mySport=="swimming"){
			crowdInClubs=headers4crowdaAtSwimmingPools+ "\n" +crowdInClubs;

		}
		if(GlobalVars.mySport=="fitness"){
			crowdInClubs=headers4crowdaAtFitness+ "\n" +crowdInClubs;

		}
		dataToWrite4.append(crowdInClubs);

		//reset crowdInClubs
		crowdInClubs="";


		// check myTimes -this is for an option when we draw times every simulation for every agent
		//	        dataToWrite5.append("6, 7, 8, 9, 10, 16, 17, 18, 19, 20, 21" + "\n"); //headers
		for (int i=0; i< myTimesReport[0].length ; i++){
			dataToWrite5.append(myTimesReport[1][i]+ ", ");
			myTimesReport[1][i]=0; //we need to reset this value, for next runs
		}
		dataToWrite5.append("\n");
		
		// 5 write down data for chi2
		for (int i=0; i< simChi2.length ; i++){
			dataToWrite6.append(simChi2[i]+ ", ");
			simChi2[i]=0; //we need to reset this value, for next batch runs
		}
		
		// 1 & 2 write it down
		bw.write(dataToWrite.toString());
		bw1.write(dataToWrite1.toString());
		bw2.write(dataToWrite2.toString());
		bw3.write(dataToWrite3.toString());
		bw4.write(dataToWrite4.toString());
		bw5.write(dataToWrite5.toString());
		bw6.write(dataToWrite6.toString());

		bw.close();
		bw1.close();
		bw2.close();
		bw3.close();
		bw4.close();
		bw5.close();
		bw6.close();


		// And log the data as well so we can see it on the console.
		LOGGER.info(dataToWrite.toString());

		numberOfDays = 0; //Lukasz Kowalski comment: number of days will restart after this function - it did not before
		realTime=0;

		// here we try to reset memory to prevent leaks
		System.gc(); //TEMP I don't know if it works
//		System.runFinalization();
//		repast.simphony.batch.setup.RemoveTransferFromRemoteNodes();


	}//end of outputAgentsData

	// reset entries
	/** This method runs through each building in the model and reset number of clients entries to 0 in first step. */ //TEMP In second it gives knowledge about crowd to EVERY agent
	public void resetEntries() throws NoIdentifierException{ //, IOException {
		// Now iterate over all the clubs

		hourOfResetEntries=  (int) realTime; 
		//		int c=0;

		crowdInClubs = crowdInClubs +numberOfDays+", "+hourOfResetEntries+", ";

		for (Building b : ContextManager.buildingContext.getObjects(Building.class)) {
			if (b.getType()==3 ) { //&& b.getSwimming()==1 //it resets every club building, but anyway we don't have so much buildings to worry about that//Lukasz Kowalski comment: potential optimisation point - we can make a list of this clubs before
				if(b.getSwimming()==1 && GlobalVars.mySport=="swimming"){

					//step 1
					b.resetClubEntries(hourOfResetEntries); 
					//step 2 (1/2)
					//				clubCrowdMatrix[c][0]=b.getClubID() ;//clubID //FOR FUTURE
					//				clubCrowdMatrix[c][1]=b.howCrowdedWasIt() ;//crowd //FOR FUTURE
					crowdInClubs+= Math.round(100* b.howCrowdedWasIt()) + ","; //result is in % of capacity
					//				c++;


					if(!headers4crowdInClubsAreDone){ 
						headers4crowdaAtSwimmingPools = headers4crowdaAtSwimmingPools+ b.getClubID()+", ";
					}
				}
				else if (b.getFitness()==1 && GlobalVars.mySport=="fitness"){
					b.resetClubEntries(hourOfResetEntries);
					crowdInClubs+= Math.round(100* b.howCrowdedWasIt()) + ",";

					
					if(!headers4crowdInClubsAreDone){ 
						headers4crowdaAtFitness = headers4crowdaAtFitness+ b.getClubID()+", ";
					}
				}
			} // 1st if

		} // for

		//		headers4crowdInClubsAreDone=true;//I want to fire it once
		crowdInClubs+="\n";
		headers4crowdInClubsAreDone=true;
		//step 2 (2/2) FOR FUTURE
		//here we make a temp clubCrowdMatrix[][] (clubID, crowd) populate it with values from above loop and here we initialise loop which
		//iterate through all agents whose time4sport is now
		//and we populate their sRanking with values from the matrix
		//		for (IAgent a: agentContext.getObjects(IAgent.class)){ //bug shoting - wchodzi ta petla do kazdego agenta //TEMP TURNED OFF
		//			a.setCrowdAwareness(clubCrowdMatrix, (int)hourOfResetEntries-1);
		//		}
		//		
		//		check += Arrays.deepToString(clubCrowdMatrix)+";";
		//		LOGGER.log(Level.INFO, "Agent " + this.toString() + check) ;



	}

	// reset agents
	/** This function runs through each agent in the model at the end of the day and sets its HeCanGo to true */
	public void resetAgents() throws NoIdentifierException{ //, IOException {
		for (IAgent a: agentContext.getObjects(IAgent.class)){ 
			a.setHeCanGo();
		}
	}

	private void createSchedule() throws NumberFormatException, ParameterNotFoundException {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		// THE CODE TO SCHEDULE THE outputBurglaryData() FUNCTION SHOULD GO HERE
		// Schedule the outputBurglaryData() function to be called at the end of the simulation
		int numberOfRuns = Integer.parseInt(ContextManager.getParameter("numberOfRuns").toString()); //Lukasz Kowalski comment:
		ScheduleParameters params = ScheduleParameters.createAtEnd(ScheduleParameters.LAST_PRIORITY);
		//		schedule.schedule(params, this, "outputBurglaryData", numberOfRuns);
		schedule.schedule(params, this, "outputAgentsData", numberOfRuns);



		// Schedule something that outputs ticks every 1000 iterations.
		//		schedule.schedule(ScheduleParameters.createRepeating(1, 1000, ScheduleParameters.LAST_PRIORITY), this,
		//				"printTicks");

		//TEMP this has to wait until agents will COME to club at full hours, e.g. 7, 16...
		// Lukasz Kowalski comment: reset entries for every club every x time(here an hour).
		// one day =1441 ticks , 1 hour = 60 ticks, 30 minutes = 30 ticks, but add 1 everywhere t get precise time
		schedule.schedule(ScheduleParameters.createRepeating(391, 1441), this,
				"resetEntries");//6:30
		schedule.schedule(ScheduleParameters.createRepeating(451, 1441), this,
				"resetEntries");//7:30
		schedule.schedule(ScheduleParameters.createRepeating(511, 1441), this,
				"resetEntries");//8:30
		schedule.schedule(ScheduleParameters.createRepeating(571, 1441), this,
				"resetEntries");//9:30
		schedule.schedule(ScheduleParameters.createRepeating(631, 1441), this,
				"resetEntries");//10:30
		schedule.schedule(ScheduleParameters.createRepeating(991, 1441), this,
				"resetEntries");//	16:30
		schedule.schedule(ScheduleParameters.createRepeating(1051, 1441), this,
				"resetEntries");//	17:30
		schedule.schedule(ScheduleParameters.createRepeating(1111, 1441), this,
				"resetEntries");//	18:30
		schedule.schedule(ScheduleParameters.createRepeating(1171, 1441), this,
				"resetEntries");//	19:30
		schedule.schedule(ScheduleParameters.createRepeating(1231, 1441), this,
				"resetEntries");//	20:30
		schedule.schedule(ScheduleParameters.createRepeating(1291, 1441), this,
				"resetEntries");//	21:30
		//At THE END OF THE DAY we call method resetAgents - it changes practicedToday to 0 again, so they are ready to practice sport again
		schedule.schedule(ScheduleParameters.createRepeating(1441, 1441), this,
				"resetAgents");//	24:00

		int endTime = Integer.parseInt(ContextManager.getParameter("END_TIME").toString());
		schedule.schedule(ScheduleParameters.createOneTime(endTime), this, "end");




		/*
		 * Schedule the agents. This is slightly complicated because if all the agents can be stepped at the same time
		 * (i.e. there are no inter- agent communications that make this difficult) then the scheduling is controlled by
		 * a separate function that steps them in different threads. This massively improves performance on multi-core
		 * machines.
		 */
		boolean isThreadable = false; //Lukasz Kowalski comment: I changed it
		for (IAgent a : agentContext.getObjects(IAgent.class)) {
			if (!a.isThreadable()) {
				isThreadable = false;
				break;
			}
		}

		if (ContextManager.TURN_OFF_THREADING) { // Overide threading?
			isThreadable = false; //Lukasz Kowalski comment: change
		}
		if (isThreadable && (Runtime.getRuntime().availableProcessors() > 1)) {
			/*
			 * Agents can be threaded so the step scheduling not actually done by repast scheduler, a method in
			 * ThreadedAgentScheduler is called which manually steps each agent.
			 */

			LOGGER.log(Level.FINE, "The multi-threaded scheduler will be used.");


			ThreadedAgentScheduler s = new ThreadedAgentScheduler();
			ScheduleParameters agentStepParams = ScheduleParameters.createRepeating(1, 1, 5);
			schedule.schedule(agentStepParams, s, "agentStep");}
		else { // Agents will execute in serial, use the repast scheduler.
			LOGGER.log(Level.FINE, "The single-threaded scheduler will be used.");
			ScheduleParameters agentStepParams = ScheduleParameters.createRepeating(1, 1, 5);
			// Schedule the agents' step methods.
			for (IAgent a : agentContext.getObjects(IAgent.class)) {
				schedule.schedule(agentStepParams, a, "step");
			}
		}

		// This is necessary to make sure that methods scheduled with annotations are called.
		schedule.schedule(this);

	}

	private static long speedTimer = -1; // For recording time per N iterations

	public void printTicks() {

		//		if (GlobalVars.loggerOn) {
		LOGGER.info("Iterations: " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount() + ". Speed: "
				+ ((double) (System.currentTimeMillis() - ContextManager.speedTimer) / 1000.0) + "sec/ticks.");
		//		}
		ContextManager.speedTimer = System.currentTimeMillis();
	}

	/* Function that is scheduled to stop the simulation */
	public void end() {
		LOGGER.info("Simulation is ending after: " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount()
				+ " iterations.");
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		schedule.setFinishing(true);
		schedule.executeEndActions();
	}

	/**
	 * Convenience function to get a Simphony parameter
	 * 
	 * @param <T>
	 *            The type of the parameter
	 * @param paramName
	 *            The name of the parameter
	 * @return The parameter.
	 * @throws ParameterNotFoundException
	 *             If the parameter could not be found.
	 */
	public static <V> V getParameter(String paramName) throws ParameterNotFoundException {
		Parameters p = RunEnvironment.getInstance().getParameters();
		Object val = p.getValue(paramName);

		if (val == null) {
			throw new ParameterNotFoundException(paramName);
		}

		// Try to cast the value and return it
		@SuppressWarnings("unchecked")
		V value = (V) val;
		return value;
	}

	/**
	 * Get the value of a property in the properties file. If the input is empty or null or if there is no property with
	 * a matching name, throw a RuntimeException.
	 * 
	 * @param property
	 *            The property to look for.
	 * @return A value for the property with the given name.
	 */
	public static String getProperty(String property) {
		if (property == null || property.equals("")) {
			throw new RuntimeException("getProperty() error, input parameter (" + property + ") is "
					+ (property == null ? "null" : "empty"));
		} else {
			String val = ContextManager.properties.getProperty(property);
			if (val == null || val.equals("")) { // No value exists in the
				// properties file
				throw new RuntimeException("checkProperty() error, the required property (" + property + ") is "
						+ (property == null ? "null" : "empty"));
			}
			return val;
		}
	}

	/**
	 * Read the properties file and add properties. Will check if any properties have been included on the command line
	 * as well as in the properties file, in these cases the entries in the properties file are ignored in preference
	 * for those specified on the command line.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void readProperties() throws FileNotFoundException, IOException {

		File propFile = new File("./data/repastcity.properties");
		if (!propFile.exists()) {
			throw new FileNotFoundException("Could not find properties file in the default location: "
					+ propFile.getAbsolutePath());
		}

		if (GlobalVars.loggerOn) {
			LOGGER.log(Level.FINE, "Initialising properties from file " + propFile.toString());
		}

		ContextManager.properties = new Properties();

		FileInputStream in = new FileInputStream(propFile.getAbsolutePath());
		ContextManager.properties.load(in);
		in.close();

		// See if any properties are being overridden by command-line arguments
		for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
			String k = (String) e.nextElement();
			String newVal = System.getProperty(k);
			if (newVal != null) {
				// The system property has the same name as the one from the
				// properties file, replace the one in the properties file.
				LOGGER.log(Level.INFO, "Found a system property '" + k + "->" + newVal
						+ "' which matches a NeissModel property '" + k + "->" + properties.getProperty(k)
						+ "', replacing the non-system one.");
				properties.setProperty(k, newVal);
			}
		} // for
		return;
	} // readProperties

	/**
	 * Check that the environment looks ok
	 * 
	 * @throws NoIdentifierException
	 */
	@SuppressWarnings("unchecked")
	private void testEnvironment() throws EnvironmentError, NoIdentifierException {

		LOGGER.log(Level.FINE, "Testing the environment");
		// Get copies of the contexts/projections from main context
		Context<Building> bc = (Context<Building>) mainContext.getSubContext(GlobalVars.CONTEXT_NAMES.BUILDING_CONTEXT);
		Context<Road> rc = (Context<Road>) mainContext.getSubContext(GlobalVars.CONTEXT_NAMES.ROAD_CONTEXT);
		Context<Junction> jc = (Context<Junction>) mainContext.getSubContext(GlobalVars.CONTEXT_NAMES.JUNCTION_CONTEXT);

		// Geography<Building> bg = (Geography<Building>)
		// bc.getProjection(GlobalVars.CONTEXT_NAMES.BUILDING_GEOGRAPHY);
		// Geography<Road> rg = (Geography<Road>)
		// rc.getProjection(GlobalVars.CONTEXT_NAMES.ROAD_GEOGRAPHY);
		// Geography<Junction> jg = (Geography<Junction>)
		// rc.getProjection(GlobalVars.CONTEXT_NAMES.JUNCTION_GEOGRAPHY);
		Network<Junction> rn = (Network<Junction>) jc.getProjection(GlobalVars.CONTEXT_NAMES.ROAD_NETWORK);

		// 1. Check that there are some objects in each of the contexts
		checkSize(bc, rc, jc);

		// 2. Check that the number of roads matches the number of edges
		if (sizeOfIterable(rc.getObjects(Road.class)) != sizeOfIterable(rn.getEdges())) {
			StringBuilder errormsg = new StringBuilder();
			errormsg.append("There should be equal numbers of roads in the road context and edges in the "
					+ "road network. But there are " + sizeOfIterable(rc.getObjects(Road.class)) + "roads and "
					+ sizeOfIterable(rn.getEdges()) + " edges. ");

			// If there are more edges than roads then something is pretty weird.
			if (sizeOfIterable(rc.getObjects(Road.class)) < sizeOfIterable(rn.getEdges())) {
				errormsg.append("There are more edges than roads, no idea how this could happen.");
				throw new EnvironmentError(errormsg.toString());
			} else { // Fewer edges than roads, try to work out which roads do not have associated edges.
				/*
				 * This can be caused when two roads connect the same two junctions and can be fixed by splitting one of
				 * the two roads so that no two roads will have the same source/destination junctions ("e.g. see here
				 * http://webhelp.esri.com/arcgisdesktop/9.2/index.cfm?TopicName=Splitting_line_features), or by
				 * deleting them. The logger should print a list of all roads that don't have matching edges below.
				 */
				HashSet<Road> roads = new HashSet<Road>();
				for (Road r : rc.getObjects(Road.class)) {
					roads.add(r);
				}
				for (RepastEdge<Junction> re : rn.getEdges()) {
					NetworkEdge<Junction> e = (NetworkEdge<Junction>) re;
					roads.remove(e.getRoad());
				}
				// Log this info (also print the list of roads in a format that is good for ArcGIS searches.
				String er = errormsg.toString() + "The " + roads.size()
						+ " roads that do not have associated edges are: " + roads.toString()
						+ "\nHere is a list of roads in a format that copied into AcrGIS for searching:\n";
				for (Road r : roads) {
					er += ("\"identifier\"= '" + r.getIdentifier() + "' Or ");
				}
				LOGGER.log(Level.SEVERE, er);
				throw new EnvironmentError(errormsg.append("See previous log messages for debugging info.").toString());
			}

		}

		// 3. Check that the number of junctions matches the number of nodes
		if (sizeOfIterable(jc.getObjects(Junction.class)) != sizeOfIterable(rn.getNodes())) {
			throw new EnvironmentError("There should be equal numbers of junctions in the junction "
					+ "context and nodes in the road network. But there are "
					+ sizeOfIterable(jc.getObjects(Junction.class)) + " and " + sizeOfIterable(rn.getNodes()));
		}

		if (GlobalVars.loggerOn) {
			LOGGER.log(Level.FINE, "The road network has " + sizeOfIterable(rn.getNodes()) + " nodes and "
					+ sizeOfIterable(rn.getEdges()) + " edges.");
		}

		// 4. Check that Roads and Buildings have unique identifiers
		HashMap<String, ?> idList = new HashMap<String, Object>();
		for (Building b : bc.getObjects(Building.class)) {
			if (idList.containsKey(b.getIdentifier()))
				throw new EnvironmentError("More than one building found with id " + b.getIdentifier());
			idList.put(b.getIdentifier(), null);
		}
		idList.clear();
		for (Road r : rc.getObjects(Road.class)) {
			if (idList.containsKey(r.getIdentifier()))
				throw new EnvironmentError("More than one building found with id " + r.getIdentifier());
			idList.put(r.getIdentifier(), null);
		}

	}

	public static int sizeOfIterable(Iterable<?> i) {
		int size = 0;
		Iterator<?> it = i.iterator();
		while (it.hasNext()) {
			size++;
			it.next();
		}
		return size;
	}

	/**
	 * Checks that the given <code>Context</code>s have more than zero objects in them
	 * 
	 * @param contexts
	 * @throws EnvironmentError
	 */
	public void checkSize(Context<?>... contexts) throws EnvironmentError {
		for (Context<?> c : contexts) {
			int numObjs = sizeOfIterable(c.getObjects(Object.class));
			if (numObjs == 0) {
				throw new EnvironmentError("There are no objects in the context: " + c.getId().toString());
			}
		}
	}

	/**
	 * Other objects can call this to stop the simulation if an error has occurred.
	 * 
	 * @param ex
	 * @param clazz
	 */
	public static void stopSim(Exception ex, Class<?> clazz) {
		ISchedule sched = RunEnvironment.getInstance().getCurrentSchedule();
		sched.setFinishing(true);
		sched.executeEndActions();
		LOGGER.log(Level.SEVERE, "ContextManager has been told to stop by " + clazz.getName(), ex);
	}

	/**
	 * Move an agent by a vector. This method is required -- rather than giving agents direct access to the
	 * agentGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 * 
	 * @param agent
	 *            The agent to move.
	 * @param distToTravel
	 *            The distance that they will travel
	 * @param angle
	 *            The angle at which to travel.
	 * @see Geography
	 */
	public static synchronized void moveAgentByVector(IAgent agent, double distToTravel, double angle) {
		ContextManager.agentGeography.moveByVector(agent, distToTravel, angle);
	}

	/**
	 * Move an agent. This method is required -- rather than giving agents direct access to the agentGeography --
	 * because when multiple threads are used they can interfere with each other and agents end up moving incorrectly.
	 * 
	 * @param agent
	 *            The agent to move.
	 * @param point
	 *            The point to move the agent to
	 */
	public static synchronized void moveAgent(IAgent agent, Point point) {
		ContextManager.agentGeography.move(agent, point);
	}

	/**
	 * Add an agent to the agent context. This method is required -- rather than giving agents direct access to the
	 * agentGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 * 
	 * @param agent
	 *            The agent to add.
	 */

	public static synchronized void addAgentToContext(IAgent agent) {
		ContextManager.agentContext.add(agent);
	}

	/**
	 * Get all the agents in the agent context. This method is required -- rather than giving agents direct access to
	 * the agentGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 * 
	 * @return An iterable over all agents, chosen in a random order. See the <code>getRandomObjects</code> function in
	 *         <code>DefaultContext</code>
	 * @see DefaultContext
	 */
	public static synchronized Iterable<IAgent> getAllAgents() {
		return ContextManager.agentContext.getRandomObjects(IAgent.class, ContextManager.agentContext.size());
	}

	/**
	 * Get the geometry of the given agent. This method is required -- rather than giving agents direct access to the
	 * agentGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 */
	public static synchronized Geometry getAgentGeometry(IAgent agent) {
		return ContextManager.agentGeography.getGeometry(agent);
	}

	/**
	 * Get a pointer to the agent context.
	 * 
	 * <p>
	 * Warning: accessing the context directly is not thread safe so this should be used with care. The functions
	 * <code>getAllAgents()</code> and <code>getAgentGeometry()</code> can be used to query the agent context or
	 * projection.
	 * </p>
	 */
	public static Context<IAgent> getAgentContext() {
		return ContextManager.agentContext;
	}

	/**
	 * Get a pointer to the agent geography.
	 * 
	 * <p>
	 * Warning: accessing the context directly is not thread safe so this should be used with care. The functions
	 * <code>getAllAgents()</code> and <code>getAgentGeometry()</code> can be used to query the agent context or
	 * projection.
	 * </p>
	 */
	public static Geography<IAgent> getAgentGeography() {
		return ContextManager.agentGeography;
	}

	/* Variables to represent the real time in decimal hours (e.g. 14.5 means 2:30pm) and a method, called at every
	 * iteration, to update the variable. */
	public static double realTime = 0.0; // (start at 0am) //Lukasz Kowalski comment: change to 0
	public static int numberOfDays = 0; // It is also useful to count the number of days.

	@ScheduledMethod(start=1, interval=1, priority=10)
	public void updateRealTime() {
		realTime += (1.0/60.0); // Increase the time by one minute (a 60th of an hour)
		if (realTime >= 24.0) { // If it's the end of a day then reset the time
			realTime = 0.0;
			numberOfDays++; // Also increment our day counter
			//	                if (GlobalVars.loggerOn) {
			LOGGER.log(Level.INFO, "Simulating day "+numberOfDays);
			//	                }
			printTicks();
		}
	}

}

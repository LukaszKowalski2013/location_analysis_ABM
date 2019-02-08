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

package repastcity3.main;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Geometry;

import repastcity3.agent.IAgent;
import repastcity3.environment.Building;
import repastcity3.environment.Route;
import repast.simphony.parameter.Parameters;

/**
 * 
 * @author nick & Lukasz Kowalski
 *
 */
public abstract class GlobalVars {
	
	private static Logger LOGGER = Logger.getLogger(GlobalVars.class.getName());
	
	/* These are strings that match entries in the repastcity.properties file.*/
	public static final String GISDataDirectory = "GISDataDirectory";
	public static final String BuildingShapefile = "BuildingShapefile";
	public static final String RoadShapefile = "RoadShapefile";
	public static final String BuildingsRoadsCoordsCache = "BuildingsRoadsCoordsCache";
	public static final String BuildingsRoadsCache = "BuildingsRoadsCache";

	
	public static final class GEOGRAPHY_PARAMS {
		
		/**
		 * Different search distances used in functions that need to find objects that are
		 * close to them. A bigger buffer means that more objects will be analysed (less
		 * efficient) but if the buffer is too small then no objects might be found. 
		 * The units represent a lat/long distance so I'm not entirely sure what they are,
		 * but the <code>Route.distanceToMeters()</code> method can be used to roughly 
		 * convert between these units and meters.
		 * @see Geometry
		 * @see Route
		 */
		public enum BUFFER_DISTANCE {
			/** The smallest distance, rarely used. Approximately 0.1m*/ //Lukasz Kowalski comment: changed max
			SMALL(0.00000001, "0.1"),
			/** Most commonly used distance, OK for looking for nearby houses or roads.
			 * Approximatey 50m */
			MEDIUM(0.002,"150"), //Lukasz Kowalski comment: changed max to around 200 m
			/** Largest buffer, approximately 550m. I use this when doing things that
			 * don't need to be done often, like populating caches.*/
			LARGE(0.01,"1000"); //Lukasz Kowalski comment: changed max
			/**
			 * @param dist The distance to be passed to the search function (in lat/long?)
			 * @param distInMeters An approximate equivalent distance in meters.
			 */
			BUFFER_DISTANCE(double dist, String distInMeters) {
				this.dist = dist;
				this.distInMeters = distInMeters;
			}
			public double dist;
			public String distInMeters;
		}

		/** The distance that agents can travel each turn. */
		public static final double TRAVEL_PER_TURN = 81; // init: 50 Slower than average (about 2mph) but good for this simulation.
//		Lukasz Kowalski comment: it is speed of walk in meters per minute (1 minute=1 tick), which is 4.9 km/h according to Google Maps. It is important to understand that all speeds are in
//		relation to this one. Which means that distance is made shorter later on, if we travel by car we divide distance by 6.76.
	}
	
	/** Names of contexts and projections. These names must match those in the
	 * parameters.xml file so that they can be displayed properly in the GUI. */
	public static final class CONTEXT_NAMES {
		
		public static final String MAIN_CONTEXT = "maincontext";
		public static final String MAIN_GEOGRAPHY = "MainGeography";
		
		public static final String BUILDING_CONTEXT = "BuildingContext";
		public static final String BUILDING_GEOGRAPHY = "BuildingGeography";
		
		public static final String ROAD_CONTEXT = "RoadContext";
		public static final String ROAD_GEOGRAPHY = "RoadGeography";
		
		public static final String JUNCTION_CONTEXT = "JunctionContext";
		public static final String JUNCTION_GEOGRAPHY = "JunctionGeography";
		
		public static final String ROAD_NETWORK = "RoadNetwork";
		
		public static final String AGENT_CONTEXT = "AgentContext";
		public static final String AGENT_GEOGRAPHY = "AgentGeography";
	
	}
	
	// Parameters used by transport networks
	public static final class TRANSPORT_PARAMS {

		// This variable is used by NetworkEdge.getWeight() function so that it knows what travel options
		// are available to the agent (e.g. has a car). Can't be passed as a parameter because NetworkEdge.getWeight()
		// must override function in RepastEdge because this is the one called by ShortestPath.
		public static IAgent currentAgent = null;
		public static Object currentBurglarLock = new Object();
		//syncronised access to club Lukasz Kowalski comment:. like above - threading 'race conditions'
		
		public static final String WALK = "walk";
		public static final String BUS = "bus";
//		public static final String TRAIN = "train";
		public static final String CAR = "car";
		// List of all transport methods in order of quickest first
		public static final List<String> ALL_PARAMS = Arrays.asList(new String[]{CAR, BUS, WALK}); //Lukasz Kowalski comment: change there was Train too

		// Used in 'access' field by Roads to indicate that they are a 'majorRoad' (i.e. motorway or a-road).
		public static final String MAJOR_ROAD = "majorRoad";		
		// Speed advantage for car drivers if the road is a major road'
		public static final double MAJOR_ROAD_ADVANTAGE = 3; //this is also a multiplier (at networkedge getSpeed() there is equation: Lukasz Kowalski comment: I commented it out at: NetworkEdge.getSpeed()

		// The speed associated with different types of road (a multiplier, i.e. x times faster than walking)
		// it is parameter, that controls the speed for car drivers and public transport passengers 
		// - it should be rather used for visualization purposes and some primary checks.
		// In my opinion (LK) all distances should be eventually pre-calculated, so that the model is really fast.
		public static double getSpeed(String type) {
			if (type.equals(WALK)) 
				return 1; //Lukasz Kowalski comment: it was 1 //Lukasz Kowalski comment: presumption, people travel 4,9 km/h or 81m/min according to GoogleMaps(my data are unreliable here, because of spatial units I used (walking distanes are very short)
			else if (type.equals(BUS))
				return 4.46; //it was 2 //Lukasz Kowalski comment:  myData: passangers travel 22,3 km/h (st.dev=12,2) or 371 m/min (==m/tick) => multiplier=4.46 (watch out! this speed was based on google distance in km and jakdojade.pl site distance in min - but buses didn't had to go along fastest road option - however I think they usually did)
//			else if (type.equals(TRAIN))
//				return 1000;
			else if (type.equals(CAR))
				return 6.76;//Lukasz Kowalski comment: it was 3 //Lukasz Kowalski comment: myData: drivers travel 33,8 km/h (st.dev= 12,3) or 563m/min=> multiplier=6.76
			else {
				LOGGER.log(Level.SEVERE, "Error getting speed: unrecognised type: "+type);
				System.out.print("transport_params- ups!!!");
				return 1;
			}
		}
	}

	//static parameters used by agents
	public static double howLongAtClub = 1.0; //1 hour // it can be a parameter changed by user, but than I should change checkClubLater 
	
	public static int numberOfPools = 27;  // +7 it has to be hard-coding because we MyMatrixes.java, distances added from file.
	public static int numberOfFitness = 122; // +7  it has to be hard-coding because we MyMatrixes.java, distances added from file.
	
	//max distance between home-club - SET at agents build - I put this variables, because minRank is not so accurate. - there's about 200 m error at the border.
	public static double maxFcarKM=20;
	public static double maxFbusKM=14;
	public static double maxScarKM=22;
	public static double maxSbusKM=16;
	
	public static double minRankCar; // Fit auto=0.00985  swim auto=0.0353  //if agent's best club has 3-ranking in sRanking below this value, agent stays home //Math.random() * 0.3; 
	public static double minRankBus; //  fit MPK=0.00799  swim MPK=0.01208
	
	// my parameters to control the model results
	public static double experienceCar; // 0.3 / variable is used to decrease Ranking of a club if agent can't enter there // Math.random()*0.3 +0.01 ;//
	public static double experiencePT;

	//FOR BUS RIDERS &FOR CAR DRIVERS: if we divided it by x= 100, for every 10 minutes of a = access time we loose 10% (0.1) of probability in Ranking (if x=50, a=10min, we get 0.2)
	public static double trafficImpact; //=10; //we assume if club is located in parking zone, driver have to spend 15 minutes more to get to club //Math.random()*200; //
	public static double busStopDistanceImpact; //=100; //!dist2road!+ !dist4walk!)/81 WATCH@ it is calculated for agents home zoneID and for clubs!!! Note that if access time in min is greater than busStopDistanceImpact for club X, this club has 0 points in sRanking //Math.random()*200; //

	public static double placesPerRoomAtFitness=15; //15 = 100% supply
	public static double placesPerLaneAtPool=8; //8 - 100% supply


	public static boolean loggerOn=false; // it works only for Route.java now and for most things at ContextManager 
	// model_log.txt is 500 mb big after 2 fitness simulations!!!
	
	public static String selectedFacilityType; 
	public static String displayMovement;
	
	public static boolean teleportationOn; //we skip all routing here
	public static String mySport = "swimming"; //swimming or fitness
	
	public static void setMySport(String selectedFacilityType) {
		if (selectedFacilityType.contains("swimming")) {
		GlobalVars.mySport = "swimming";}
		else if (selectedFacilityType.contains("fitness")){
			GlobalVars.mySport = "fitness";}
	}

	public static void setTeleportationOn(String displayMovement) {
		if (displayMovement.contains("display_movement")) {
		GlobalVars.teleportationOn = false;}
		else if (displayMovement.contains("teleportation_on")){
			GlobalVars.teleportationOn = true;}
		}
	
	public static double[][] hours= 
		   	//hours /Swimming /Fitness
//			the numbers in 2nd and 3rd column stand for popularity of certain hours
//			according to authors survey in Krakow (more information can be found in JASSS article)
					{{6,5.76,1.93},
					{7,4.68,4.82},
					{8,4.32,5.21},
					{9,3.42,2.89},
					{10,0.0,2.6},
					{16,3.6,5.98},
					{17,7.74,8.01},
					{18,14.22,17.66},
					{19,14.04,18.82},
					{20,17.28,17.27},
					{21,5.94,5.5}};
	}
	


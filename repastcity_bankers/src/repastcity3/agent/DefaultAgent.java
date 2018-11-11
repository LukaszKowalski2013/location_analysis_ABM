/*
©Copyright 2012 Nick Malleson
This file is part of RepastCity.

RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
 */

package repastcity3.agent;          

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repastcity3.environment.Building;
import repastcity3.environment.Route;
import repastcity3.main.ContextManager;
import repastcity3.main.GlobalVars;
import repastcity3.main.MyMatrixes;

public class DefaultAgent implements IAgent {

	private static Logger LOGGER = Logger.getLogger(DefaultAgent.class
			.getName());

	private Building home; // Where the agent lives
	//private Building workplace; // Where the agent works
	private Route route; // An object to move the agent around the world
	private String transport = ""; 
	private List<String> transportAvailable = null;

	//private boolean goingHome = false; // Whether the agent is going to or from
	// their home

	private static int uniqueID = 0;
	private int id;

	
	private int whereAmI=0; // This variable says where this agent is.
	//0 - home, 1 - work, 2 - club, 3 - on the way, 4- at destination

	private double myFavouriteClub=0.0; //myId index and at the same time it can be index of row in sRanking array
	int numberOfClubChanges=0; //this variable is to report number agents who changed their favourite club
	double coolingVariable; //variable is used to decrease Ranking of a club if agent can't enter there
	private double minRank; //if agent's best club has 3-ranking in sRanking below this value, agent stays home 
	
	private double myTimeOfDeparture; //for routing system checks
	private double myTimeAtClub; //for routing system checks
	private int fireOnce = 1; //0-no, 1-yes
	
	private double myTime1; // it is drawn
	private double myTime2; // it is drawn
	private double[] myTimeList = {myTime1, myTime2};
	int traffic; // it is used to set if there is traffic in city centre or not. It can be 0 or 1.
	private int practicedToday=0;
	// 0-starting point, 1-he practiced 2-he was waiting (he didn't practiced)
	private boolean heCanGo=true;//this is set by Context Manager, I added it because it is convenient for me to control departure time of an by 'theTime>myTime' (theTime cannot be used with ==) 

	
	// Used whenever Geometry objects need to be created - its needed for teleportation
	private final GeometryFactory geomFac = new GeometryFactory();
	
	//KEJ+ MY VARIABLES hold in myRanking
	//	sRanking indexes: 0- myId 1-idClub 2-myHour 3-ranking 4-HCkm 5-HCmin 6-howCrowded (in persons per place) 7-accessTime 8-initialRanking (based on distance equation)
	// null for sport split
	private double[][] sRanking =null;// TEMP new double[GlobalVars.numberOfPools*myTimeList.length][9];
	private Building[] allClubs =null; // TEMP new Building[GlobalVars.numberOfPools]; //KEJ+ an array of all clubs

	private double ZONE_ID;//taken from Shapefile

	// sRanking => ranking of swimming pools (20); [9] because there are 9 columns (see below);
	//1st equation in [] is like this because number of rows is multiplyed by hours which agent check
	// - so every club is present there twice, when agent have to check 2 option of hours.
	
	//my variables for hours calculations -this is for an option when we draw times every simulation for every agent
	private double myFavouriteHour;
	double[][] hours= 
   	//hours /Swimming /Fitness
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

	public DefaultAgent() { //bankers change. CONSTRUCTOR.
		this.id = uniqueID++;
		
		//sport split
		if (GlobalVars.mySport=="swimming"){
			sRanking =new double[GlobalVars.numberOfPools*myTimeList.length][9];
			allClubs =new Building[GlobalVars.numberOfPools]; //KEJ+ an array of all clubs
		}
		else if(GlobalVars.mySport=="fitness"){
			sRanking =new double[GlobalVars.numberOfFitness*myTimeList.length][9]; //temp for fitness
			allClubs=new Building[GlobalVars.numberOfFitness]; //TEMP for fitness [or for fitter :)]
		}
		
		// Find a building that agents can use (home or clubs for now). First, iterate over all buildings in the model (there should be less than 10 000)
		// populate 2 arrays of clubs.
		int i=0;
		int j=0; //temp for fitness
		for (Building b:ContextManager.buildingContext) {
			if (GlobalVars.mySport=="swimming"){
				//make a list of all clubs & populate idClub column in sRanking, depending on what sport do we check
				//1 swimming pools
				if (b.getType()==3 && b.getSwimming()==1 ) {
					allClubs[i]= b;

					//here we populate myId column (index 0)
					sRanking[i][0]=i;
					sRanking[GlobalVars.numberOfPools+i][0]=GlobalVars.numberOfPools+i;				
					
					//here we populate idClub column (index 1)			
					sRanking[i][1]=b.getClubID();
					sRanking[GlobalVars.numberOfPools +i][1]=sRanking[i][1];
									
					i++;
				}				
			}

			else if (GlobalVars.mySport=="fitness"){
				//2 fitness clubs
				if (b.getType()==3 && b.getFitness()==1 ) {
					allClubs[j]= b;

					//here we populate myId column (index 0)
					sRanking[j][0]=j;
					sRanking[GlobalVars.numberOfFitness+j][0]=GlobalVars.numberOfFitness+j;				
					
					//here we populate idClub column (index 1)			
					sRanking[j][1]=b.getClubID();
					sRanking[GlobalVars.numberOfFitness +j][1]=sRanking[j][1];
					j++;
				}	
			}

		} //end of for reading buildings loop

		//set my time  -this is for an option when we draw times every simulation for every agent
		//VERSION 1. this was an option for stable hours for every simulation drawn in rozmnazanie.java, see F:\\swiat_gis\\ABM_workspace\\RozmnazanieAgentow\\
		myTime1=whatsMyFavouriteHour();
		myTime2=whatsMyFavouriteHour();
		while (myTime2==myTime1){
			myTime2=whatsMyFavouriteHour();
//			System.out.println("my times are equal, but I'll change them"); //checked - its fine
		}
		
//		System.out.println("juhu!");
//		if(isAvailableTransport("car")){
//			minRank=GlobalVars.minRankCar; //if agent's best club has 3-ranking in sRanking below this value, agent stays home 
//			System.out.println("hurra!");
//		}
//		else if(isAvailableTransport("bus")){
//			minRank=GlobalVars.minRankBus; 
//			System.out.println("hurra!");
//		}
		
//		checkMyRanking();
				
	} //end of constructor

	// Kej+ STEP - AGENT CONTROL

	@Override
	public void step() throws Exception {
		// step is done in 3 parts: 1. Checks where am I 2. if at destination do sth 3. else: travel

		// 1.  See what the time is, this will determine what the agent should be doing. The BigDecimal stuff
		// is just to round the time to 5 decimal places, otherwise it will never be exactly 9.0 or 17.0.
//		double theTime = BigDecimal.valueOf(ContextManager.realTime).round(new MathContext(2,RoundingMode.HALF_UP)).doubleValue();
		 //performance optimisation point - can we move it to ContextManager? agent's actions are not controlled by time here //Kej+ I changed from 5 to 2
		
		if (fireOnce==1){
			myRankingUpdate(transport.contains("car")); //it should stay as it is - if there is no car- agent should travel by bus
////			[1][5]= Route.getRouteDistance(this, this.allClubs[whatsClubIndexInAllClubs(1)].getCoords(), this.allClubs[whatsClubIndexInAllClubs(1)])[1];
			
			whatsMyFavouriteClub(); // my 1st favourite club
			fireOnce--;

//			if(this.getFavClubStrefa_moj().contains("POZA_CENTRUM")){
//				System.out.println(this.getFavClubStrefa_moj());			
//			}

//			checkMyRanking();
//		System.out.print(Arrays.deepToString(sRanking2));
		}

		// 2. IS HE HOME? //TEMP add practicedToday option, so he doesn't have to check time, when he's home
		if (whereAmI==0 && practicedToday==0 && heCanGo){
			if (sRanking[(int)myFavouriteClub][3] > minRank){

				if (ContextManager.realTime > sRanking[(int)myFavouriteClub][2]) { // I assume at 4pm he goes to club or home //Kej+potential optimisation point
				
											
					//TODO teleportation here
					if(GlobalVars.teleportationOn==false){
						whereAmI=3; // he is on his WAY now; 3 = agent is changing his location to route
						this.route = new Route(this, this.allClubs[whatsClubIndexInAllClubs(myFavouriteClub)].getCoords(),
								this.allClubs[whatsClubIndexInAllClubs(myFavouriteClub)]); // Create a route home	
						myTimeOfDeparture=ContextManager.realTime;
						heCanGo=false;
					}
					else if(GlobalVars.teleportationOn){
						teleportMe(this, this.allClubs[whatsClubIndexInAllClubs(myFavouriteClub)].getCoords(),
								this.allClubs[whatsClubIndexInAllClubs(myFavouriteClub)]);
						whereAmI=4; //4- at destination
						myTimeOfDeparture=ContextManager.realTime;
						heCanGo=false;
					}
				}
			}
		}

		// 3. IS HE FINISHED AT CLUB? SORTING MECHANISM, 2/2. Check if they finished their sport practice //TEMP IT CAN BE SHORTER
		else if(whereAmI==2){ // IT WAS myTimeAtClub before INSTEAD of myTime2 

			//agents, who did not practiced OR DID practice sport today change their ranking (only for some agents) (controlled by club)
			if (practicedToday==1){ //yes. he practiced
				//change ranking, go home
				// agent DID get in, so we increase value of his favouriteClub by cooling variable value 
				sRanking[(int)myFavouriteClub][3]+=coolingVariable;
				if(sRanking[(int)myFavouriteClub][3]>1){//we don't want this values to begreater than 1 in ranking points
					sRanking[(int)myFavouriteClub][3]=1;
				}				
				// note down crowd (later on - other things)
//				sRanking[(int)myFavouriteClub][6]=allClubs[whatsClubIndexInAllClubs(myFavouriteClub)].howCrowdedWasIt(); //crowd

				practicedToday=0;
				if(GlobalVars.teleportationOn==false){
					whereAmI=3;
					this.route = new Route(this, this.home.getCoords(), this.home);
				}
				else if(GlobalVars.teleportationOn){
					teleportMe(this, this.home.getCoords(), this.home); // 0 means agent leaves now
					whereAmI=0; //0- home
				}
			}

			if (practicedToday==2){ //no. he has just waited and didn't practiced
			
				// note down accessTime & crowd (later on - other things)
				sRanking[(int)myFavouriteClub][7]= 1; //TEMP 1 should be changed to real accessTime TODO
				sRanking[(int)myFavouriteClub][6]=allClubs[whatsClubIndexInAllClubs(myFavouriteClub)].howCrowdedWasIt(); //crowd// right now I use it just for information
				// agent didn't get in, so we decrease value of his favouriteClub by cooling variable value 
				sRanking[(int)myFavouriteClub][3]-=coolingVariable;
				if(sRanking[(int)myFavouriteClub][3]<0){//we don't want negative values in ranking points
					sRanking[(int)myFavouriteClub][3]=0;
				}
				

				//TODO teleportation here
				if(GlobalVars.teleportationOn==false){
					this.route = new Route(this, this.home.getCoords(), this.home);
					whereAmI=3; //travelling (or waiting for teleport)
				}
				else if(GlobalVars.teleportationOn){
					teleportMe(this, this.home.getCoords(), this.home);
					whereAmI=0; //0-home
				}
				
				whatsMyFavouriteClub(); //CLUB CHANGE//GO TO THE HEART OF MY ABM
				
				practicedToday=0;
				//temp// delay of exit time is 2 minutes now = .033 of an hour at theTime
				//this should be rather: if (hourOfResetEntries==myTime1){iterate through allClubs and raport its crowd - isn't it deleted at this time?}
				//else if [same but 4 myTime2].

//				System.out.print(ContextManager.hourOfResetEntries + " "); at what time there is reset
//				checkMyRanking();
			}
			
			
		} //end of finished at club

		if(GlobalVars.teleportationOn==false){ // TODO here we have the hardest part
			if (this.route == null) {
			} // Don't do anything if a route hasn't been created.

			//		4. if agent reached his destination, he has to do: 
			else if (this.route.atDestination()) { 
				// Have reached our destination, lets delete the old route (more efficient).t

				this.route = null;
				whereAmI=4; //4 means "agent is at destination // it is here because of teleportation reasons
			}
			//		5. TRAVEL
			else {
				// Otherwise travel towards the destination
				this.route.travel();
			} // else
		}

		if (whereAmI==4){
			int myClubIndexInAllClubs = whatsClubIndexInAllClubs(myFavouriteClub);

			//4A. AM I AT CLUB? 
			if (allClubs[myClubIndexInAllClubs].getCoords() 
					== ContextManager.getAgentGeometry(this).getCoordinate()){
				//if agent is by the club, he checks if he can enter (if not => he goes home, if yes - he stay there for 1 hour)

				if (!allClubs[myClubIndexInAllClubs].isAgentAtThisClub(this)){ //check if he can enter 
					//TEMP THERE WAS, allClubs[myClubIndexInAllClubs].mayIenter() &&
					//HE ENTERS THE CLUB
					whereAmI=2;
					allClubs[myClubIndexInAllClubs].addAgent(this);
					allClubs[myClubIndexInAllClubs].clientEntry(); //add up client entry
					myTimeAtClub=ContextManager.realTime;
					double accessTime=0; //TEMP it should be taken from club
					//note down accessTime and HCmin in sRanking
					sRanking[(int)myFavouriteClub][7]= accessTime; //TEMP 1 should be changed accessTime
					sRanking[(int)myFavouriteClub][5]=Math.round((myTimeAtClub-myTimeOfDeparture)*60); // temp, watch out, performace optimisation point

					//					System.out.print("\n"+"HOP ");
					//					System.out.print("agent " + this.id + " got here in (h): ");
					//					System.out.printf("%.2f", (ContextManager.realTime-myTimeOfDeparture));
					//					
					//					System.out.print(" and traveled: ");
					//					System.out.printf("%.2f",HC);
				}
			}

			//4B. AM I HOME?
			else { //if (this.home.getCoords() == ContextManager.getAgentGeometry(this).getCoordinate())
				whereAmI=0;
				//				checkMyRanking();// show sRanking at the end of the day
			}

		} // end of atDestination triggered by whereAmI == 4 // before it was end of if ...atDestination
		
//		if (whereAmI==3 && GlobalVars.teleportationOn){ // here we have our option for teleporting
//			//TODO 
//		}


	}	// step()

	
	//MY METHOD, SETTERS AND GETTERS
	/**
	 * There will be no inter-agent communication so these agents can be
	 * executed simulataneously in separate threads.
	 */
	@Override //KEJ+ I changed it
	public final boolean isThreadable() {
		return false;
	}

	@Override
	public void setHome(Building home) { //there should be error here
		this.home = home;
	}

	@Override
	public Building getHome() {
		return this.home;
	}

	@Override
	public <T> void addToMemory(List<T> objects, Class<T> clazz) {
	}

	@Override
	public List<String> getTransportAvailable() {
		//List<String> myTransport = new ArrayList<String>();
		//myTransport.add("bus");
		return this.transportAvailable; //IT SHOULD BE WRITTEN IN PEOPLE SHAPEFILE
	} //	return null;

	// return new Arrays.asList(new String[]{"car", "bus"});
	
	@Override
	public boolean isAvailableTransport(String transport1){ //I added it so IAgent can read it
		return transport.contains(transport1);
	}

	@Override
	public String toString() {
		return "Agent " + this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultAgent))
			return false;
		DefaultAgent b = (DefaultAgent) obj;
		return this.id == b.id;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	public void setTransport(String transport) { //taken from shapefile method
		this.transport=transport;
		this.transportAvailable = Arrays.asList( this.transport.split("\\s+")); //it splits a string because 1 agent can use at least 2 modes of transport (walk + sth else).
//		System.out.print(this.id+":"+this.transport+" - "+this.transportAvailable.toString()); //check
	}
	public String getTransport() {
		return this.transport;
	}
	
	public void setZONE_ID(double ZONE_ID){
		this.ZONE_ID = ZONE_ID;}
	public double getZONE_ID(){
		return ZONE_ID;	}
	
	public void setMyTime1(double myTime1){
		this.myTime1 = myTime1;}
	public double getMyTime1(){
		return this.myTime1;	}

	public void setMyTime2(double myTime2){
		this.myTime2 = myTime2;}
	public double getMyTime2(){
		return this.myTime2;	}
	
	
	//get sRanking 
	public double[][] getAgentsSRanking(){
		return this.sRanking;
	}
	//get favourite club row
	public int getAgentsFavouriteClub(){
		return (int)this.myFavouriteClub;
	}
	 //get minRank
	public double getMinRank(){
		return this.minRank;
	}
	public void setMinRank(double AgentsMinRank){
		minRank=AgentsMinRank;
	}
	public void setCoolingVariable(double AgentsCoolingVariable){
		coolingVariable=AgentsCoolingVariable;
	}
	public String getFavClubStrefa_moj(){
		return this.allClubs[whatsClubIndexInAllClubs(myFavouriteClub)].getStrefa_moj();
	}
	
	//Kej+how to get to club with id X  //WARNING, THIS OPTION IS ONLY FOR TWO myHour
	public int whatsClubIndexInAllClubs(double myId){
		int searchedIndex=-1;
		for (int s=0; s< sRanking.length; s++) { 
			// check if the building is a club

			if (myId==sRanking[s][0]){
				if (s>allClubs.length - 1){
					searchedIndex=s-allClubs.length;
				}
				else{
					searchedIndex=s;
				}
				break;
			}
		}
		return searchedIndex;
	}


	/**
	 * Here we draw an hour, when agent want to practice sport activity, based on probability from my survey
	 */
	public double whatsMyFavouriteHour(){
		double sumOfRankingPoints=0;

		for (int f=0; f<hours.length;f++){
			sumOfRankingPoints +=hours[f][1]; //1 is for swimming, it should be parameter that we enter here
		}

		//here we're looking for the best hour, based on hours array in column 1 (swimming) and 2 (fitness), which play role of weights, so this is random search based on probability
		double p=Math.random()*sumOfRankingPoints;
		double countWeight =0;
		for (int f=0; f<hours.length;f++) {
			countWeight += hours[f][1];
			if (countWeight >= p){
				//            	System.out.print(" hurra!!!"+ f + " ");
				myFavouriteHour=hours[f][0]; //col 0 is an hour
				break;
			}
		}
		return myFavouriteHour; 
	}

	
	/**
	 * here we draw a favourite club, based on probability
	 */
	public double whatsMyFavouriteClub(){
//		if (numberOfClubChanges<7){ //7 says that each agent can change his club max 7 times. why the hell only 7?
			double sumOfRankingPoints=0;
			
		for (int f=0; f<sRanking.length;f++){

			sumOfRankingPoints +=sRanking[f][3];
		}
		
		//here we're looking for the best club, based on ranking points in column 3, which play role of weights, so this is random search based on probability
		double p=Math.random()*sumOfRankingPoints;
		double countWeight =0;
        for (int f=0; f<sRanking.length;f++) {
        		countWeight += sRanking[f][3];
            if (countWeight >= p){
//            	System.out.print(" hurra!!!"+ f + " ");
            	myFavouriteClub=f;
            	break;
            }
        }
        
		return myFavouriteClub;
		
	}
	
	//check if Ranking is OK
	private void checkMyRanking(){
		String check = Arrays.deepToString(sRanking); //temp for fitness it should be sRanking2 TO COUNT FITNESS
		LOGGER.log(Level.INFO, "Agent " + this.toString() + "has favourite club with myId: "+ myFavouriteClub+
				" and has ranking list of all clubs which is: " + check) ;
	}


	public void setPracticedToday(int b){
		practicedToday=b;
	}

//	public void setMyDistance(double myDistance){
//		HC= myDistance;
//	}
	private int setMyRowInMatrix(double[][] matrix){
		int row=0;
		for (int f=0; f<matrix.length;f++){
			if (ZONE_ID == matrix[f][0]){ //3-ranking
				row=f;
				break;
			}
		}
		return row;
	}
	
	private int setMyColInMatrix(double[][] matrix, int sRankingClubIndex){
		int col=0;
		int c=1; //clubID starts here
		boolean found=false;
		while (!found && c< matrix[0].length){
			if(matrix[0][c]==sRanking[sRankingClubIndex][1]){
				found=true;
				col=c;//sRanking[c][6]=inputArray[r][1];
				break;
			}
			else{
				c++;
			}
		}
		return col;
	}


	// this method is called to populate ranking with HCkm
	private void myRankingUpdate(boolean doesHeHasACar){
		//		double [379][21] myHCMatrix;
		//		double [379][21] myrankMatrix;

		double[][] myHCMatrix = null;
		double[][] myrankMatrix=null;
		double myspeed = 0;
		
		int myRowInMatrix=0;
		int myColInMatrix=0;
		
		int i; //col in matrix
		
		//here we check if agent uses car - unless he uses bus
		if (GlobalVars.mySport=="swimming"){ 
			if (doesHeHasACar){
				myHCMatrix = MyMatrixes.MatrixHCkmSWIM_AUTO; //it should be changed for fitness
				myrankMatrix= MyMatrixes.MatrixrankSWIM_AUTO; //it should be changed for fitness
				myspeed=33.8; //car speed in km/h
			}
			else if (!doesHeHasACar){
				myHCMatrix = MyMatrixes.MatrixHCkmSWIM_MPK; //it should be changed for fitness
				myrankMatrix= MyMatrixes.MatrixrankSWIM_MPK; //it should be changed for fitness
				myspeed=22.3; //bus speed in km/h
			}			
		}
		else if (GlobalVars.mySport=="fitness"){ 
			if (doesHeHasACar){
				myHCMatrix = MyMatrixes.MatrixHCkmFIT_AUTO; //it should be changed for fitness
				myrankMatrix= MyMatrixes.MatrixrankFIT_AUTO; //it should be changed for fitness
				myspeed=33.8; //car speed in km/h
			}
			else if (!doesHeHasACar){
				myHCMatrix = MyMatrixes.MatrixHCkmFIT_MPK; //it should be changed for fitness
				myrankMatrix= MyMatrixes.MatrixrankFIT_MPK; //it should be changed for fitness
				myspeed=22.3; //bus speed in km/h
			}
		}
		
		 myRowInMatrix=setMyRowInMatrix(myHCMatrix); ;

			
		
		//filling out the rest of fields of sRanking
		for (int s=0; s< sRanking.length; s++) {

			myColInMatrix=setMyColInMatrix(myHCMatrix, s);
			i= myColInMatrix;
			
//			System.out.println(myrankMatrix[0][i] + " & "+ sRanking[s][1]); //KEJ check right col search // it is OK for swimming pools
			
			//8 - initialRanking (number of points based on distance
			sRanking[s][8] = myrankMatrix[myRowInMatrix][i];
			
			// 4 - HCkm
			sRanking[s][4] =myHCMatrix[myRowInMatrix][i];

			//	System.out.print(ZONE_ID+" "); 
			// 5 - HCmin
			sRanking[s][5]= Math.round((sRanking[s][4]/myspeed)*60); // it is in min. Watch out, if you want to sync it with myTime

			// 2 - myHour step 1/2
			if (s<GlobalVars.numberOfPools){ 
			sRanking[s][2]=myTime1;}
			else{
			sRanking[s][2]=myTime2;}

			
			//setting Traffic for cars in city centre, based on paid parking area (10:00 - 20:00)
			if ((sRanking[s][2]>19.5 || sRanking[s][2]<10.5 ) && doesHeHasACar){ //19:30 because it is the time when the trip starts 
				//- this function goes before myHour, because right now it shows myTime1
				traffic = 0;	}
			else{
				traffic=1;	}
			
			// 7 - accessTime
			if (doesHeHasACar) {// CAR DRIVERS     	//if agent doesn't have a car, he travels by bus.
				sRanking[s][7]= (this.allClubs[whatsClubIndexInAllClubs(s)].GetAccessCar()* GlobalVars.trafficImpact)*traffic; //GetAccessCar() - for city centre's clubs is set to 1 in ArcGIS, so by now traffic variable is a duplicate - TODO - get rid off this duplicate
			}
			else if(!doesHeHasACar){ //BUS RIDERS
				sRanking[s][7]= (this.allClubs[whatsClubIndexInAllClubs(s)].GetAccessBus() +home.GetAccessBus())/GlobalVars.busStopDistanceImpact;
			}
			// 2 - myHour step 2/2
			if(GlobalVars.teleportationOn==false){ //TODO -just marked. If teleportation is on - agent just teleports to the club at myHour
				sRanking[s][2]= (double) Math.round( (sRanking[s][2] - sRanking[s][5]/60) * 100) / 100 ;
				// We subtract time needed to get to club from myHour (time when agent has to be in club)
				//strange thing here is that still agents time calculated this way is longer then time calculated by departureTime and arrivalTime
				//which means we have to control situation when agents want to get to the club too early - in order not to spoil crowd coefficient calculation,
				//which is heart of this ABM
			}

			//STARTING POINT OF 3-RANKING
			//3-ranking (in number of points)
			sRanking[s][3] =  Math.round( ( myrankMatrix[myRowInMatrix][i]- sRanking[s][7]) * 1000)  / 1000.0 ;
			//this initial ranking is based on distance [myRowInMatrix] and accesTime 
			if (sRanking[s][3]<0){
				sRanking[s][3]=0;
			}
			// here we set ranking points to 0 for every new location, apart of the chosen one (it there is any)
			if (sRanking[s][1]>=3000 && ContextManager.chosenLocationIdClub!=sRanking[s][1]){ // every new location has its idClub >= 3000
				sRanking[s][3]=0;
//				System.out.println("hurra! new location with ID: "+sRanking[s][1] + " was set to " + sRanking[s][3] ); //check
			}
				//and every club where capacity is 0 at myTime
			if (this.allClubs[whatsClubIndexInAllClubs(s)].getCapacityAtTime(myTime1)==0){
				sRanking[s][3]=0;
			}
			if (this.allClubs[whatsClubIndexInAllClubs(s)].getCapacityAtTime(myTime2)==0){
				sRanking[s][3]=0;
			}
			
//			here we make sure that if club-home Distance in km is greater than max - ranking is 0 then 
			if(GlobalVars.mySport=="fitness"){
				if (doesHeHasACar){
					if (sRanking[s][4] > GlobalVars.maxFcarKM){
						sRanking[s][3]=0;}
				}
				else{
					if (sRanking[s][4] > GlobalVars.maxFbusKM){
						sRanking[s][3]=0;}
				}
			}
			if(GlobalVars.mySport=="swimming"){
				if (doesHeHasACar){
					if (sRanking[s][4] > GlobalVars.maxScarKM){
						sRanking[s][3]=0;}
				}
				else{
					if (sRanking[s][4] > GlobalVars.maxSbusKM){
						sRanking[s][3]=0;}
				}
			}
			
			i++;
		}
		
		//	reset big arrays to null because we don't need them anymore
		myHCMatrix = null;
		myrankMatrix=null;

	} //end of myRankingUpdate
	
	
	public void setHeCanGo(){
		heCanGo=true;
	}
	
	// NOT USED NOW
	//this method is called to set global knowledge about crowd in clubs - it should greatly decrease number of iterations
	// it isn't very behavioural method, though
	public void setCrowdAwareness(double[][] inputArray, double inputHourOfResetEntries){
		for (int i=0; i<sRanking.length;i++){
			// should be sth like this, but with correct hour: if (inputHourOfResetEntries==sRanking[i][2]){
			//find a value of idClub in inputArray and set it to x variable
			int r=0;
			boolean found=false;
			while (!found && r<inputArray.length){
				if(inputArray[r][0]==sRanking[i][1]){
					found=true;
					sRanking[i][6]=inputArray[r][1];					
				}
				else{
					r++;
				}
			}
		}
	}
	
	//TODO
	//KEJ+ new way of transportation invented in Poland by Kej
	private void teleportMe(IAgent agent, Coordinate destination, Building destinationBuilding) {
			ContextManager.moveAgent(this, geomFac.createPoint(destination));
	}
	
	
	
} //defaultAgent

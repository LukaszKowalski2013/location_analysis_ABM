package repastcity3.main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


import au.com.bytecode.opencsv.CSVReader;


//CsvReader2
public class MyMatrixes // this class is made to hold and calculate matrixes, which should shorten time of simulation initialisation, if there would be many agents
{
	//here there will be 8 matrixes: 4 HC distnce matrixes (2t x 2s) + 4 init ranking points (2t x 2s)
	static String file_MatrixHCkmFIT_AUTO = "./data/HCkmFIT_AUTO.csv";
	static String file_MatrixHCkmFIT_MPK = "./data/HCkmFIT_MPK.csv";
	static String file_MatrixHCkmSWIM_AUTO = "./data/HCkmSWIM_AUTO.csv";
	static String file_MatrixHCkmSWIM_MPK = "./data/HCkmSWIM_MPK.csv";
	
	static String file_MatrixrankFIT_AUTO = "./data/rankFIT_AUTO.csv";
	static String file_MatrixrankFIT_MPK = "./data/rankFIT_MPK.csv";
	static String file_MatrixrankSWIM_AUTO = "./data/rankSWIM_AUTO.csv";
	static String file_MatrixrankSWIM_MPK = "./data/rankSWIM_MPK.csv";
	
	static String file_FIT_AUTOinput = "./data/FIT_AUTOinput.csv";
	static String file_FIT_MPKinput = "./data/FIT_MPKinput.csv";
	static String file_SWIM_AUTOinput = "./data/SWIM_AUTOinput.csv";
	static String file_SWIM_MPKinput = "./data/SWIM_MPKinput.csv";	
	
	
	static String file_timeArraySWIM = "./data/timeArraySWIM.csv";	
	static String file_timeArrayFIT = "./data/timeArrayFIT.csv";	
	
	//	        CsvReader2 parseCSVFile = new CsvReader2();

	//	        System.out.println("Starting to parse CSV file using opencsv");
	//	        parseCSVFile.parseUsingOpenCSV(file_MatrixHCkmSWIM_AUTO);

//	here there are my 8 matrixes:
	//4 distance matrixes:
	public static double[][] MatrixHCkmFIT_AUTO =new double[378][116+7]; //+7 new locations
	public static double[][] MatrixHCkmFIT_MPK =new double[378][116+7];
	
	public static double[][] MatrixHCkmSWIM_AUTO =new double[378][21+7];
	public static double[][] MatrixHCkmSWIM_MPK =new double[378][21+7];
	
	//and 4 initial ranking matrixes:
	public static double[][] MatrixrankFIT_AUTO =new double[378][116+7];
	public static double[][] MatrixrankFIT_MPK =new double[378][116+7];
	
	public static double[][] MatrixrankSWIM_AUTO =new double[378][21+7];
	public static double[][] MatrixrankSWIM_MPK =new double[378][21+7];
	
	// and 4 input matrixes for later results
	public static double[][] FIT_AUTOinput =new double[378][116+7];
	public static double[][] FIT_MPKinput =new double[378][116+7];
	
	
	public static double[][] SWIM_AUTOinput =new double[378][21+7];
	public static double[][] SWIM_MPKinput =new double[378][21+7];
	
	// and 2 timeArrays for fitness and swimming
	public static double[][] timeArrayFIT=new double[13][116+7]; //times are in rows (apart last one - 9191) clubID are in cols
	public static double[][] timeArraySWIM=new double[13][21+7];
	
	public static void parseAllMatrixes(){
		parseMatrix(file_MatrixHCkmFIT_AUTO, MatrixHCkmFIT_AUTO);
		parseMatrix(file_MatrixHCkmFIT_MPK, MatrixHCkmFIT_MPK);
		
		parseMatrix(file_MatrixHCkmSWIM_AUTO, MatrixHCkmSWIM_AUTO);
		parseMatrix(file_MatrixHCkmSWIM_MPK, MatrixHCkmSWIM_MPK);
		
		parseMatrix(file_MatrixrankFIT_AUTO, MatrixrankFIT_AUTO);
		parseMatrix(file_MatrixrankFIT_MPK, MatrixrankFIT_MPK);
		
		parseMatrix(file_MatrixrankSWIM_AUTO, MatrixrankSWIM_AUTO);
		parseMatrix(file_MatrixrankSWIM_MPK, MatrixrankSWIM_MPK);
		
		parseMatrix(file_FIT_AUTOinput, FIT_AUTOinput);
		parseMatrix(file_FIT_MPKinput, FIT_MPKinput);
		
		parseMatrix(file_SWIM_AUTOinput, SWIM_AUTOinput);
		parseMatrix(file_SWIM_MPKinput, SWIM_MPKinput);

		parseMatrix(file_timeArrayFIT, timeArrayFIT);
		parseMatrix(file_timeArraySWIM, timeArraySWIM);
	}

	public static void parseMatrix(String pathToMatrixCSV, double[][] MatrixToParse)  //this method make 8 matrixes and it is called at the beginning of simulation by Context Manager 
	{
		CSVReader reader;
		try
		{
			reader = new CSVReader(new FileReader(pathToMatrixCSV));
			String[] row;
			//	        String[] col;

			List<?> content = reader.readAll();

			int j=-1;
			for (Object object : content){ // kolejne obiekty sa wierszami, pierwszy wiersz to clubID 
				j++;
				row = (String[]) object;
				for (int i = 0; i < row.length; i++)  // WATCH @ -7 T IS BECAUSE AT THE END WE HAVE ROWS WITH NEW LOCATIONS // kolejne i-te elementy sa kolumnami w wierszu, pierwsza kolumna to zones
				{
					MatrixToParse[j][i]= Double.parseDouble(row[i]); //tutaj tworzymy Array z macierza odleglosci zone X club (dla srodkow transportu t i sportu s)
					// display CSV values
					//System.out.print(" " + i); // object 
					//	                System.out.print(" " + row[i]); //" Cell Value: " +
				}
				//	                System.out.println("");
			}
			//System.out.print("nr of cols: " +j);
//			String check = Arrays.deepToString(MatrixToParse);
//			System.out.print(check);
			        
		}
		catch (FileNotFoundException e) 
		{
			System.err.println(e.getMessage());
		}
		catch (IOException e) 
		{
			System.err.println(e.getMessage());
		}
		// ADD CLOSE FILE

	} //end of method 
	
}



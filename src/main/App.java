package main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class App {
	
	private static final String DAILY_STEPS_FILE =  "src/resources/dailySteps_merged.csv";
	private static final String CARDIO_FILE =  "src/resources/cardio_train.csv";
	private static final String RISK_CATEGORY_FILE =  "src/resources/risk_category_output.csv";

    public static void main(String[] args) {
       
        try {
        	// Calculate average steps for each member from dailySteps_merged.csv
        	List<String[]> averageSteps = computeAverageSteps();            
            List<Double> riskScores = new ArrayList<>();
            
           	// Output List to hold risk score, percentile, and risk category
           	List<String[]> output = new ArrayList<>();
           	String[] outputHeader = new String[] {"risk_score", "percentile", "risk_category"};
           	output.add(outputHeader);            

           	// List holding cardio_train.csv data and calculate risk score of the members
           	List<String[]> cardioDetails = computeRiskScore(output, averageSteps, riskScores);
           	
           	// Calculate percentile and risk category
	        computePercentileAndRiskCategory(riskScores, output);
	        
	        // Write the final output with the corresponding inputs post cleaning of incorrect values
	        writeFinalOutput(cardioDetails, output);
		} catch (IOException e) {
			e.printStackTrace();
		}                
    }
    
    public static List<String[]> computeAverageSteps() throws IOException {
    	
        List<String[]> averageSteps = new ArrayList<>();
                
    	try (CSVReader reader = new CSVReader(new FileReader(DAILY_STEPS_FILE))) {
            List<String[]> dailyStepsDetails = reader.readAll();
            int stepsTotalCount = 0;
            String previousMemberId = "";
            int daysCount=0;
            
            for(int i=1; i < dailyStepsDetails.size(); i++) {            	
            	String[] recordArray = dailyStepsDetails.get(i);            	            
            	String memberId = recordArray[0];

            	// Set previous iteration member id value with current iteration member id
            	if(i == 1) {
            		previousMemberId = memberId;
            	}

            	// Add daily step & increment days count variable for last iteration
            	int dailyStep = Integer.parseInt(recordArray[2]);
            	if(i == dailyStepsDetails.size()-1)
            	{
            		stepsTotalCount += dailyStep;
            		daysCount++;
            	}
            	
            	// If current iteration member id equals to previous, add daily steps and increment days count variable
                if(memberId.equals(previousMemberId) && i != dailyStepsDetails.size()-1) {                
                	stepsTotalCount += dailyStep;
                	daysCount++;
                } else {
                	// If not, find average steps for the member and initialise daily steps and days count for next member
                	String[] averageStepsRecord = new String[2];
                    averageStepsRecord[0] = previousMemberId;
                    averageStepsRecord[1] = String.valueOf(stepsTotalCount / daysCount);
                	averageSteps.add(averageStepsRecord);
                	stepsTotalCount = dailyStep;
                	daysCount = 1;
                }
                previousMemberId = memberId;
            }
    	}
    	return averageSteps;
    }
    
    public static List<String[]> computeRiskScore(List<String[]> output, 
    		List<String[]> averageSteps, List<Double> riskScores) throws IOException {
    	
    	List<String[]> cardioDetails = new ArrayList<>();
    	try (CSVReader readerCardio = new CSVReader(new FileReader(CARDIO_FILE))) {
            cardioDetails = readerCardio.readAll();
            
            for(int i=1; i < cardioDetails.size(); i++) {             	
                double riskScore=0;                
                String[] recordArray = cardioDetails.get(i);
                String[] outputRecord = new String[3];
          	
                // Handle incorrect data 9days for age and set it as null
                if(i == 16) {
         		    outputRecord[0] = String.valueOf(-1);
         		    output.add(outputRecord);
         		    recordArray[1] = "null";
         		    continue;
                }
          	    
                // Calculate risk score for age
          	  	String ageString = recordArray[1];
          	  	if(!ageString.equals("null")) {
          		    if(ageString.startsWith("-")) {
          			    ageString = ageString.substring(1);
          			    recordArray[1] = ageString;
          		    }
          		    int age = Integer.parseInt(ageString);
          		    riskScore += (float)age / 365 * 10;
          	  	} else {
         		    outputRecord[0] = String.valueOf(-1);
         		    output.add(outputRecord);
         		    continue;
          	  	}
          	 
          	  	// Calculate risk score for BMI
          	  	String heightString = recordArray[3];
          	  	String weightString = recordArray[4];
              
          	  	if(!heightString.equals("null") && !weightString.equals("null")) {
          	  		int height = Integer.parseInt(heightString);
          	  		float weight = Float.parseFloat(weightString);
              
          	  		double bmi = (weight * 2.205 * 703) / ((height / 2.54) * (height / 2.54));
          	  		riskScore += bmi * 10;
          	  	} else {
          	  		outputRecord[0] = String.valueOf(-1);
          	  		output.add(outputRecord);
          	  		continue;
          	  	}
              
          	  	// Calculate risk score for BP
          	  	String apHiString = recordArray[5];
          	  	String apLoString = recordArray[6];
              
          	  	if(!apHiString.equals("null")) {
          	  		int apHi = Integer.parseInt(apHiString);
          	  		int apLo = Integer.parseInt(apLoString);
              
          	  		if (apHi > 120 || apLo > 80) {
          	  			riskScore += 1 * 10;
          	  		}
          	  	} else {
          	  		outputRecord[0] = String.valueOf(-1);
          	  		output.add(outputRecord);
          	  		continue;
          	  	}
                 
          	  	// Calculate risk score for cholesterol
          	  	String cholesterolString = recordArray[7];
          	  	int cholesterol = Integer.parseInt(cholesterolString);
          	  	if (cholesterol != 1) {
          	  		riskScore += (cholesterol - 1) * 10;
          	  	}
          	  	
          	  	// Calculate risk score for glucose
          	  	String glucoseString = recordArray[8];
          	  	int glucose = Integer.parseInt(glucoseString);
          	  	if (glucose != 1) {
          	  		riskScore += (glucose - 1) * 10; 
          	  	}
              
          	  	// Calculate risk score for smoking
          	  	String smokeString = recordArray[9];
          	  	int smoke = Integer.parseInt(smokeString);
          	  	riskScore += smoke * 10;
          
          	  	// Calculate risk score for alcohol consumption
          	  	String alcoholString = recordArray[10];
          	  	int alcohol = Integer.parseInt(alcoholString);
          	  	riskScore += alcohol * 10;
              
          	  	// Calculate risk score for physical activity with average steps or activity flag
          	  	String idString = recordArray[0];
          	  	int memberId = Integer.parseInt(idString);
          	 
          	  	if (memberId > 0 && memberId < 6607) {
          	  		String stepsString = averageSteps.get(memberId-1)[1];
          	  		int steps = Integer.parseInt(stepsString);
          	  		if (steps < 7000) {
          	  			riskScore += 1 * 10;
          	  		}
          	  	} else {
          	  		String activityString = recordArray[11];
          	  		int activity = Integer.parseInt(activityString);
          	  		riskScore += activity * 10;
          	  	}
          	 
          	  	// Calculate risk score for cardio
          	  	String cardioString = recordArray[12];
          	  	int cardio = Integer.parseInt(cardioString);
          	  	riskScore += cardio * 20;
             
          	  	// Calculate risk score for medication inadherence
          	  	String medicationString = recordArray[13];
          	  	if(medicationString.equals("N]")) {
          	  		riskScore += 1 * 10;
          	  	}
          	 
          	  	// Store the riskscore to output variable
          	  	outputRecord[0] = String.valueOf(riskScore);
          	  	output.add(outputRecord);
          	  	riskScores.add(riskScore);          	 
           } 
    	}
    	return cardioDetails;
    }
    
    public static void computePercentileAndRiskCategory(List<Double> riskScores, List<String[]> output) {
    	double[] riskScoresArray = riskScores.stream().mapToDouble(riskScore -> riskScore).toArray();
        Arrays.sort(riskScoresArray);     
        
        for(int i = 1; i < output.size(); i++) {
     	   String[] outputRecord = output.get(i);
     	   double riskScore = Double.parseDouble(outputRecord[0]);
     	   
     	   if(riskScore == -1) {
     		   outputRecord[1] = String.valueOf(riskScore);
     		   outputRecord[2] = "NA";
     	   } else {
     		   float percentile = percentile(riskScore, riskScoresArray, riskScoresArray.length);
     		   outputRecord[1] = String.valueOf(percentile);
     		   outputRecord[2] = findRiskCategory(percentile);
     	   }            	  
        }
    }
    
    public static float percentile(double riskScore, double[] riskScores, int riskScoresSize)
    {
    	// Use binary search in the sorted array to find the count of number of higher risk scores than the member
    	int count = Arrays.binarySearch(riskScores, riskScore);
        return (float)(count * 100) / (riskScoresSize - 1);
    }
    
    public static String findRiskCategory(float percentile) {
    	if (percentile >= 60.0) {
    		return "HIGH";
    	} else if (percentile >= 30.0 && percentile < 60.0) {
    		return "MEDIUM";
    	} else {
    		return "LOW";
    	}
    }
    
    public static void writeFinalOutput(List<String[]> cardioDetails, List<String[]> output) throws IOException {
        File file = new File(RISK_CATEGORY_FILE);
        
        try(FileWriter outputFile = new FileWriter(file)) {         	
            CSVWriter writer = new CSVWriter(outputFile); 
            String[] header = ArrayUtils.addAll(cardioDetails.get(0), output.get(0));
            writer.writeNext(header);
      
            for(int i = 1; i < cardioDetails.size(); i++)
            {
            	writer.writeNext(ArrayUtils.addAll(cardioDetails.get(i), output.get(i)));            
            }            
            writer.close();
        }        
    }
}
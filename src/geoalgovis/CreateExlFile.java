package geoalgovis;


import  java.io.*;
import java.util.Arrays;
import java.util.List;
import  org.apache.poi.hssf.usermodel.HSSFSheet;
import  org.apache.poi.hssf.usermodel.HSSFWorkbook;
import  org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;

public class CreateExlFile{
    public static void main(String[]args) {
        
        String[] options = {"time", "score"}; //time or score
        for (String option: options){
        try {
            String filename = "C:\\Users\\mart_\\Documents\\GitHub\\Project-Algorithms-for-geographic-data\\results\\"+option+".xls" ;
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("FirstSheet"); 
            
            CellStyle style = workbook.createCellStyle(); 
            style.setFillForegroundColor(IndexedColors.GREEN.getIndex());  
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] algorithms = {"pullBackIncreasingRadi", "pullBackCentralFirst", "pushAlgorithm", "centerAreaSpread", "lpPush", "lpCenterPush", "pullBackGreedyDirections", "pullBackDecreasingRadi"};
            
            HSSFRow rowhead = sheet.createRow((short)0);
            rowhead.createCell(0).setCellValue("Instance");
            int j = 1;
            for (String algorithm: algorithms){
                rowhead.createCell(j).setCellValue(algorithm);
                j++;
            }

            BufferedReader reader;
            for (int i = 1; i<=200; i++){
                HSSFRow row = sheet.createRow((short)i);
                row.createCell(0).setCellValue("generatedSample"+i);
                try {
                    reader = new BufferedReader(new FileReader("C:\\Users\\mart_\\Documents\\GitHub\\Project-Algorithms-for-geographic-data\\results\\Large spread & no overlap\\generatedSample"+i+"-Result.txt"));
                    String line = reader.readLine();
                    float minTime = Float.POSITIVE_INFINITY;
                    float minScore = Float.POSITIVE_INFINITY;
                    int indexMinScore = -1;
                    while (line != null) {
                        List<String> lineList = Arrays.asList(line.split(","));
                        String instance = lineList.get(0);
                        String algorithm = lineList.get(1);
                        String time = lineList.get(2);
                        float timeFloat = Float.parseFloat(time);
                        String valid = lineList.get(3);
                        String score = lineList.get(4);
                        float scoreFloat = Float.parseFloat(score);
                        
                        if (valid.equals("true")){
                            
                            
                            // find index algorithm
                            int index = -1;
                            for (int k=0;k<algorithms.length;k++) {
                                if (algorithms[k].equals(algorithm)) {
                                    index = k;
                                    break;
                                }
                            }
                            
                            if (timeFloat < minTime){
                                minTime = timeFloat;
                            }
                            if (scoreFloat < minScore){
                                minScore = scoreFloat;
                                indexMinScore = index+1;
                            }

                            if (option.equals("time")){
                                row.createCell(index+1).setCellValue(time);
                            }
                            if (option.equals("score")) {
                                row.createCell(index+1).setCellValue(score);
                            }
                        }

                        line = reader.readLine();
                    }
                            
                    for (Cell cell : row) {
                        if (cell.getStringCellValue().equals(String.valueOf(minTime)) && option.equals("time")){
                            cell.setCellStyle(style);
                        }
                        if (cell.getStringCellValue().equals(String.valueOf(minScore)) && option.equals("score")){
                            cell.setCellStyle(style);
                        }         
                    }
                    if (option.equals("score")){ 
                        row.getCell(indexMinScore).setCellStyle(style);
                    }
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileOutputStream fileOut = new FileOutputStream(filename);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            System.out.println("Your excel file has been generated!");
            
        
        } catch ( Exception ex ) {
            System.out.println(ex);
        }
    }
    }
}
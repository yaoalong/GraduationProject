package org.lab.mars.onem2m.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class RemoveTest {

    public void removeLinesStartsWith(String file, String startsWith, Boolean keepOrigFile) {
        String line = null;
        BufferedReader rd = null;
        PrintWriter wt = null;
        File tempFile = null;

        try {
            // Open input file
            File inputFile = new File(file);

            if (!inputFile.isFile()) {
                System.out.println("ERROR: " + file + " is not a valid file.");
                return;
            }

            // Create temporary file

            tempFile = new File(file + "_OUTPUT");

            //Read input file and Write to tempFile

            rd = new BufferedReader(new FileReader(inputFile));
            wt = new PrintWriter(new FileWriter(tempFile));
            while ((line = rd.readLine()) != null) {
                if (line.substring(0, startsWith.length()).equals(startsWith)) {
                    wt.println(line);
                    wt.flush();
                }

            }
            rd.close();

            if (!keepOrigFile) {

                inputFile.delete();
                rd.close();
                wt.close();
                if (tempFile.renameTo(new File(file))) {
                    System.out.println("OK");
                } else {
                    System.out.println("NOT OK");
                }
            }

        }

        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        finally {
            if (tempFile != null && tempFile.isFile()) {
                wt.close();
            }
        }

    }
    public static void main(String args[]){
    	RemoveTest removeTest=new RemoveTest();
    	removeTest.removeLinesStartsWith("yaoalong.txt", "123", false);
    }
}
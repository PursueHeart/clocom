import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.List;
import java.util.ArrayList;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Scanner;

public class Output {

    //ArrayList<MatchGroup> matchGroupList = new ArrayList<MatchGroup>();
    int algorithmMode;
    boolean enableRepetitive;
    boolean enableOneMethod;
    int matchMode;
    String outputDir;
    int minNumberStatements;
    boolean debug;
    boolean enablePercentageMatching;

    public Output (int alogrithm, 
            boolean enableRepetitiveIn, 
            boolean enableOneMethodIn,
            int matchModeIn,
            String outputDirIn,
            int minNumberStatementsIn,
            boolean debugIn,
            boolean enablePercentageMatchingIn) {
        enableRepetitive = enableRepetitiveIn;
        enableOneMethod = enableOneMethodIn;
        algorithmMode = alogrithm;
        matchMode = matchModeIn;
        outputDir = outputDirIn;
        minNumberStatements = minNumberStatementsIn;
        debug = debugIn;
        enablePercentageMatching = enablePercentageMatchingIn;
    }

    HashMap<Integer,MatchGroup> matchGroupList = new HashMap<Integer,MatchGroup>();

    // file coverage, start-end line
    // statement hash number, start-end
    // method line coverage, start-end line
    public void addClone(String file1, int lineStart1, int lineEnd1, 
            String file2, int lineStart2, int lineEnd2, int length,
            ArrayList<Statement> statementRaw1, int statementStart1, int statementEnd1,
            ArrayList<Statement> statementRaw2, int statementStart2, int statementEnd2,
            int totalHashValue) {

        if (algorithmMode == 0) {
            // check for hashing error during the group hash process
            // only for non-gapped clones
            boolean status = Analyze.hasHashError(
                    statementRaw1.subList(statementStart1, statementEnd1), 
                    statementRaw2.subList(statementStart2, statementEnd2));
            if (status ==  true) {
                return;
            }

            // check for repetitive statements
            if (enableRepetitive) {
                if (Analyze.isRepetitive(statementRaw1.subList(statementStart1, statementEnd1)) == true) {
                    return;
                }
            }

            // require at least one method call
            if (enableOneMethod) {
                if (Analyze.checkNumMethods(statementRaw1.subList(statementStart1, statementEnd1), 1) == false) {
                    return;
                }
            }

            // check for valid scope
            //if (Analyze.hasValidScope(statementRaw1.subList(statementStart1, statementEnd1)) == false) {
            //    return;
            //}
        } else {
            // require at least one method call
            if (minNumberStatements > 0) {
                if (Analyze.checkNumMethods(statementRaw1.subList(statementStart1, statementEnd1), minNumberStatements) == false) {
                    if (debug) {
                	    System.out.println("Removed from lack of method call");
                        System.out.println(file1);
                        System.out.println(file2);
                        System.out.println();
                    }
                    return;
                }
                if (Analyze.checkNumMethods(statementRaw2.subList(statementStart2, statementEnd2), minNumberStatements) == false) {
                    if (debug) {
					    System.out.println("Removed from lack of method call");
                        System.out.println(file1);
                        System.out.println(file2);
                        System.out.println();
                    }
                    return;
                }

                if (debug) {
                    System.out.println("Satisfied minimum method call threashold: " + minNumberStatements);
                }
            }
            // ensure majority of the file is matched (only for SO snippets)
            boolean isJavaCode = Utilities.checkIsJava(file2);
            if (enablePercentageMatching && isJavaCode == false) {
                int size = -2;
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file2));
                    String line = br.readLine();
                    while (line != null) {
                        line = br.readLine();
                        size = size + 1;
                    }
                    br.close();
                } catch (Exception e) {
                    System.out.println("Error on % matching");
                    System.exit(0);
                }
                int sizeMatched = lineEnd2 - lineStart2 + 1;
                float percentage = ((float)sizeMatched * 100 / (float)size);
                //System.out.println(percentage + "%");
                //System.out.println(file1);
                //System.out.println(file2);
                //System.out.println(lineEnd2 + " "  + lineStart2 + "= " + sizeMatched + " X " + size);
                if (percentage < 60) {
                    if (debug) {
					    System.out.println("Removed from lack of % matching: " + String.valueOf(percentage));
                        System.out.println(file1);
                        System.out.println(file2);
                        System.out.println();
                    }
                    return;
                }
            }
        }

        boolean added = false;
        if (matchMode == 0) {
            // between comparison
            MatchGroup matchGroup = matchGroupList.get(totalHashValue);
            if (matchGroup != null) {
                boolean status1 = matchGroup.checkMatchExist(file1, lineStart1, lineEnd1, 0);
                boolean status2 = matchGroup.checkMatchExist(file2, lineStart2, lineEnd2, 1);
                if (status1 == true && status2 == false) {
                    // add as a clone
                    matchGroup.addMatch(1, file2, lineStart2, lineEnd2, 
                            statementRaw2, statementStart2, statementEnd2, totalHashValue);
                    added = true;
                } else if (status1 == false && status2 == true) {
                    // add as a master
                    matchGroup.addMatch(0, file1, lineStart1, lineEnd1,
                            statementRaw1, statementStart1, statementEnd1, totalHashValue);
                    added = true;
                } else if (status1 == true && status2 == true) {
                    added = true;
                }
            } else {
                // status 1 and 2 are false
                if (added == false) {
                    MatchGroup newGroup = new MatchGroup(length);

                    newGroup.addMatch(0, file1, lineStart1, lineEnd1,
                            statementRaw1, statementStart1, statementEnd1, totalHashValue);
                    newGroup.addMatch(1, file2, lineStart2, lineEnd2,
                            statementRaw2, statementStart2, statementEnd2, totalHashValue);

                    matchGroupList.put(totalHashValue, newGroup);
                }
            }
        } else {
            // full mesh
            MatchGroup matchGroup = matchGroupList.get(totalHashValue);
            if (matchGroup != null) {
                boolean status1 = matchGroup.checkMatchExist(file1, lineStart1, lineEnd1, 2);
                boolean status2 = matchGroup.checkMatchExist(file2, lineStart2, lineEnd2, 2);

                if (status1 == true && status2 == false) {
                    // add as a clone
                    matchGroup.addMatch(1, file2, lineStart2, lineEnd2,
                            statementRaw2, statementStart2, statementEnd2, totalHashValue);
                    added = true; 
                } else if (status1 == false && status2 == true) {
                    // add as a clone
                    matchGroup.addMatch(1, file1, lineStart1, lineEnd1,
                            statementRaw1, statementStart1, statementEnd1, totalHashValue);
                    added = true;
                } else if (status1 == true && status2 == true) {
                    added = true;
                }
            } else {
                // status 1 and 2 are false
                if (added == false) {
                    MatchGroup newGroup = new MatchGroup(length);

                    newGroup.addMatch(0, file1, lineStart1, lineEnd1,
                            statementRaw1, statementStart1, statementEnd1, totalHashValue);
                    newGroup.addMatch(1, file2, lineStart2, lineEnd2,
                            statementRaw2, statementStart2, statementEnd2, totalHashValue);

                    matchGroupList.put(totalHashValue, newGroup);
                }
            }
        }
    }

    public void saveResults(String path) {
        try {
            // Serialize file and write to file
            FileOutputStream fout = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(matchGroupList);
            fout.close();
            oos.close();
        } catch (Exception e) {
            System.out.println("Error while writing results\n" + e.getStackTrace());
            System.exit(0);
        }
    }

    public void loadResults(String path) {
        try {
            // Load serialized file
            FileInputStream fin = new FileInputStream(path);
            ObjectInputStream ois = new ObjectInputStream(fin);
            matchGroupList = (HashMap<Integer,MatchGroup>) ois.readObject();
            fin.close();
            ois.close();
        } catch (Exception e) {
            System.out.println("Error while loading results\n" + e.getStackTrace());
            System.exit(0);
        }
    }

    public void processOutputTerms (FrequencyMap fMap) {

        for (Integer key : matchGroupList.keySet()) {
            MatchGroup thisMatchGroup = matchGroupList.get(key);
            HashSet<String> listTerms = thisMatchGroup.dumpTerms();

            fMap.addInstance(listTerms);
        }

    }

    public void printResults(boolean saveEmpty, 
            int similarityRange, 
            boolean enableSimilarity,
            int matchMode,
            boolean debug,
            ArrayList<String> banListSim) {
        if (debug) {
            System.out.println("Inside printResults()");
        }
        DescriptiveStatistics statsInternalClones = new DescriptiveStatistics();
        DescriptiveStatistics statsExternalClones = new DescriptiveStatistics();
        int sumInternalClones = 0;
        int sumExternalClones = 0;

        int sumExternalClonesComment = 0;

        int numMatchesWithComment = 0;
        int matchIndex = 0;
        int outputIndex = 0;
        int emptyIndex = 0;
        
        PrintWriter writerMasterHasComment = null;
        PrintWriter writerMasterNoComment = null;
        try {
            writerMasterHasComment = new PrintWriter(outputDir + "allComments.txt", "UTF-8");
            writerMasterNoComment = new PrintWriter(outputDir + "noComments.txt", "UTF-8");
           
            System.out.println("\n\n");
            for (Integer key : matchGroupList.keySet()) {
                if (debug) {
                    System.out.println("#####");
                    System.out.println("Printing one result");
                }
                MatchGroup thisMatchGroup = matchGroupList.get(key);
                thisMatchGroup.mapCode2Comment();
                thisMatchGroup.pruneComments(similarityRange, enableSimilarity, debug, banListSim);
                thisMatchGroup.pruneDuplicateComments(debug);

                boolean hasComment = thisMatchGroup.hasComment();
                PrintWriter writerMaster;
                if (hasComment) {
                    writerMaster = writerMasterHasComment;
                } else {
                    writerMaster = writerMasterNoComment;
                }
                // no comment and remove empty is true
                // simply export the ones without comment to a different filename
                try {
                    String outputFileName = "";
                    if (hasComment == false) {
                        System.out.println("No comments");
                        outputFileName = outputDir + "empty-" + Integer.toString(emptyIndex) + ".txt";
                        emptyIndex = emptyIndex + 1;
                    } else {
                        System.out.println("Has comment(s)");
                        outputFileName = outputDir + "full-" + Integer.toString(outputIndex) + ".txt";
                        numMatchesWithComment++;
                        outputIndex = outputIndex + 1;
                    }
                    if (debug) {
                        System.out.println(outputFileName);
                    }
                    PrintWriter writerComment = new PrintWriter(outputFileName, "UTF-8");
                    String groupStr = "Match Group " + matchIndex + " of size " +
                            thisMatchGroup.getMasterSize() + "+" + thisMatchGroup.getCloneSize();
                    System.out.println(groupStr);
                    writerComment.println(groupStr);
                    writerMaster.println(groupStr);
                    thisMatchGroup.printAllMappings(saveEmpty, matchMode, 1, outputDir, writerComment, writerMaster);

                    // make sure similarity terms are already been gathered
                    // only can rank comment if there is a comment
                    if (enableSimilarity && hasComment == true) {
                        thisMatchGroup.printRankedComments(writerComment, writerMaster);
                    }

                    writerComment.close();
                } catch (Exception e) {
                    System.out.println("Error in Output.java case 1");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }

                writerMaster.println();

                matchIndex++;
            }
            System.out.println("#####\n\n\n");
            System.out.println(numMatchesWithComment + " comment groups has a comment");

        } catch (FileNotFoundException e) {
            System.out.println("Error while creating an output file");
        } catch (UnsupportedEncodingException e) {
            System.out.println("UnsupportedEncodingException error in print writer");
        } finally {
            if (writerMasterHasComment != null) {
                writerMasterHasComment.close();
            }
            if (writerMasterNoComment != null) {
                writerMasterNoComment.close();
            }
        }

    }

    public void search (String outputDir) {

        String userInput;
        while (true) {
            System.out.print("Enter a list of terms seperated by space: ");

            Scanner in = new Scanner(System.in);
            userInput = in.nextLine();

            if (userInput.equals("")) {
                continue;
            }

            // break down the terms by camel case
            String[] stringList = userInput.split("\\s");
            HashSet<String> setSplittedString = new HashSet<String>();
            for (String str : stringList) {
                Set<String> splitSet = Utilities.splitCamelCaseSet(str);
                setSplittedString.addAll(splitSet);
            }

            // search for a clone that contains all the terms
            for (Integer key : matchGroupList.keySet()) {
                MatchGroup thisMatchGroup = matchGroupList.get(key);

                thisMatchGroup.findClones(setSplittedString, outputDir);

            }

        }


    }



}




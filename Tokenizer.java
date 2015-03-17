import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.ArrayList;

import java.util.Set;
import java.util.HashSet;

public class Tokenizer {
	
    public boolean inMethod = false;

    List<Integer> currentTokenList = new ArrayList<Integer>();

    String currentMethodName = "";
    Method currentMethodObj;

    int startLine;
    int endLine;

    int minNumLines;

    boolean statementHasMethodInvocation = false;
    boolean methodHasMethodInvocation = false;

    boolean debugStatements;

    ArrayList<Method> methodList = new ArrayList<Method>();
    
    HashSet<String> simpleNameList = new HashSet<String>();

    public Tokenizer(int numLines, boolean debug) {
        minNumLines = numLines;
        debugStatements = debug;
    }

    public void insertSimpleName(String name) {
        simpleNameList.add(name);
    }

    public void methodStart(String name, int mStartLine) {
        if (currentMethodName == "") {
            currentMethodName = name;
            currentMethodObj = new Method(name, mStartLine);
            methodHasMethodInvocation = false;

            if (debugStatements == true) {
                System.out.println("\n\n");
            }
        }
    }
    
    public void methodEnd(String name, int mEndLine) {
        // prevent methods within a method
    	if (currentMethodName.equals(name)) {
            currentMethodName = "";

            if (currentMethodObj.getNumStatements() >= minNumLines &&
                    methodHasMethodInvocation == true) {
                //currentMethodObj.buildHash(minNumLines);
                currentMethodObj.setEndLine(mEndLine);
                methodList.add(currentMethodObj);
            } else {
                // clear the statments
                currentTokenList.clear();
            }
        }
    }

    public void statementStart(int sLine, int eLine) {
        startLine = sLine;
        endLine = eLine;
        statementHasMethodInvocation = false;
        simpleNameList = new HashSet<String>();
    }

    public void statementEnd(int scopeLevel) {
        if (!currentTokenList.isEmpty()) {
            int hash_value = hashLine(currentTokenList);
            currentTokenList.clear();

            if (inMethod == true) {
                currentMethodObj.addStatement(hash_value, startLine, endLine,
                        statementHasMethodInvocation, scopeLevel, simpleNameList);

                // debug
                if (debugStatements == true) {
                    System.out.println("added");
                    System.out.print("terms : ");
                    for (String simpleName : simpleNameList) {
                        System.out.print(simpleName + " ");
                    }
                    System.out.println("");
                }
            }
        }
    }
    
    public void getHash(int nodeType, String str) {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((str == null) ? 0 : str.hashCode());
        result = prime * result + nodeType;
        currentTokenList.add(result);
    }
    
    public int hashLine(List<Integer> statementTokens) {
    	
		    final int prime = 31;
		    int result = 1;   	
    	
		    for (Integer tkn : statementTokens) {
    		    result = prime * result + tkn;
    	  }
		
        if (debugStatements == true) {
            System.out.printf("\t>> Hashed statement: %d\n", result);
        }

		    return result;
    }

    public void hasMethodInvocation() {
        statementHasMethodInvocation = true;
        methodHasMethodInvocation = true;
    }

    // Add node name
    public void addHash(int type, int lineNum) {
        getHash(type, null);
        if (debugStatements == true) {
            debug(type, null, lineNum);
        }
    }

    // Add code element
    public void addHash(int type, String str, int lineNum) {        
        getHash(type, str);
        if (debugStatements == true) {
            debug(type, str, lineNum);
        }
    }

    private void debug(int type, String str, int lineNum) {
    
    	if (str == null) {
    		System.out.printf("	>> \"%s\" at line \"%d\"\n", TokenType.values()[type].toString(), lineNum);
    	}
    	else {
            str = str.replaceAll("\\n","");
    		System.out.printf("	>> \"%s\" with value \"%s\" at line \"%d\"\n", TokenType.values()[type].toString(), str, lineNum);
    	}
    	
    }

    public ArrayList<Method> getTokenizedMethods() {
        return methodList;   
    }
}


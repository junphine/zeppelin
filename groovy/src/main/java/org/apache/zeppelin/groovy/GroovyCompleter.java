package org.apache.zeppelin.groovy;

/*
 * This source file is based on code taken from SQLLine 1.0.2 See SQLLine notice in LICENSE
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.GroovyShell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import jline.console.completer.ArgumentCompleter.ArgumentList;
import jline.console.completer.ArgumentCompleter.WhitespaceArgumentDelimiter;

import org.apache.zeppelin.completer.CachedCompleter;
import org.apache.zeppelin.completer.CompletionType;
import org.apache.zeppelin.completer.StringsCompleter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;

/**
 * auto complete functionality for the JdbcInterpreter.
 */
public class GroovyCompleter {
  private static Logger logger = LoggerFactory.getLogger(GroovyCompleter.class);
  
  GroovyShell shell = null;

 

  /**
   * Schema completer.
   */
  private CachedCompleter schemasCompleter;

  /**
   * Contain different completer with table list for every schema name.
   */
  private Map<String, CachedCompleter> tablesCompleters = new HashMap<>();

  /**
   * Contains different completer with column list for every table name
   * Table names store as schema_name.table_name.
   */
  private Map<String, CachedCompleter> columnsCompleters = new HashMap<>();

  /**
   * Completer for groovy keywords.
   */
  private CachedCompleter keywordCompleter;

  private int ttlInSeconds;

  public GroovyCompleter(GroovyShell shell,int ttlInSeconds) {
    this.ttlInSeconds = ttlInSeconds;
    this.shell = shell;
  }
  
 
  public int completion(String buf, int cursor, List<InterpreterCompletion> results)  {
    
    if (buf.length() < cursor) {
      cursor = buf.length();
    }
    String completionString = getCompletionTargetString(buf, cursor);
    String completionCommand = "__zeppelin_completion__.getCompletion('" + completionString + "')";
    logger.debug("completionCommand: " + completionCommand);

    List<String> completionList = new LinkedList<>();
    if(completionString.startsWith("import") || completionString.startsWith("new")) {
    	Class[] loadedClass = this.shell.getClassLoader().getLoadedClasses();
    	for(Class cls: loadedClass) {
    		completionList.add(cls.getName());
    		if(completionList.size()>10) {
    			break;
    		}
    	}
    }
    else {
    	Object o = this.shell.getVariable(completionString);
    	if(o!=null) {
    		for(Method m : o.getClass().getMethods()) {
    			completionList.add(m.getName());
        		if(completionList.size()>10) {
        			break;
        		}
    		}
    	}
    }
    //end code for completion
    if (completionList == null || completionList.size()==0) {
      return 0;
    }
    for (String name : completionList) {
      results.add(new InterpreterCompletion(name, name, StringUtils.EMPTY));
    }
    return results.size();
  }

  private String getCompletionTargetString(String text, int cursor) {
    String[] completionSeqCharaters = {" ", "\n", "\t", "\r"};
    int completionEndPosition = cursor;
    int completionStartPosition = cursor;
    int indexOfReverseSeqPostion = cursor;

    String resultCompletionText = "";
    String completionScriptText = "";
    try {
      completionScriptText = text.substring(0, cursor);
    } catch (Exception e) {
      logger.error(e.toString());
      return null;
    }
    completionEndPosition = completionScriptText.length();

    String tempReverseCompletionText = new StringBuilder(completionScriptText).reverse().toString();

    for (String seqCharacter : completionSeqCharaters) {
      indexOfReverseSeqPostion = tempReverseCompletionText.indexOf(seqCharacter);

      if (indexOfReverseSeqPostion < completionStartPosition && indexOfReverseSeqPostion > 0) {
        completionStartPosition = indexOfReverseSeqPostion;
      }

    }

    if (completionStartPosition == completionEndPosition) {
      completionStartPosition = 0;
    } else {
      completionStartPosition = completionEndPosition - completionStartPosition;
    }
    resultCompletionText = completionScriptText.substring(
        completionStartPosition, completionEndPosition);

    resultCompletionText = resultCompletionText.split("\\.")[0];
    return resultCompletionText;
  }

  public void initKeywords(Set<String> keywords) {
    if (keywords != null && !keywords.isEmpty()) {
      keywordCompleter = new CachedCompleter(new StringsCompleter(keywords), 0);
    }
  }


  /**
   * Complete buffer in case it is a keyword.
   *
   * @return -1 in case of no candidates found, 0 otherwise
   */
  private int completeKeyword(String buffer, int cursor, List<CharSequence> candidates) {
    return keywordCompleter.getCompleter().complete(buffer, cursor, candidates);
  }


  private void addCompletions(List<InterpreterCompletion> interpreterCompletions,
      List<CharSequence> candidates, String meta) {
    for (CharSequence candidate : candidates) {
      interpreterCompletions.add(new InterpreterCompletion(candidate.toString(),
          candidate.toString(), meta));
    }
  }

}

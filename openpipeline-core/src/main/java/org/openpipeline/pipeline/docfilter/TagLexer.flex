/*******************************************************************************
 * Copyright 2009 Dieselpoint, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


/*
 * File: TagLexer.flex
 *
 * Parses tags in the form: <tag name=value name=value>
 */

package org.openpipeline.pipeline.docfilter;

import java.io.*;
import java.util.*;

@SuppressWarnings({"fallthrough", "unused"})

%%

/* These are options for generating the lexer */
%class TagLexer
%unicode
%public
%int
%final

/* This is code that appears in the generated output. */

%{
    public static final int EOF = 1;
    public static final int IDENTIFIER = 2; 
    public static final int QUOTEDSTRING = 3;
    public static final int TOKEN = 4;

    public static final String [] resultAsString = {
        "",
        "eof",
        "identifier",
        "quotedstring",
        "token"
    };

    /**
     * Parse the input tag into name/value pairs and add
     * to the HashMap.
     *
     */    
    public void parse(HashMap hashMap) throws IOException {
        String id = null;
        while (true) {
            int lexStatus = yylex();
            if (lexStatus == YYEOF)
                break;
            
            switch(lexStatus) {
            case IDENTIFIER:
                // strip the equals at the end
                id = new String(zzBuffer, zzStartRead, yylength() - 1);
                id = id.toLowerCase();
                break;
            case TOKEN:
                if (id != null) {
                    hashMap.put(id, yytext());
                    id = null;
                }
                break;
            case QUOTEDSTRING:
                if (id != null) {
                    // strip the quotes
                    if (yylength() > 2) {
                        String quotstr = new String(zzBuffer, zzStartRead + 1, yylength() - 2);
                        hashMap.put(id, quotstr);
                        id = null;
                    }
                
                }
            }    
        }
    }

    // this exists solely for the purpose of suppressing warnings in Eclipse
    // over unused private variables and methods
    protected void dummy() throws IOException {
    	if ((yyline + yychar + yycolumn) == 0 || zzAtBOL) {}
		yyclose();
    	yystate();
    	yytext();
    	yycharat(0);
    	yypushback(0);
    	yybegin(0);
    	yyreset(null);
    }
 
%}

letter=[:letter:]
digit=[:digit:]
id_char={letter}|{digit}|"_"|"-"
space=" "
identifier={id_char}+{space}*"="
quotedstring="\""~"\""
singlequotedstring="'"~"'"
/* match everything but <, >, newline, space, or = */
token=[^"<"">""\r""\n""\u2028""\u2029""\u000B""\u000C""\u0085"" ""="]+

/*
punct1={c1}|{c2}|{c3}|{c4}|{c5}|{c6}|{c7}|{c8}|{c9}|{c10}
punct2={c11}|{c12}|{c13}|{c14}|{c15}|{c16}|{c17}|{c18}|{c19}|{c20}
punct3={c21}|{c22}|{c23}|{c24}|{c25}|{c26}|{c27}|{c28}
token=({letter}|{digit}|{punct1}|{punct2}|{punct3})+

c1="~"
c2="`"
c3="!"
c4="@"
c5="#"
c6="$"
c7="%"
c8="^"
c9="&"
c10="*"
c11="("
c12=")"
c13="_"
c14="-"
c15="+"
c16="|"
c17="\\"
c18="{"
c19="}"
c20="["
c21="]"
c22=":"
c23=";"
c24="'"
c25=","
c26="."
c27="/"
c28="?"
*/

%%

{identifier}                { return IDENTIFIER; }
{quotedstring}              { return QUOTEDSTRING; }
{singlequotedstring}        { return QUOTEDSTRING; }
{token}                     { return TOKEN; }

// the following matches any single char. It's a catch-all.
([^])                       { /* nothing */ }





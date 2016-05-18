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
 * File: HtmlLexer.flex
 *
 * Processes an HTML file. Recognizes tags, returns text.
 *
 * Recognized constructs:
 *
 *  <!-- ... -->                returned as COMMENT
 *  <tag ... >                  returned as STARTTAG
 *  </tag ...>                  returned as ENDTAG
 *  <title ...></title ...>     returned as STARTTITLE, ENDTITLE
 *  <head ...>                  returned as STARTHEAD, ENDHEAD
 *  <a ...>                     returned as STARTANCHOR, ENDANCHOR
 *  <noindex on><noindex off>   returned as STARTNOINDEX, ENDNOINDEX
 *  <noindex></noindex>         returned as STARTNOINDEX, ENDNOINDEX
 *  <meta ...>                  (same pattern)
 *  <script ...>
 *  <style ... >
 *  <!doctype ...>
 *  <img ...>
 *  <base href="...">           returned as BASE
 *  &#123;                      returned as DECIMAL_SYMBOL
 *  &#xabc;                     returned as HEX_SYMBOL
 *  &nbsp;                      returned as SYMBOL
 *
 *  Everything else             returned as TEXT
 */

package org.openpipeline.html;

import java.io.*;
import org.openpipeline.util.FastStringBuffer;

@SuppressWarnings("unused")

%%
/* These are options for generating the lexer */
%class HTMLLexer
%unicode
%public
%int
%final
%apiprivate
%char


/* This is code that appears in the generated output. */

%{
    public static final int EOF = -1;
    public static final int TEXT = 2; 
    public static final int CHAR = 3;

    public static final int COMMENT = 4;
    public static final int STARTTITLE = 5;
    public static final int ENDTITLE = 6;
    public static final int STARTHEAD = 7;
    public static final int ENDHEAD = 8;
    public static final int STARTTAG = 9;
    public static final int ENDTAG = 10;
    public static final int SCRIPT = 11;
    public static final int META = 12;
    public static final int STYLE = 13;
    public static final int DOCTYPE = 14;
    public static final int IMG = 15;
    public static final int NEWLINE = 16;
    public static final int WHITESPACE = 17;
    public static final int STARTANCHOR = 18;
    public static final int ENDANCHOR = 19;
    public static final int FRAME = 20;
    public static final int NBSP = 21;
    public static final int AMP = 22;
    public static final int GT = 23;
    public static final int LT = 24;
    public static final int QUOT = 25;
    public static final int APOS = 26;
    
    public static final int SYMBOL_DECIMAL = 27;
    public static final int SYMBOL_HEX = 28;
    //public static final int SYMBOL_CHAR = 29;
    
    public static final int BASE = 30;
    public static final int STARTNOINDEX = 31;
    public static final int ENDNOINDEX = 32;


    public static final String [] resultAsString = {
        "",
        "eof",
        "text",
        "char",
        "comment",
        "starttitle",
        "endtitle",
        "starthead",
        "endhead",
        "starttag",
        "endtag",
        "script",
        "meta",
        "style",
        "doctype",
        "img",
        "newline",
        "whitespace",
        "startanchor",
        "endanchor",
        "frame",
        "nbsp",
        "amp",
        "gt",
        "lt",
        "quot",
        "apos",
        "symbol_decimal",
        "symbol_hex",
        "symbol_char",
        "base",
        "startnoindex",
        "endnoindex"
    };

	public HTMLLexer() {}

    public int lex(FastStringBuffer matchedText)
      throws IOException {
      matchedText.clear();
      int lexStatus = yylex();
      if (lexStatus != YYEOF) {
        matchedText.append(zzBuffer, zzStartRead, yylength());
        return lexStatus;
      } else {
        return EOF;
      }
    }
    
    public int lex() throws IOException {
    	int lexStatus = yylex();
    	if (lexStatus == YYEOF) {
    		return EOF;
    	}
    	return lexStatus;
    }
    
	public int getText(char [] dest, int destOffset) {
		int count = yylength();
		System.arraycopy(zzBuffer, zzStartRead, dest, destOffset, count);
		return count;
	}
	
	public String getText() {
		return new String(zzBuffer, zzStartRead, yylength());
	}
	
	public void getText(FastStringBuffer matchedText) {
		matchedText.clear();
		matchedText.append(zzBuffer, zzStartRead, yylength());
	}

	public char getChar() {
		return zzBuffer[zzStartRead];
	}
 
	public void reset(java.io.Reader reader) {
		yyreset(reader);
	}
	
	public int getOffset() {
		return yychar;
	}
	
	public int getSize() {
		return yylength();
	}
 
%}


/* These are macro definitions. */
/* Don't add spaces before or after the =. There's a JFlex bug.*/

/* text matches anything but brackets, newline, space, tab, or ampersand */
text=[^"<"">""\r""\n""\u2028""\u2029""\u000B""\u000C""\u0085"" ""\t""&"]+

/* this matches characters that are allowed to make up a tag name*/
tagchar=([:letter:]|[:digit:]|"-"|"_"|"?")

newline=\r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085
whitespace=(" "|"\t"|{newline})+

comment="<!"~">"  /* also handles "<!--"~"-->"*/

/* this method matches a tag when the tag exists without
anything else <tag>, or when the tag is followed by 
whitespace and then other stuff <tag stuff>. It won't
match <tagxxx>, where "tag" is just a prefix*/

title=[Tt][Ii][Tt][Ll][Ee]
starttitle=("<"{title}">")|("<"{title}{whitespace}~">")
endtitle=("</"{title}">")|("</"{title}{whitespace}~">")

head=[Hh][Ee][Aa][Dd]
starthead=("<"{head}">")|("<"{head}{whitespace}~">")
endhead=("</"{head}">")|("</"{head}{whitespace}~">")

anchor=[Aa]
startanchor="<"{anchor}{whitespace}~">"
endanchor=("</"{anchor}">")|("</"{anchor}{whitespace}~">")

noindex=[Nn][Oo][Ii][Nn][Dd][Ee][Xx]
startnoindex=("<"{noindex}">")|("<"{noindex}{whitespace}[Oo][Nn]~">")
endnoindex=("</"{noindex}">")|("<"{noindex}{whitespace}[Oo][Ff][Ff]~">")

meta=[Mm][Ee][Tt][Aa]
metatag="<"{meta}{whitespace}~">"

img=[Ii][Mm][Gg]
imgtag="<"{img}{whitespace}~">"

frame=[Ff][Rr][Aa][Mm][Ee]
frametag="<"{frame}{whitespace}~">"

base=[Bb][Aa][Ss][Ee]
basetag="<"{base}{whitespace}~">"

script="<"[Ss][Cc][Rr][Ii][Pp][Tt]~("</"[Ss][Cc][Rr][Ii][Pp][Tt]~">")
style="<"[Ss][Tt][Yy][Ll][Ee]~("</"[Ss][Tt][Yy][Ll][Ee]~">")
doctype="<!"[Dd][Oo][Cc][Tt][Yy][Pp][Ee]~">"

starttag="<"{tagchar}+~">"
endtag="</"{tagchar}+~">"

/* Symbols. Semicolons are optional */
nbsp="&"[Nn][Bb][Ss][Pp]";"?     /* special case common symbols */
amp="&"[Aa][Mm][Pp]";"?
gt="&"[Gg][Tt]";"?
lt="&"[Ll][Tt]";"?
quot="&"[Qq][Uu][Oo][Tt]";"?
apos="&"[Aa][Pp][Oo][Ss]";"?

digits=[:digit:]+
hexdigit=[0-9a-fA-F]
hexdigits={hexdigit}+

symbol_decimal="&#"{digits}";"?
symbol_hex="&#"[Xx]{hexdigits}";"?

/*
symbol_char="&"{letter_without_x}{letters_or_digits}*";"?
letters_or_digits=([:letter:]|[:digit:])+
letter_without_x=[a-wyzA-WYZ]
*/

%%

{nbsp}                      { return NBSP; }
{amp}                       { return AMP; }
{gt}                        { return GT; }
{lt}                        { return LT; }
{quot}                      { return QUOT; }
{apos}                      { return APOS; }
{symbol_decimal}            { return SYMBOL_DECIMAL; }
{symbol_hex}                { return SYMBOL_HEX; }
/* {symbol_char}               { return SYMBOL_CHAR; } */

{doctype}                   { return DOCTYPE; }
{comment}                   { return COMMENT; }
{script}                    { return SCRIPT; }
{style}                     { return STYLE; }
{imgtag}                    { return IMG; }

{starttitle}                { return STARTTITLE; }
{endtitle}                  { return ENDTITLE; }
{starthead}                 { return STARTHEAD; }
{endhead}                   { return ENDHEAD; }
{metatag}                   { return META; }
{startanchor}               { return STARTANCHOR; }
{endanchor}                 { return ENDANCHOR; }
{startnoindex}              { return STARTNOINDEX; }
{endnoindex}                { return ENDNOINDEX; }
{frametag}                  { return FRAME; }
{basetag}                   { return BASE; }

{starttag}                  { return STARTTAG; }
{endtag}                    { return ENDTAG; }

{text}                      { return TEXT; }
{whitespace}                { return WHITESPACE; }

/* 
The section below matches the more unusual symbols.
The reason it's here, instead of using the symbolchar macro above,
is that a doc may contain something like "elephant&castle". Semicolons
are optional, so the &castle might get recognized incorrectly using generic code.
By naming all the allowed entities explicitly that won't happen.
The return value is 100000 + the decimal Unicode number.
Trailing semicolons are optional.
TODO must finish this section 
http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
*/

"&laquo"";"?				{ return 100171; }
"&raquo"";"?				{ return 100187; }

// The following "." matches any character except newline.  It catches
// anything that doesn't match any of the above patterns (there shouldn't
// be a newline).  It must be last, otherwise it could hide other one
// character matches.

(.)                         { return CHAR; }




package org.openpipeline.html;

import java.util.HashMap;
import java.util.StringTokenizer;

public class HTMLUtils {

	private static HashMap symbolTable = new HashMap(); 

	static final String symbols = "nbsp,160,iexcl,161,cent,162,pound,163,curren,164,yen,165,brvbar,166,"
			+ "sect,167,uml,168,copy,169,ordf,170,laquo,171,not,172,shy,173,reg,174,"
			+ "macr,175,deg,176,plusmn,177,sup2,178,sup3,179,acute,180,"
			+ "micro,181,para,182,middot,183,cedil,184,sup1,185,ordm,186,"
			+ "raquo,187,frac14,188,frac12,189,frac34,190,iquest,191,"
			+ "Agrave,192,Aacute,193,Acirc,194,Atilde,195,Auml,196,"
			+ "Aring,197,AElig,198,Ccedil,199,Egrave,200,Eacute,201,"
			+ "Ecirc,202,Euml,203,Igrave,204,Iacute,205,Icirc,206,Iuml,207,"
			+ "ETH,208,Ntilde,209,Ograve,210,Oacute,211,Ocirc,212,"
			+ "Otilde,213,Ouml,214,times,215,Oslash,216,Ugrave,217,"
			+ "Uacute,218,Ucirc,219,Uuml,220,Yacute,221,THORN,222,szlig,223,"
			+ "agrave,224,,aacute,225,acirc,226,atilde,227,auml,228,"
			+ "aring,229,,aelig,230,ccedil,231,egrave,232,eacute,233,"
			+ "ecirc,234,euml,235,igrave,236,iacute,237,icirc,238,iuml,239,"
			+ "eth,240,ntilde,241,ograve,242,oacute,243,ocirc,244,"
			+ "otilde,245,ouml,246,divide,247,oslash,248,ugrave,249,"
			+ "uacute,250,ucirc,251,uuml,252,yacute,253,thorn,254,yuml,255";

	static {
		/**
		 * This populates a static symbol table.
		 */
		StringTokenizer st = new StringTokenizer(symbols, ",");
		while (st.hasMoreTokens()) {
			String name = "&" + st.nextToken();
			String valueString = st.nextToken();
			try {
				int valueNumber = Integer.parseInt(valueString);
				symbolTable.put(name, new Character((char) valueNumber));
			} catch (NumberFormatException ex) {
				throw new Error("HTMLDoc Invalid character symbol value "
						+ valueString);
			}
		}
	}

	/**
	 * Convert "&#123;" to a char using decimal.
	 * 
	 * @param buf
	 *            a buffer containing the symbol
	 * @return the char
	 */
	static public char convertSymbolDecimal(CharSequence buf) {
		int value = 0;
		int len = buf.length();
		for (int i = 2; i < len; i++) { // Leave off leading & # and trailing ;
			char c = buf.charAt(i);
			if (c == ';')
				break;
			value = (value * 10) + (c - '0');
			if (value > 0xffff)
				return ' ';
		}
		return (char) value;
	}

	/**
	 * Convert "&#x123;" to char using hex.
	 * 
	 * @param buf
	 *            a buffer containing the symbol
	 * @return the char
	 */
	static public char convertSymbolHex(CharSequence buf) {
		int value = 0;
		int len = buf.length();

		// Leave off leading & # X and trailing ;
		for (int i = 3; i < len; i++) {
			char c = buf.charAt(i);
			if (c == ';')
				break;
			if ('0' <= c && c <= '9') {
				value = (value * 16) + (c - '0');
			} else {
				int charval;
				if (c < 'G') // uppercase
					charval = 10 + c - 'A';
				else
					charval = 10 + c - 'a';
				value = (value * 16) + charval;
				if (value > 0xffff)
					return ' ';
			}
		}
		return (char) value;
	}



}

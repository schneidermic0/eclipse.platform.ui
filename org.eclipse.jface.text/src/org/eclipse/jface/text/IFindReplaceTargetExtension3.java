/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jface.text;

/**
 * Extension interface for <code>IFindReplaceTarget</code>. Extends the find replace target's
 * findAndSelect method to allow regular expression find/replace.
 * 
 * @since 3.0
 */
public interface IFindReplaceTargetExtension3 {

	/**
	 * Searches for a string starting at the given offset and using the specified search
	 * directives. If a string has been found it is selected and its start offset is 
	 * returned. If regExSearch is <code>true</code> the findString is
	 * interpreted as a regular expression for searching.
	 *
	 * @param offset the offset at which searching starts
	 * @param findString the string which should be found
	 * @param searchForward <code>true</code> searches forward, <code>false</code> backwards
	 * @param caseSensitive <code>true</code> performes a case sensitve search, <code>false</code> an insensitive search
	 * @param wholeWord if <code>true</code> only occurences are reported in which the findString stands as a word by itself.
	 * 				Must not be used in combination with <code>regExSearch</code>. 
	 * @param regExSearch if <code>true</code> findString represents a regular expression
	 * 				Must not be used in combination with <code>wholeWord</code>. 
	 * @return the position of the specified string, or -1 if the string has not been found
	 * @throws PatternSyntaxException if regExSearch is <code>true</code> and findString is an invalid regular expression
	 */
	int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch);

	/**
	 * Replaces the currently selected range of characters with the given text.
	 * If regExReplace is <code>true</code> the text is interpreted as a regular
	 * expression for searching.
	 * This target must be editable. Otherwise nothing happens.
	 * 
	 * @param text the substitution text
	 * @param regExReplace if <code>true</code> text represents a regular expression 
	 * @throws IllegalStateException if a REPLACE or REPLACE_FIND operation is not preceded by a successful FIND operation
	 * @throws PatternSyntaxException if regExReplace is <code>true</code> and text is an invalid regular expression
	 */
	void replaceSelection(String text, boolean regExReplace);
}

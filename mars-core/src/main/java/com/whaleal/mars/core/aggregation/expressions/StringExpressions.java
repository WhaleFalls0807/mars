/**
 *    Copyright 2020-present  Shanghai Jinmu Information Technology Co., Ltd.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the Server Side Public License, version 1,
 *    as published by Shanghai Jinmu Information Technology Co., Ltd.(The name of the development team is Whaleal.)
 *
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    Server Side Public License for more details.
 *
 *    You should have received a copy of the Server Side Public License
 *    along with this program. If not, see
 *    <http://www.whaleal.com/licensing/server-side-public-license>.
 *
 *    As a special exception, the copyright holders give permission to link the
 *    code of portions of this program with the OpenSSL library under certain
 *    conditions as described in each individual source file and distribute
 *    linked combinations including the program with the OpenSSL library. You
 *    must comply with the Server Side Public License in all respects for
 *    all of the code used other than as permitted herein. If you modify file(s)
 *    with this exception, you may extend this exception to your version of the
 *    file(s), but you are not obligated to do so. If you do not wish to do so,
 *    delete this exception statement from your version. If you delete this
 *    exception statement from all source files in the program, then also delete
 *    it in the license file.
 */
package com.whaleal.mars.core.aggregation.expressions;

import com.whaleal.mars.core.aggregation.expressions.impls.*;

import static java.util.Arrays.asList;

/**
 * Defines helper methods for the string expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#string-expression-operators String Expressions
 */
public final class StringExpressions {

    private StringExpressions() {
    }

    /**
     * Concatenates any number of strings.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     *  $concat
     */
    public static Expression concat(Expression first, Expression... additional) {
        return new Expression("$concat", Expressions.toList(first, additional));
    }

    /**
     * Searches a string for an occurence of a substring and returns the UTF-8 byte index of the first occurence. If the substring is not
     * found, returns -1.
     *
     * @param string    the string to search
     * @param substring the target string
     * @return the new expression
     *  $indexOfBytes
     */
    public static IndexExpression indexOfBytes(Expression string, Expression substring) {
        return new IndexExpression("$indexOfBytes", string, substring);
    }

    /**
     * Searches a string for an occurence of a substring and returns the UTF-8 code point index of the first occurence. If the substring is
     * not found, returns -1
     *
     * @param string    the string to search
     * @param substring the target string
     * @return the new expression
     *  $indexOfCP
     */
    public static IndexExpression indexOfCP(Expression string, Expression substring) {
        return new IndexExpression("$indexOfCP", string, substring);
    }

    /**
     * Removes whitespace or the specified characters from the beginning of a string.
     *
     * @param input The string to trim. The argument can be any valid expression that resolves to a string.
     * @return the new expression
     *  $ltrim
     */
    public static TrimExpression ltrim(Expression input) {
        return new TrimExpression("$ltrim", input);
    }

    /**
     * Applies a regular expression (regex) to a string and returns information on the first matched substring.
     *
     * @param input the string to evaluate
     * @return the new expression
     *  $regexFind
     */
    public static RegexExpression regexFind(Expression input) {
        return new RegexExpression("$regexFind", input);
    }

    /**
     * Applies a regular expression (regex) to a string and returns information on the all matched substrings.
     *
     * @param input the string to evaluate
     * @return the new expression
     *  $regexFindAll
     */
    public static RegexExpression regexFindAll(Expression input) {
        return new RegexExpression("$regexFindAll", input);
    }

    /**
     * Applies a regular expression (regex) to a string and returns a boolean that indicates if a match is found or not.
     *
     * @param input the string to process
     * @return the new expression
     *  $regexMatch
     */
    public static RegexExpression regexMatch(Expression input) {
        return new RegexExpression("$regexMatch", input);
    }

    /**
     * Replaces all instances of a search string in an input string with a replacement string.
     *
     * @param input       the input value/source
     * @param find        the search expression
     * @param replacement the replacement value
     * @return the new expression
     *  $replaceAll
     */
    public static Expression replaceAll(Expression input, Expression find, Expression replacement) {
        return new ReplaceExpression("$replaceAll", input, find, replacement);
    }

    /**
     * Replaces the first instance of a search string in an input string with a replacement string.
     *
     * @param input       the input value/source
     * @param find        the search expression
     * @param replacement the replacement value
     * @return the new expression
     *  $replaceOne
     */
    public static Expression replaceOne(Expression input, Expression find, Expression replacement) {
        return new ReplaceExpression("$replaceOne", input, find, replacement);
    }

    /**
     * Removes whitespace or the specified characters from the end of a string.
     *
     * @param input The string to trim. The argument can be any valid expression that resolves to a string.
     * @return the new expression
     *  $rtrim
     */
    public static TrimExpression rtrim(Expression input) {
        return new TrimExpression("$rtrim", input);
    }

    /**
     * Splits a string into substrings based on a delimiter. Returns an array of substrings. If the delimiter is not found within the
     * string,
     * returns an array containing the original string.
     *
     * @param input     The string to split. The argument can be any valid expression that resolves to a string.
     * @param delimiter The delimiter to use when splitting the string expression. delimiter can be any valid expression as long as it
     *                  resolves to a string.
     * @return the new expression
     *  $split
     */
    public static Expression split(Expression input, Expression delimiter) {
        return new Expression("$split", asList(input, delimiter));
    }

    /**
     * Returns the number of UTF-8 encoded bytes in a string.
     *
     * @param input the string to process
     * @return the new expression
     *  $strLenBytes
     */
    public static Expression strLenBytes(Expression input) {
        return new Expression("$strLenBytes", input);
    }

    /**
     * Returns the number of UTF-8 code points in a string.
     *
     * @param input the string to process
     * @return the new expression
     *  $strLenCP
     */
    public static Expression strLenCP(Expression input) {
        return new Expression("$strLenCP", input);
    }

    /**
     * Performs case-insensitive string comparison and returns: 0 if two strings are equivalent, 1 if the first string is greater than the
     * second, and -1 if the first string is less than the second.
     *
     * @param first  the first string to compare
     * @param second the first string to second
     * @return the new expression
     *  $strcasecmp
     */
    public static Expression strcasecmp(Expression first, Expression second) {
        return new Expression("$strcasecmp", asList(first, second));
    }

    /**
     * Deprecated. Use $substrBytes or $substrCP.
     * <p>
     * *note*:  Included for completeness and discoverability.
     *
     * @param input  the string to process
     * @param start  the starting position
     * @param length the number of characters
     * @return the new expression
     *  $substr
     * @deprecated Deprecated since version 3.4: $substr is now an alias for {@link #substrBytes(Expression, int, int)}
     */
    @Deprecated
    @SuppressWarnings("unused")
    public static Expression substr(Expression input, int start, int length) {
        throw new UnsupportedOperationException("Use $substrBytes or $substrCP.");
    }

    /**
     * Returns the substring of a string. Starts with the character at the specified UTF-8 byte index (zero-based) in the string and
     * continues for the specified number of bytes.
     *
     * @param input  the string to process
     * @param start  Indicates the starting point of the substring
     * @param length the byte count to include.  Can not result in an ending index that is in the middle of a UTF-8 character.
     * @return the new expression
     *  $substrBytes
     */
    public static Expression substrBytes(Expression input, int start, int length) {
        return new Expression("$substrBytes", asList(input, start, length));
    }

    /**
     * Returns the substring of a string. Starts with the character at the specified UTF-8 code point (CP) index (zero-based) in the string
     * and continues for the number of code points specified.
     *
     * @param input  the string to process
     * @param start  Indicates the starting point of the substring
     * @param length the code points to include.
     * @return the new expression
     *  $substrCP
     */
    public static Expression substrCP(Expression input, int start, int length) {
        return new Expression("$substrCP", asList(input, start, length));
    }

    /**
     * Converts a string to lowercase. Accepts a single argument expression.
     *
     * @param input the string to process
     * @return the new expression
     *  $toLower
     */
    public static Expression toLower(Expression input) {
        return new Expression("$toLower", input);
    }

    /**
     * Converts value to a string.
     *
     * @param input the value to process
     * @return the new expression
     *  $toString
     */
    public static Expression toString(Expression input) {
        return new Expression("$toString", input);
    }

    /**
     * Converts a string to uppercase. Accepts a single argument expression.
     *
     * @param input the string to process
     * @return the new expression
     *  $toUpper
     */
    public static Expression toUpper(Expression input) {
        return new Expression("$toUpper", input);
    }

    /**
     * Removes whitespace or the specified characters from the beginning and end of a string.
     *
     * @param input the string to process
     * @return the new expression
     *  $trim
     */
    public static TrimExpression trim(Expression input) {
        return new TrimExpression("$trim", input);
    }

}

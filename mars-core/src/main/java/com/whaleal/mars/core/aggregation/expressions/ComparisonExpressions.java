package com.whaleal.mars.core.aggregation.expressions;


import com.whaleal.icefrog.core.collection.ListUtil;
import com.whaleal.mars.core.aggregation.expressions.impls.Expression;
import com.whaleal.mars.core.aggregation.expressions.impls.ExpressionList;

/**
 * Defines helper methods for the comparison expressions
 */
public final class ComparisonExpressions {
    private ComparisonExpressions() {
    }

    /**
     * Returns 0 if the two values are equivalent, 1 if the first value is greater than the second, and -1 if the first value is less than
     * the second.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $cmp
     */
    public static Expression cmp(Expression first, Expression second) {
        return new Expression("$cmp", ListUtil.of(first, second));
    }

    /**
     * Returns true if the values are equivalent.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $eq
     */
    public static Expression eq(Expression first, Expression second) {
        return new Expression("$eq", ListUtil.of(first, second));
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is greater than the second value.
     * <li>false when the first value is less than or equivalent to the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $gt
     */
    public static Expression gt(Expression first, Expression second) {
        return new Expression("$gt", new ExpressionList(first, second));
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is greater than or equivalent to the second value.
     * <li>false when the first value is less than the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $gte
     */
    public static Expression gte(Expression first, Expression second) {
        return new Expression("$gte", ListUtil.of(first, second));
    }

    /**
     * Returns true if the first value is less than the second.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $lt
     */
    public static Expression lt(Expression first, Expression second) {
        return new Expression("$lt", ListUtil.of(first, second));
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is less than or equivalent to the second value.
     * <li>false when the first value is greater than the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $lte
     */
    public static Expression lte(Expression first, Expression second) {
        return new Expression("$lte", ListUtil.of(first, second));
    }

    /**
     * Returns true if the values are not equivalent.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $ne
     */
    public static Expression ne(Expression first, Expression second) {
        return new Expression("$ne", ListUtil.of(first, second));
    }

}

package com.whaleal.mars.core.aggregation.stages;

import com.mongodb.lang.Nullable;
import com.whaleal.mars.core.aggregation.AggregationException;
import com.whaleal.mars.core.aggregation.expressions.Expressions;
import com.whaleal.mars.core.aggregation.expressions.impls.DocumentExpression;
import com.whaleal.mars.core.aggregation.expressions.impls.Expression;


/**
 * Replaces the input document with the specified document. The operation replaces all existing fields in the input document, including
 * the _id field.
 *
 * @aggregation.expression $replaceRoot
 */
public class ReplaceRoot extends Stage {
    private Expression value;
    private DocumentExpression document;

    protected ReplaceRoot(Expression expression) {
        this();
        this.value = expression;
    }

    protected ReplaceRoot() {
        super("$replaceRoot");
    }

    /**
     * Creates a new stage
     *
     * @return the new stage
     */
    public static ReplaceRoot replaceRoot() {
        return new ReplaceRoot();
    }

    /**
     * Creates a new stage to replace the root with the given expression.  This expression must evaluate to a document.  No further
     * fields can be added to this stage.
     *
     * @param expression the document expression
     * @return the new stage
     */
    public static ReplaceRoot replaceRoot(Expression expression) {
        return new ReplaceRoot(expression);
    }

    /**
     * Creates a new stage
     *
     * @return the new stage
     * @deprecated use {@link #replaceRoot()}
     */
    @Deprecated()
    public static ReplaceRoot with() {
        return new ReplaceRoot();
    }

    /**
     * Creates a new stage to replace the root with the given expression.  This expression must evaluate to a document.  No further
     * fields can be added to this stage.
     *
     * @param expression the document expression
     * @return the new stage
     * @deprecated use {@link #replaceRoot(Expression)}
     */
    @Deprecated()
    public static ReplaceRoot with(Expression expression) {
        return new ReplaceRoot(expression);
    }

    /**
     * Adds a new field
     *
     * @param name       the field name
     * @param expression the value expression
     * @return this
     */
    public ReplaceRoot field(String name, Expression expression) {
        if (value != null) {
            throw new AggregationException("mixedModesNotAllowed "+stageName());
        }
        if (document == null) {
            document = Expressions.of();
        }
        document.field(name, expression);

        return this;
    }

    /**
     * @return the expression
     */
    @Nullable
    public DocumentExpression getDocument() {
        return document;
    }

    /**
     * @return the expression
     */
    @Nullable
    public Expression getValue() {
        return value;
    }
}

package com.whaleal.mars.core.aggregation.stages;

import com.mongodb.lang.Nullable;
import com.whaleal.mars.core.aggregation.AggregationException;
import com.whaleal.mars.core.aggregation.expressions.Expressions;
import com.whaleal.mars.core.aggregation.expressions.impls.DocumentExpression;
import com.whaleal.mars.core.aggregation.expressions.impls.Expression;


/**
 * Replaces the input document with the specified document. The operation replaces all existing fields in the input document, including
 * the _id field. With $replaceWith, you can promote an embedded document to the top-level. You can also specify a new document as the
 * replacement.
 * <p>
 * The $replaceWith is an alias for $replaceRoot.
 *
 * @aggregation.expression $replaceWith
 */
public class ReplaceWith extends Stage {
    private Expression value;
    private DocumentExpression document;

    protected ReplaceWith(Expression expression) {
        this();
        this.value = expression;
    }

    protected ReplaceWith() {
        super("$replaceWith");
    }

    /**
     * Creates a new stage
     *
     * @return the new stage
     */
    public static ReplaceWith replaceWith() {
        return new ReplaceWith();
    }

    /**
     * Creates a new stage to replace the root with the given expression.  This expression must evaluate to a document.  No further
     * fields can be added to this stage.
     *
     * @param expression the document expression
     * @return the new stage
     */
    public static ReplaceWith replaceWith(Expression expression) {
        return new ReplaceWith(expression);
    }

    /**
     * Creates a new stage
     *
     * @return the new stage
     * @deprecated use {@link #replaceWith()}
     */
    @Deprecated()
    public static ReplaceWith with() {
        return new ReplaceWith();
    }

    /**
     * Creates a new stage to replace the root with the given expression.  This expression must evaluate to a document.  No further
     * fields can be added to this stage.
     *
     * @param expression the document expression
     * @return the new stage
     * @deprecated use {@link #replaceWith(Expression)}
     */
    @Deprecated()
    public static ReplaceWith with(Expression expression) {
        return new ReplaceWith(expression);
    }

    /**
     * Adds a new field
     *
     * @param name       the field name
     * @param expression the value expression
     * @return this
     */
    public ReplaceWith field(String name, Expression expression) {
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

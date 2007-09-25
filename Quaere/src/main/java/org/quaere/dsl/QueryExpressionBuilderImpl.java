package org.quaere.dsl;

import org.quaere.IncompleteQueryException;
import org.quaere.Queryable;
import org.quaere.QueryableIterable;
import org.quaere.expressions.*;
import org.quaere.quaere4objects.Quaere4ObjectsQueryEngine;

import java.util.*;

public class QueryExpressionBuilderImpl<R> implements
        Iterable<R>,
        QueryExpressionBuilder<R>,
        FromClauseBuilder<R>,
        LetClauseBuilder<R>,
        GroupClauseBuilder<R>,
        QueryBodyBuilder<R>,
        QueryContinuationBuider<R>,
        QueryContinuationOrQueryBodyBuilder<R> {
    private QueryExpressionBuilderImpl<R> parentQueryBuilder;
    private Identifier currentIdentifier;
    private Identifier currentGroupIdentifier;
    private Identifier continuationIdentifier;
    Map<Identifier, Queryable> sources = new HashMap<Identifier, Queryable>();
    private FromClause fromClause;
    List<QueryBodyClause> queryBodyClauses = new ArrayList<QueryBodyClause>();
    private SelectOrGroupClause currentSelectOrGroupClause;
    private QueryContinuation queryContinuation;
    private List<OrderByCriteria> currentOrderByCriterias = new ArrayList<OrderByCriteria>();

    public QueryExpressionBuilderImpl() {
    }
    private QueryExpressionBuilderImpl(QueryExpressionBuilderImpl<R> parentQueryBuilder) {
        this.parentQueryBuilder = parentQueryBuilder;
    }

    private void addOrderByCriteria(Expression expression, OrderByCriteria.Direction direction, Comparator comparator) {
        currentOrderByCriterias.add(new OrderByCriteria(expression, direction, comparator));
    }

    private void terminateOrderingClauses() {
        if (currentOrderByCriterias.size() > 0) {
            queryBodyClauses.add(new OrderByClause(currentOrderByCriterias));
            currentOrderByCriterias = new ArrayList<OrderByCriteria>();
        }
    }
    public Iterator<R> iterator() {
        if (parentQueryBuilder != null) {
            QueryBody queryBody = new QueryBody(queryBodyClauses, currentSelectOrGroupClause, queryContinuation);
            return parentQueryBuilder.iterator(queryBody);
        } else {
            if (currentSelectOrGroupClause == null) {
                throw new IncompleteQueryException("A query must have a select or group by clause.");
            }
            QueryBody queryBody = new QueryBody(queryBodyClauses, currentSelectOrGroupClause, queryContinuation);
            QueryExpression queryExpression = new QueryExpression(fromClause, queryBody);
            Quaere4ObjectsQueryEngine q = new Quaere4ObjectsQueryEngine();
            for (Map.Entry<Identifier, Queryable> sourceEntry : sources.entrySet()) {
                q.addSource(sourceEntry.getValue().getSourceIdentifier(sourceEntry.getKey()), sourceEntry.getValue());
            }
            Iterable<R> result = q.evaluate(queryExpression);
            return result.iterator();
        }
    }
    private Iterator<R> iterator(QueryBody continuationQueryBody) {
        this.queryContinuation = new QueryContinuation(
                continuationIdentifier,
                continuationQueryBody
        );
        return iterator();
    }
    public FromClauseBuilder<R> from(String identifier) {
        currentIdentifier = new Identifier(identifier);
        return this;
    }
    public FromClauseBuilder<R> from(QueryExpressionBuilder subquery) {
        throw new RuntimeException("The method is not implemented");
    }
    public LetClauseBuilder<R> let(String identifier) {
        throw new RuntimeException("The method is not implemented");
    }
    public QueryBodyBuilder<R> where(String predicate) {
        return where(LiteralExpression.parse(predicate));
    }
    public QueryBodyBuilder<R> where(Expression predicate) {
        queryBodyClauses.add(new WhereClause(predicate));
        return this;
    }
    public JoinClauseBuilder<R> join(String identifer) {
        return new JoinClauseBuilderImpl(this, new Identifier(identifer));
    }
    public QueryBodyBuilder<R> orderBy(String expression) {
        return orderBy(expression, null);
    }
    public QueryBodyBuilder<R> orderBy(String expression, Comparator comparator) {
        return orderBy(LiteralExpression.parse(expression), comparator);
    }
    public QueryBodyBuilder<R> orderBy(Expression expression) {
        return orderBy(expression, null);
    }
    public QueryBodyBuilder<R> orderBy(Expression expression, Comparator comparator) {
        addOrderByCriteria(expression, OrderByCriteria.Direction.ASCENDING, comparator);
        return this;
    }
    public QueryBodyBuilder<R> orderByDescending(String expression) {
        return orderByDescending(expression, null);
    }
    public QueryBodyBuilder<R> orderByDescending(String expression, Comparator comparator) {
        return orderByDescending(LiteralExpression.parse(expression), comparator);
    }
    public QueryBodyBuilder<R> orderByDescending(Expression expression) {
        return orderByDescending(expression, null);
    }
    public QueryBodyBuilder<R> orderByDescending(Expression expression, Comparator comparator) {
        addOrderByCriteria(expression, OrderByCriteria.Direction.DESCENDING, comparator);
        return this;
    }
    public <R> QueryContinuationOrQueryBodyBuilder<R> select(String expression) {
        return select(LiteralExpression.parse(expression));
    }
    public <R> QueryContinuationOrQueryBodyBuilder<R> select(Expression expression) {
        terminateOrderingClauses();
        currentSelectOrGroupClause = new SelectClause(expression);
        return (QueryContinuationOrQueryBodyBuilder<R>) this;
    }
    public GroupClauseBuilder<R> group(String identifier) {
        terminateOrderingClauses();
        currentGroupIdentifier = new Identifier(identifier);
        return this;
    }
    public <T> QueryBodyBuilder<R> in(T[] source) {
        return in(Arrays.asList(source));
    }
    public <T> QueryBodyBuilder<R> in(Iterable<T> source) {
        return in(new QueryableIterable<T>(source));
    }
    public <T> QueryBodyBuilder<R> in(Queryable<T> source) {
        sources.put(currentIdentifier, source);
        return in(
                new Statement(
                        Arrays.<Expression>asList(
                                new Identifier(source.getSourceIdentifier(currentIdentifier).getText())
                        )
                )
        );
    }
    public QueryBodyBuilder<R> in(String expression) {
        return in(LiteralExpression.parse(expression));
    }
    private QueryBodyBuilder<R> in(Expression expression) {
        if (fromClause == null) {
            // This is the intial from clause
            fromClause = new FromClause(currentIdentifier, expression);
        } else {
            queryBodyClauses.add(new FromClause(currentIdentifier, expression));
        }
        return this;
    }
    public QueryContinuationBuider<R> by(String expression) {
        return by(LiteralExpression.parse(expression), null);
    }
    public QueryContinuationBuider<R> by(String expression, Comparator comparator) {
        return by(LiteralExpression.parse(expression), comparator);
    }
    public QueryContinuationBuider<R> by(Expression expression) {
        return by(expression, null);
    }
    public QueryContinuationBuider<R> by(Expression expression, Comparator comparator) {
        currentSelectOrGroupClause = new GroupClause(
                currentGroupIdentifier,
                expression,
                comparator
        );
        currentGroupIdentifier = null;
        return this;
    }
    public QueryContinuationOrQueryBodyBuilder<R> into(String identifier) {
        if (currentSelectOrGroupClause instanceof GroupClause) {
            // Are we creating a source from a group by?
            this.continuationIdentifier = new Identifier(identifier);
            return new QueryExpressionBuilderImpl<R>(this);
        } else {
            // ..or was is a join?
            JoinClause nonGroupedJoin = (JoinClause) queryBodyClauses.get(queryBodyClauses.size() - 1);
            JoinClause groupedJoin = new JoinClause(
                    nonGroupedJoin.getClassName(),
                    nonGroupedJoin.getIdentifier(),
                    nonGroupedJoin.getInIndentifier(),
                    nonGroupedJoin.getOnExpression(),
                    nonGroupedJoin.getEqualsExpression(),
                    new Identifier(identifier)
            );
            queryBodyClauses.remove(nonGroupedJoin);
            queryBodyClauses.add(groupedJoin);
            return this;
        }
    }
}

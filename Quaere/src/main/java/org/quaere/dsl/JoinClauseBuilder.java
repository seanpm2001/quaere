package org.quaere.dsl;

import org.quaere.expressions.Expression;
import org.quaere.Queryable;


public interface JoinClauseBuilder<R> {
    JoinOnClauseBuilder<R> in(String expression);
    //    JoinOnClauseBuilder<R> in(Expression expression);
    <T> JoinOnClauseBuilder<R> in(T[] source);
    <T> JoinOnClauseBuilder<R> in(Iterable<T> source);
    <T> JoinOnClauseBuilder<R> in(Queryable<T> source);
}

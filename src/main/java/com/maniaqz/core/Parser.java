package com.maniaqz.core;

import com.maniaqz.configuration.JsonConfigKey;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.json.JSONObject;

import java.util.List;

public class Parser extends JsonManager {
    /**
     * Parses SQL-String and converts it to JSONObject
     *
     * @param sql SQL-String
     * @return org.json.JSONObject() or return null if conversion failed
     */
    public JSONObject parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            statement.accept(statementVisitor);
            return this.json;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private final StatementVisitorAdapter statementVisitor = new StatementVisitorAdapter() {
        //SELECT
        @Override
        public void visit(Select select) {
            //CRUD
            putToJson(JsonConfigKey.CRUD, JsonConfigKey.SELECT);
            select.getSelectBody().accept(selectVisitorAdapter);
            super.visit(select);
        }

        //INSERT
        @Override
        public void visit(Insert insert) {
            // CRUD
            putToJson(JsonConfigKey.CRUD, JsonConfigKey.INSERT);

            // TABLE
            Table table = insert.getTable();
            if (table != null) {
                putToJson(JsonConfigKey.FROM, table.toString());
                table.accept(fromItemVisitor);
            }

            // COLUMNS
            List<Column> columns = insert.getColumns();
            if (columns != null)
                columns.forEach(column -> column.accept(expressionVisitor));

            // VALUES
            List<Expression> expressions = ((ExpressionList) insert.getItemsList()).getExpressions();
            if (expressions != null)
                expressions.forEach(expression -> putToJson(JsonConfigKey.VALUE, expression.toString()));

            super.visit(insert);
        }


        //UPDATE
        @Override
        public void visit(Update update) {
            // crud
            putToJson(JsonConfigKey.CRUD, JsonConfigKey.UPDATE);

            // columns
            List<Column> columns = update.getColumns();
            if (columns != null)
                columns.forEach(column -> column.accept(expressionVisitor));

            // tables
            List<Table> tables = update.getTables();

            if (tables != null) {
                tables.forEach(table -> putToJson(JsonConfigKey.FROM, table.toString()));
                tables.forEach(table -> table.accept(fromItemVisitor));
            }

            // values
            List<Expression> expressions = update.getExpressions();
            if (expressions != null)
                expressions.forEach(expression -> putToJson(JsonConfigKey.VALUE, expression.toString()));

            // where
            Expression whereExpression = update.getWhere();
            if (whereExpression != null)
                putToJson(JsonConfigKey.WHERE, whereExpression.toString());

            super.visit(update);
        }

        //DELETE
        @Override
        public void visit(Delete delete) {
            // crud
            putToJson(JsonConfigKey.CRUD, JsonConfigKey.DELETE);

            // table
            Table table = delete.getTable();
            if (table != null) {
                putToJson(JsonConfigKey.FROM, table.toString());
                table.accept(fromItemVisitor);
            }

            // where
            Expression whereExpression = delete.getWhere();
            if (whereExpression != null) {
                putToJson(JsonConfigKey.WHERE, whereExpression.toString());
                whereExpression.accept(whereExpressionVisitor);
            }

            super.visit(delete);
        }
    };

    //SELECT
    private final SelectVisitorAdapter selectVisitorAdapter = new SelectVisitorAdapter() {
        @Override
        public void visit(PlainSelect plainSelect) {
            // distinct
            Distinct distinct = plainSelect.getDistinct();
            if (distinct != null)
                putToJson(JsonConfigKey.DISTINCT, "TRUE");

            // column
            List<SelectItem> selectItems = plainSelect.getSelectItems();
            if (selectItems != null)
                selectItems.forEach(selectItem -> putToJson(JsonConfigKey.COLUMN, selectItem.toString()));

            // table
            FromItem fromItem = plainSelect.getFromItem();
            if (fromItem != null) {
                putToJson(JsonConfigKey.FROM, fromItem.toString());
                Alias alias = fromItem.getAlias();
                if (alias != null) {
                    String aliasName = alias.getName();
                    putToJson(JsonConfigKey.FROM_ALIAS, aliasName);

                    // remove table alias from table query
                    fromItem.getAlias().setUseAs(false);
                    fromItem.setAlias(null);
                }
                fromItem.accept(fromItemVisitor);
            }

            // where
            Expression whereExpression = plainSelect.getWhere();
            if (whereExpression != null) {
                putToJson(JsonConfigKey.WHERE, whereExpression.toString());
                whereExpression.accept(whereExpressionVisitor);
            }

            // group by
            GroupByElement groupByElement = plainSelect.getGroupBy();
            if (groupByElement != null)
                groupByElement.accept(groupByVisitor);

            // order by
            List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
            if (orderByElements != null)
                orderByElements.forEach(orderByElement -> orderByElement.accept(orderByVisitor));

            // joins
            List<Join> joins = plainSelect.getJoins();
            if (joins != null) {
                joins.forEach(join -> {
                    putToJson(JsonConfigKey.JOIN, 1, join.toString());
                    Alias joinAlias = join.getRightItem().getAlias();
                    if (joinAlias != null) {
                        String joinAliasName = joinAlias.getName();
                        putToJson(JsonConfigKey.JOIN_ALIAS, 1, joinAliasName);

                        // remove join alias in join query
                        joinAlias.setUseAs(false);
                        join.getRightItem().setAlias(null);
                    }
                });
            }
        }

        //select union
        @Override
        public void visit(SetOperationList setOperationList) {

            List<SelectBody> selectBodies = setOperationList.getSelects();
            List<SetOperation> setOperations = setOperationList.getOperations();

            for (int i = 0; i < selectBodies.size(); ++i) {
                if (i == 0)
                    injectJson(new Parser().parse(selectBodies.get(i).toString()));

                if (i < setOperations.size()) {
                    String setOperationKey = String.format("%s %d", setOperations.get(i), i + 1); // ex) UNION 1, UNION ALL 1 ...
                    String setOperationAnalyseKey = String.format("%s %d", setOperations.get(i) + " ANALYSE", i + 1); // ex) UNION ANALYSE 1, UNION ALL ANALYSE 1 ...
                    putToJson(setOperationKey, selectBodies.get(i + 1).toString());
                    putToJson(setOperationAnalyseKey, new Parser().parse(selectBodies.get(i + 1).toString()));
                } else break;
            }
        }
    };

    private final FromItemVisitorAdapter fromItemVisitor = new FromItemVisitorAdapter() {
        //sub query in from
        @Override
        public void visit(SubSelect subSelect) {
            putToJson(JsonConfigKey.FROM_SUB_QUERY, 1, subSelect.toString());
            putToJson(JsonConfigKey.FROM_SUB_QUERY_ANALYSE, 1, new Parser().parse(subSelect.toString()));
            super.visit(subSelect);
        }
    };

    private final ExpressionVisitorAdapter expressionVisitor = new ExpressionVisitorAdapter() {
        //sub query in where
        @Override
        public void visit(SubSelect subSelect) {
            putToJson(JsonConfigKey.WHERE_SUB_QUERY, 1, subSelect.toString());
            putToJson(JsonConfigKey.WHERE_SUB_QUERY_ANALYSE, 1, new Parser().parse(subSelect.toString()));
            super.visit(subSelect);
        }

        //column for select, set
        @Override
        public void visit(Column column) {
            putToJson(JsonConfigKey.COLUMN, column.toString());
            super.visit(column);
        }
    };

    //WHERE
    private final ExpressionVisitorAdapter whereExpressionVisitor = new ExpressionVisitorAdapter() {
        @Override
        public void visit(SubSelect subSelect) {
            putToJson(JsonConfigKey.WHERE_SUB_QUERY, 1, subSelect.toString());
            putToJson(JsonConfigKey.WHERE_SUB_QUERY_ANALYSE, 1, new Parser().parse(subSelect.toString()));
            super.visit(subSelect);
        }
    };

    //GROUP BY
    private GroupByVisitor groupByVisitor = groupByElement -> {
        List<Expression> groupByExpressions = groupByElement.getGroupByExpressions();
        groupByExpressions.forEach(expression -> {
            putToJson(JsonConfigKey.GROUP_BY, expression.toString());
        });

    };

    //ORDER BY
    private final OrderByVisitor orderByVisitor = new OrderByVisitorAdapter() {
        @Override
        public void visit(OrderByElement orderBy) {
            putToJson(JsonConfigKey.ORDER_BY, orderBy.toString());
            super.visit(orderBy);
        }
    };
}

/*******************************************************************************
 *    Licensed to the Apache Software Foundation (ASF) under one or more 
 *    contributor license agreements.  See the NOTICE file distributed with 
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with 
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *******************************************************************************/
package edu.hku.sdb.parse;

import java.util.ArrayList;
import java.util.List;

import edu.hku.sdb.catalog.DataType;
import edu.hku.sdb.catalog.MetaStore;
import edu.hku.sdb.parse.BinaryPredicate.BinOperator;
import edu.hku.sdb.parse.NormalArithmeticExpr.Operator;

public class SemanticAnalyzer extends BasicSemanticAnalyzer {

  private final MetaStore metaDB;

  public SemanticAnalyzer(MetaStore metaDB) {
    this.metaDB = metaDB;
  }

  /**
   * Perform semantic analyze of a AST tree. A new parse tree is returned.
   * 
   * @return parseNode
   */
  @Override
  public ParseNode analyze(ASTNode tree) throws SemanticException {
    tree = ParseUtils.findRootNotNull(tree);
    return analyzeInternal(tree);
  }

  /**
   * Analyze from the token of the root.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private ParseNode analyzeInternal(ASTNode tree) throws SemanticException {
    ParseNode parseTree = null;

    switch (tree.getType()) {
    case HiveParser.TOK_QUERY:
      parseTree = buildStmt(tree);
    }

    if (parseTree != null)
      parseTree.analyze(metaDB, (ParseNode) null);

    return parseTree;
  }

  /**
   * Construct a query statement.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private StatementBase buildStmt(ASTNode tree) throws SemanticException {
    StatementBase stmt = null;
    InsertStmt insertStmt = null;
    List<TableRef> fromStmt = null;

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      case HiveParser.TOK_FROM:
        fromStmt = buildFromStmt(child);
        continue;
      case HiveParser.TOK_INSERT:
        insertStmt = buildInsertStmt(child);
        continue;
      default:
        throw new SemanticException("Unsupported Query Type!");
      }
    }

    if (insertStmt != null) {
      if (fromStmt != null)
        ((SelectStmt) insertStmt.getQueryStmt()).setTableRefs(fromStmt);

      // It is just a simple selection query.
      if (insertStmt.getTargetTbl() == null)
        stmt = insertStmt.getQueryStmt();
      else
        stmt = insertStmt;
    }

    return stmt;
  }

  /**
   * Construct a insert statement.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private InsertStmt buildInsertStmt(ASTNode tree) throws SemanticException {
    SelectStmt selectStmt = new SelectStmt();
    Expr whereStmt = null;
    List<Expr> groupingExprs = null;
    String targetTbl = null;

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      case HiveParser.TOK_SELECT:
        selectStmt = buildSelectStmt(child);
        continue;
      case HiveParser.TOK_WHERE:
        whereStmt = buildWhereStmt(child);
        continue;
      case HiveParser.TOK_GROUPBY:
        groupingExprs = buildGroupByStmt(child);
        // TODO cannot support "insert into" yet
      case HiveParser.TOK_DESTINATION:
        targetTbl = findDestination(child);
        continue;
      default:
        throw new SemanticException("Unsupported Query Type!");
      }
    }

    InsertStmt insertStmt = new InsertStmt(targetTbl);

    if (selectStmt != null) {
      selectStmt.setWhereClause(whereStmt);
      selectStmt.setGroupingExprs(groupingExprs);
    }

    insertStmt.setQueryStmt(selectStmt);

    return insertStmt;
  }

  /**
   * Get the query statement without the "insert into".
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private QueryStmt buildQueryStmt(ASTNode tree) throws SemanticException {

    StatementBase stmt = buildStmt(tree);

    if (stmt instanceof InsertStmt)
      return ((InsertStmt) stmt).getQueryStmt();
    else
      // WARNING: This cast is dangerous, should guarantee buildStmt return a
      // QueryStmt.
      return (QueryStmt) stmt;
  }

  /**
   * Construct the selection statement.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private SelectStmt buildSelectStmt(ASTNode tree) throws SemanticException {
    SelectStmt selectStmt = new SelectStmt();
    SelectionList selectList = new SelectionList();

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      // TODO Cannot support select * at this moment
      case HiveParser.TOK_SELEXPR:
        selectList.getItemList().add(buildSelectItem(child));
        continue;
      default:
        throw new SemanticException("Unsupported selection element!");
      }
    }

    selectStmt.setSelectList(selectList);

    return selectStmt;
  }

  /**
   * Construct the from statement.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private List<TableRef> buildFromStmt(ASTNode tree) throws SemanticException {
    List<TableRef> tblRefs = new ArrayList<TableRef>();

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      case HiveParser.TOK_JOIN:
      case HiveParser.TOK_LEFTOUTERJOIN:
      case HiveParser.TOK_RIGHTOUTERJOIN:
      case HiveParser.TOK_FULLOUTERJOIN:
      case HiveParser.TOK_CROSSJOIN:
        tblRefs.addAll(buildJoinStmt(child,
            ParseUtils.JOIN_OPERATOR_MAP.get(child.getType())));
        continue;
      case HiveParser.TOK_TABREF:
        tblRefs.add(buildBaseTableRef(child, null));
        continue;
      default:
        throw new SemanticException("Unsupported from element!");
      }
    }

    return tblRefs;
  }

  /**
   * Construct the where statement.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private Expr buildWhereStmt(ASTNode tree) throws SemanticException {
    Expr whereStmt = null;

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      // TODO support compound predicate
      case HiveLexer.LESSTHAN:
      case HiveLexer.GREATERTHAN:
      case HiveLexer.EQUAL:
      case HiveLexer.NOTEQUAL:
      case HiveLexer.LESSTHANOREQUALTO:
      case HiveLexer.GREATERTHANOREQUALTO:
        whereStmt = buildNormalBinPredicate(child,
            ParseUtils.BIN_OPERATOR_MAP.get(child.getType()));
        continue;
      default:
        throw new SemanticException("Unsupported where element!");
      }
    }

    return whereStmt;
  }

  private List<Expr> buildGroupByStmt(ASTNode tree) throws SemanticException {
    List<Expr> groupingExprs = new ArrayList<Expr>();

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      case HiveParser.TOK_TABLE_OR_COL:
        groupingExprs.add(new FieldLiteral("", child.getChild(0).getText(),
            DataType.UNKNOWN));
        continue;
      default:
        throw new SemanticException("Unsupported group by element!");
      }
    }

    return groupingExprs;
  }

  /**
   * Find the insert location.
   * 
   * @param tree
   * @return
   */
  private String findDestination(ASTNode tree) {
    return null;
  }

  /**
   * Construct a selection item.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private SelectionItem buildSelectItem(ASTNode tree) throws SemanticException {
    SelectionItem selectItem = new SelectionItem();

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      Expr expr;
      switch (child.getType()) {
      case HiveLexer.PLUS:
      case HiveLexer.MINUS:
      case HiveLexer.STAR:
      case HiveLexer.DIV:
        expr = buildNormalArithmeticExpr(child,
            ParseUtils.OPERATOR_MAP.get(child.getType()));
        selectItem.setExpr(expr);
        continue;
      case HiveLexer.DOT:
        selectItem.setExpr(buildDotExpr(child));
        continue;
      case HiveParser.TOK_TABLE_OR_COL:
        selectItem.setExpr(new FieldLiteral("", child.getChild(0).getText(),
            DataType.UNKNOWN));
        continue;
      case HiveParser.TOK_FUNCTIONSTAR:
        selectItem
            .setExpr(new FunctionCallStarExpr(child.getChild(0).getText()));
        continue;
      case HiveLexer.Identifier:
        selectItem.setAlias(child.getText());
        continue;
      default:
        throw new SemanticException("Unsupported select element!");
      }
    }

    return selectItem;
  }

  /**
   * Construct a normal arithmetic expression.
   * 
   * @param tree
   * @param op
   * @return
   * @throws SemanticException
   */
  private Expr buildNormalArithmeticExpr(ASTNode tree, Operator op)
      throws SemanticException {
    NormalArithmeticExpr expr = new NormalArithmeticExpr(op);

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      case HiveLexer.PLUS:
      case HiveLexer.MINUS:
      case HiveLexer.STAR:
      case HiveLexer.DIV:
        expr.addChild(buildNormalArithmeticExpr(child,
            ParseUtils.OPERATOR_MAP.get(child.getType())));
        continue;
      case HiveLexer.DOT:
        expr.addChild(buildDotExpr(child));
        continue;
      case HiveParser.TOK_TABLE_OR_COL:
        expr.addChild(new FieldLiteral("", child.getChild(0).getText(),
            DataType.UNKNOWN));
        continue;
      default:
        throw new SemanticException("Unsupported arithmetic element!");
      }
    }

    return expr;
  }

  /**
   * Construct an expression for dot reference.
   * 
   * @param tree
   * @return
   */
  private Expr buildDotExpr(ASTNode tree) {
    // TODO Better to do error checking
    return new FieldLiteral(tree.getChild(0).getChild(0).getText(), tree
        .getChild(1).getText(), DataType.UNKNOWN);
  }

  /**
   * Construct a normal binary predicate.
   * 
   * @param tree
   * @param op
   * @return
   * @throws SemanticException
   */
  private Expr buildNormalBinPredicate(ASTNode tree, BinOperator op)
      throws SemanticException {
    NormalBinPredicate binPred = new NormalBinPredicate(op);

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      Expr expr;
      switch (child.getType()) {
      case HiveLexer.PLUS:
      case HiveLexer.MINUS:
      case HiveLexer.STAR:
      case HiveLexer.DIV:
        expr = buildNormalArithmeticExpr(child,
            ParseUtils.OPERATOR_MAP.get(child.getType()));
        binPred.addChild(expr);
        continue;
      case HiveLexer.DOT:
        binPred.addChild(buildDotExpr(child));
        continue;
      case HiveParser.TOK_TABLE_OR_COL:
        binPred.addChild(new FieldLiteral("", child.getChild(0).getText(),
            DataType.UNKNOWN));
        continue;
      case HiveParser.BigintLiteral:
      case HiveParser.SmallintLiteral:
      case HiveParser.TinyintLiteral:
        binPred.addChild(new IntLiteral(child.getText()));
        continue;
      case HiveParser.Number:
        binPred.addChild(buildNumLiteral(child));
        continue;
      case HiveParser.DecimalLiteral:
        binPred.addChild(new FloatLiteral(child.getText()));
        continue;
      case HiveParser.StringLiteral:
        binPred.addChild(new StringLiteral(child.getText()));
        continue;
      default:
        throw new SemanticException("Unsupported binary predicate element!");
      }
    }

    return binPred;
  }

  /**
   * Construct a join statement.
   * 
   * @param tree
   * @param op
   * @return
   * @throws SemanticException
   */
  private List<TableRef> buildJoinStmt(ASTNode tree, JoinOperator op)
      throws SemanticException {
    List<TableRef> tblRefs = new ArrayList<TableRef>();
    Expr onClause = null;

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      case HiveParser.TOK_JOIN:
      case HiveParser.TOK_LEFTOUTERJOIN:
      case HiveParser.TOK_RIGHTOUTERJOIN:
      case HiveParser.TOK_FULLOUTERJOIN:
      case HiveParser.TOK_CROSSJOIN:
        tblRefs.addAll(buildJoinStmt(child,
            ParseUtils.JOIN_OPERATOR_MAP.get(child.getType())));
        continue;
      case HiveParser.TOK_TABREF:
        tblRefs.add(buildBaseTableRef(child, op));
        continue;
      case HiveParser.TOK_SUBQUERY:
        tblRefs.add(buildInlineViewRef(child));
        continue;
        // Hive only supports equal condition
      case HiveLexer.EQUAL:
        onClause = buildOnClause(child);
        continue;
      case HiveLexer.KW_AND:
        throw new SemanticException(
            "Cannot support more than one join condition!");
      default:
        throw new SemanticException("Unsupported join element!");
      }
    }

    int size = tblRefs.size();

    if (size < 2) {
      throw new SemanticException("Only one table in a join clause!");
    }

    tblRefs.get(size - 1).setJoinOp(op);
    tblRefs.get(size - 1).setOnClause(onClause);
    tblRefs.get(size - 1).setLeftTblRef(tblRefs.get(size - 2));

    return tblRefs;
  }

  /**
   * Construct a base table reference.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private TableRef buildBaseTableRef(ASTNode tree, JoinOperator op)
      throws SemanticException {
    String tblName = "";
    String alias = "";

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      case HiveParser.TOK_TABNAME:
        tblName = child.getChild(0).getText();
        continue;
      case HiveLexer.Identifier:
        alias = child.getText();
        continue;
      default:
        throw new SemanticException("Unsupported basetable element!");
      }
    }

    BaseTableRef baseTbl = new BaseTableRef(tblName, alias);
    baseTbl.setJoinOp(op);

    return baseTbl;
  }

  /**
   * Construct a subquery statement.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private TableRef buildInlineViewRef(ASTNode tree) throws SemanticException {
    InLineViewRef inlineView = new InLineViewRef();

    for (int i = 0; i < tree.getChildCount(); i++) {
      ASTNode child = (ASTNode) tree.getChild(i);
      switch (child.getType()) {
      case HiveParser.TOK_QUERY:
        inlineView.setQueryStmt(buildQueryStmt(child));
        continue;
      case HiveLexer.Identifier:
        inlineView.setAlias(child.getText());
        continue;
      default:
        throw new SemanticException("Unsupported inlineview element!");
      }
    }

    return inlineView;
  }

  /**
   * Construct a join condition clause. Currently, only equal condition is
   * supported.
   * 
   * @param tree
   * @return
   * @throws SemanticException
   */
  private Expr buildOnClause(ASTNode tree) throws SemanticException {
    Expr onClause = buildNormalBinPredicate(tree, BinOperator.EQ);

    return onClause;
  }

  /**
   * Construct a number literal.
   * 
   * @param tree
   * @return
   */
  private LiteralExpr buildNumLiteral(ASTNode tree) {
    LiteralExpr expr = null;
    String text = tree.getText();

    if (text.contains("."))
      expr = new FloatLiteral(text);
    else
      expr = new IntLiteral(text);

    return expr;
  }
}

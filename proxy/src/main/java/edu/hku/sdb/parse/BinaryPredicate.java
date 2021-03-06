/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.hku.sdb.parse;

public abstract class BinaryPredicate extends Predicate {

  public enum BinOperator {
    EQ("=", "eq"),
    NE("!=", "ne"),
    LE("<=", "le"),
    GE(">=", "ge"),
    LT("<", "lt"),
    GT(">", "gt");

    private final String description;
    private final String name;

    private BinOperator(String description, String name) {
      this.description = description;
      this.name = name;
    }

    @Override
    public String toString() {
      return description;
    }

    public String getName() {
      return name;
    }
  }

  protected final BinOperator op;

  public BinaryPredicate(BinOperator op) {
    this.op = op;
  }

  /**
   * @return the op
   */
  public BinOperator getOp() {
    return op;
  }

  public Expr getLeftExpr() {
    return getChild(0);
  }

  public Expr getRightExpr() {
    return getChild(1);
  }

  public void setLeftExpr(Expr expr) {
    setChild(0, expr);
  }

  public void setRightExpr(Expr expr) {
    setChild(1, expr);
  }

}

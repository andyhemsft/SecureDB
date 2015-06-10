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

import edu.hku.sdb.catalog.MetaStore;

/**
 * Created by Eric Haibin Lin on 28/3/15.
 */
public class LoadStmt implements ParseNode {

  String filePath;
  TableName tableName;
  boolean overWrite = true;

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public TableName getTableName() {
    return tableName;
  }

  public void setTableName(TableName tableName) {
    this.tableName = tableName;
  }

  public LoadStmt(String filePath, TableName tableName) {
    super();
    setFilePath(filePath);
    setTableName(tableName);
  }

  @Override
  public void analyze(MetaStore metaDB, ParseNode... fieldSources) throws SemanticException {

  }

  @Override
  public String toSql() {
    return "LOAD DATA" + " INPATH '" + filePath + "' " + (overWrite ? "OVERWRITE" : "") + " INTO TABLE " + tableName.getName();
  }

  @Override
  public boolean involveSdbEncrytedCol() {
    return false;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof LoadStmt)) {
      return false;
    } else if (!((LoadStmt) object).getTableName().equals(tableName)) {
      return false;
    }
    return filePath.equals(((LoadStmt) object).filePath);
  }
}
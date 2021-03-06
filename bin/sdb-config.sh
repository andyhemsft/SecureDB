#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# included in all the spark scripts with source command
# should not be executable directly
# also should not be passed any arguments, since we need original $*

# resolve links - $0 may be a softlink
this="${BASH_SOURCE-$0}"
common_bin=$(cd -P -- "$(dirname -- "$this")" && pwd -P)
script="$(basename -- "$this")"
this="$common_bin/$script"

# convert relative path to absolute path
config_bin=`dirname "$this"`
script=`basename "$this"`
config_bin=`cd "$config_bin"; pwd`
this="$config_bin/$script"

SDB_VERSION=0.2-SNAPSHOT

export SDB_PREFIX=`dirname "$this"`/..
export SDB_HOME=${SDB_PREFIX}
export SDB_LIB="$SDB_HOME/lib"
export SDB_CONF_DIR="$SDB_HOME/conf"

export SDB_PROXY_LIB="$SDB_HOME/proxy/lib"
export SDB_UDFS_HIVE_DIR="$SDB_HOME/udfs/udfs-hive"
export SDB_UDFS_ODPS_DIR="$SDB_HOME/udfs/udfs-odps"

# Pull all user define parameters
. "$SDB_CONF_DIR/sdb-env.sh" 

for jar in ${SDB_PROXY_LIB}/hive-jdbc/*.jar; do
    SDB_HIVE_JDBC_JAR+=:$jar;
done

export COMMON_JAR=`ls $SDB_LIB/sdb-common-${SDB_VERSION}.jar`
export PROXY_JAR=`ls $SDB_LIB/sdb-proxy-${SDB_VERSION}.jar`
export UDFS_HIVE_JAR=`ls $SDB_UDFS_HIVE_DIR/target/sdb-udfs-hive-${SDB_VERSION}.jar`
export UDFS_ODPS_JAR=`ls $SDB_UDFS_ODPS_DIR/target/sdb-udfs-odps-${SDB_VERSION}.jar`


#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

if [ -n "$CYGWIN" ] ; then
  case `uname` in
    CYGWIN*)
      CYGWIN="${CYGWIN} --noglob"
      ;;
  esac
fi

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD and nproc as necessary
if [ "x$MAX_FD" = "x" ] ; then
  MAX_FD="maximum"
fi

unwrap() {
  case "$1" in
    -* ) ;;
    * ) eval "$1"='`cygpath --path --unix "$'"$1"'"'`' ;;
  esac
}

CYGWIN=false
msys=false
case "`uname`" in
  CYGWIN* )
    CYGWIN=true
    ;;
  MINGW* | MSYS* )
    msys=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if $CYGWIN ; then
  app_home_lib_dir=`cygpath -w -m "$APP_HOME/gradle/wrapper"`
  class_path="$app_home_lib_dir/gradle-wrapper.jar"
  app_home_lib_dir=`cygpath -w -m "$APP_HOME"`
  PROCESSOR_ARCHITECTURE="${NUMBER_OF_PROCESSORS#* }"
else
  class_path="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
fi

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    # IBM's JDK on AIX uses strange locations for the java executable
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ] ; then
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation."
    exit 1
  fi
else
  JAVACMD="java"
  which java >/dev/null 2>&1 || {
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation."
    exit 1
  }
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$msys" = "false" ] ; then
  MAX_FD_LIMIT=`ulimit -H -n 2>/dev/null`
  if [ $? -eq 0 ] ; then
    if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
      MAX_FD="$MAX_FD_LIMIT"
    fi
    ulimit -n $MAX_FD 2>/dev/null
    if [ $? -ne 0 ] ; then
      echo "Could not set maximum file descriptor limit: $MAX_FD"
    fi
  fi
fi

# For Darwin/MacOS, add options to control the use of the Swift runtime
if [ "`uname`" = "Darwin" ] ; then
  DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS \"-Dfile.encoding=UTF-8\""
fi

# Collect all arguments for the java command, following the shell quoting standards
save() {
  for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/'/" ; done
  printf " "
}

DEFAULT_JVM_OPTS=$(echo "$DEFAULT_JVM_OPTS" | xargs)

APP_ARGS=$(save "$@")

# Execute JVM
exec "$JAVACMD" $DEFAULT_JVM_OPTS "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$class_path" org.gradle.wrapper.GradleWrapperMain "$@"

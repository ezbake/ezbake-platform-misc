#! /bin/bash
#   Copyright (C) 2013-2014 Computer Sciences Corporation
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.


if [ -z "`ls target/ezbake-thrift-runner-*.jar`" ];
then
    echo "You must build the jar before running this script."
    exit
fi

if [ -n "`jar tvf target/ezbake-thrift-runner-*.jar  | grep ezshade | grep -i  ezbake `" ]; 
then
    echo "No: There is an ezbake library being shaded."
    exit
fi

if [ -n "`jar tvf target/ezbake-thrift-runner-*.jar  | grep -v ezshade | grep -vi ezbake | grep -vi javax |  grep \\\.class`" ];
then
    echo "No: There is a (excluding ezbake) class not being shaded."
    exit
fi

echo "Yes, everything is shaded correctly"


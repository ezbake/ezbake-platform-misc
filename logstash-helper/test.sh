#!/bin/bash
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


export LOGSTASH_CONF_TEMPLATE=templates/logstash.conf.erb

source logstash.sh

# test log function
log log.log hello this is a log message

# make sure check args works
&>/dev/null check_arguments "should fail Usage: <app_name> <service_name>" app_name=1 svc_name= && exit 1
check_arguments "should not fail Usage: <app_name> <service_name>" app_name=1 svc_name=2 || exit 1

# should do some assertions on the generated logstash template
configure_logstash common_services ezca build /tmp/common_services/ || exit 1
configure_logstash common_services ezca build /opt/ezfrontend/logs/ || exit 1

# test start_logstash function
start_logstash common_services logstash_test build || { echo "start_logstash test failure" >&2;  exit 1; }

stop_logstash common_services logstash_test build build || { echo "stop_logstash test failure" >&2;  exit 1; }

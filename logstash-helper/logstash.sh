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


# location of the logstash.conf template defaults to /etc/sysconfig/ezbake/logstash.conf.erb,
# but can be overriden by exporting LOGSTASH_CONF_TEMPLATE before sourcing this file
CONF_TEMPLATE=${LOGSTASH_CONF_TEMPLATE:-"/etc/sysconfig/ezbake/logstash.conf.erb"}

# attempt to read the ezbake_log_dir out of the default ezconfiguration directory
# the log directory will be one of:
#  - EZBAKE_LOG_DIR environment variable
#  - value returned by grepping for the ezbake.log.directory in the default ezbake directory
#  - /tmp
LOG_DIR=${EZBAKE_LOG_DIR:-$(grep -s 'ezbake.log.directory' /etc/sysconfig/ezbake/*.properties | cut -d'=' -f2-)}
EZBAKE_LOG_DIR=${LOG_DIR:-"/tmp"}

# location of the logstash binary
LOGSTASH_BIN=${LOGSTASH_BIN:-"/opt/logstash/bin/logstash"}

JAVA_OPTS="-Djava.io.tmpdir=/opt/logstash/tmp"

# A quick function which will log messages to the log file message will
# have the date attached to them.
#
# Arguments:
#   $* = The log message that we will use
function log() {
    echo "[$(date)]: ${@:2}" | tee -a "${1}"
}

function check_arguments() {
    local msg="${1}"
    local args="${@:2}"
    for arg in ${args[@]}; do
        local var=$(echo ${arg} | cut -d'=' -f1)
        local val=$(echo ${arg} | cut -d'=' -f2)
        if [ "${val}x" == "x" ]; then
            echo "${msg}" >&2
            echo "${var} is required" >&2
            return 1
        fi
    done
    return 0
}


function is_running() {
    local pid_file="$1"
    if [ -f "${pid_file}" ]; then
        local lspid=$(cat "${pid_file}")
        if /bin/ps -p $lspid 1>&2 >/dev/null; then
            return 0
        fi
    fi
    return 1
}


function configure_logstash() {
    local app_name="$1"
    local svc_name="$2"
    local config_dir="$3"
    local log_dir="$4"

    check_arguments "Usage: configure_logstash <app_name> <svc_name> <config_dir> <log_dir>" \
        "app_name=$app_name" "svc_name=$svc_name" "config_dir=$config_dir" "log_dir=${log_dir}" \
        || return 1

    if [ ! -f "${CONF_TEMPLATE}" ]; then
        echo "logstash configuration template ${CONF_TEMPLATE} does not exist" >&2 && return 1
    fi

    if [ ! -d "${config_dir}" ]; then
        mkdir -p "${config_dir}"
    fi

    # evaluate logstash configuration template and place the result in the config directory
    env EZBAKE_APPLICATION_NAME="${app_name}" EZBAKE_SERVICE_NAME="${svc_name}" \
        OPENSHIFT_LOG_DIR="${log_dir}" \
	erb -dv "${CONF_TEMPLATE}" > "${config_dir}/logstash.conf"
    return $?
}

function start_logstash() {
    local app_name="$1"
    local svc_name="$2"
    local eff_app_name="$([ "${app_name}" == "common_services" ] && echo "${svc_name}" || echo "${app_name}")"
    local logstash_dir="$3"
    local log_dir="${4:-"${EZBAKE_LOG_DIR}/${eff_app_name}/"}"
    local pid_file="${logstash_dir}/logstash.pid"
    local conf_file="${logstash_dir}/logstash.conf"

    # check arguments
    check_arguments "Usage: start_logstash <app_name> <svc_name> <logstash_dir>" \
	    "app_name=$app_name" "svc_name=$svc_name" "logstash_dir=$logstash_dir" \
	    || return 1

    # determine whether or not logstash is already running
    if ! is_running "${pid_file}"; then
        # re-write the logstash config file
	configure_logstash "${app_name}" "${svc_name}" "${logstash_dir}" "${log_dir}" \
		|| { echo "failed to generate logstash configuration file"; return 1; }

	local log_file="${log_dir}/logstash.log"
        JAVA_OPTS="${JAVA_OPTS}" nohup "${LOGSTASH_BIN}" agent -f "${conf_file}" -l "${log_file}" &>/dev/null &
        local ret=$?
        if [ $ret -ne 0 ]; then
            log "${log_file}" "Logstash failed to start - $ret"
            return $ret
        fi
        echo $! > "${pid_file}"
    fi

    return 0
}

function stop_logstash() {
    local app_name="$1"
    local svc_name="$2"
    local logstash_dir="$3"
    local eff_app_name="$([ "${app_name}" == "common_services" ] && echo "${svc_name}" || echo "${app_name}")"
    local log_dir="${4:-"${EZBAKE_LOG_DIR}/${eff_app_name}/"}"
    local log_file="${log_dir}/logstash.log"
    local pid_file="${logstash_dir}/logstash.pid"
    local conf_file="${logstash_dir}/logstash.conf"

    check_arguments "Usage: stop_logstash <app_name> <svc_name> <logstash_dir> [log_dir]" \
	    "app_name=$app_name" "svc_name=$svc_name" "logstash_dir=$logstash_dir" \
	    || return 1

    if is_running "${pid_file}"; then
        local lspid=$(<"${pid_file}");
        log "${log_file}" "Stopping logstash by sending SIGTERM to ${lspid}"
        kill -s SIGTERM $lspid
	rm -f "${pid_file}"
        log "${log_file}" "Successfully stopped logstash cartridge"
        if [ -f "${conf_file}" ]; then
            rm -f "${conf_file}"
        fi
    else
        log "${log_file}" "Logstash is not running!"
    fi
    return 0
}

function restart_logstash() {
    start_logstash $*
    stop_logstash $*
}


#!/usr/bin/env bash

function find_idle_profile()
{
    RESPONSE_CODE=$(sudo curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/ping/)

    if [ "${RESPONSE_CODE}" -ge 400 ]
    then
        CURRENT_PROFILE=prod2
    else
        CURRENT_PROFILE=prod1
    fi

    if [ "${CURRENT_PROFILE}" == prod1 ]
    then
      IDLE_PROFILE=prod2
    else
      IDLE_PROFILE=prod1
    fi

    echo "${IDLE_PROFILE}"
}

function find_idle_port()
{
    IDLE_PROFILE=$(find_idle_profile)

    if [ "${IDLE_PROFILE}" == prod1 ]
    then
      echo "8081"
    else
      echo "8082"
    fi
}

#!/bin/bash
set -e

# Start ProxySQL with PID 1
exec java -XX:+UseG1GC -Dapp.port=${APP_PORT} -Daccess.token=${ACCESS_TOKEN} -Dsecret.token=${SECRET_TOKEN} -Dslack.noti=${SLACK_NOTI} -Dslack.errornoti=${SLACK_ERRORNOTI} -jar /opt/buybye.jar &
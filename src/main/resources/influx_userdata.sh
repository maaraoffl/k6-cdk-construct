#!/bin/bash
yum update -y

wget https://dl.influxdata.com/influxdb/releases/influxdb-1.8.7.x86_64.rpm
yum localinstall -y influxdb-1.8.7.x86_64.rpm
service influxdb start
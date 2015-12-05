#!/bin/sh

# Run this script as root

rabbitmqctl delete_user ccm-dev
rabbitmqctl delete_user ccm-admin
rabbitmqctl delete_vhost ccm-dev-vhost


#!/bin/sh
# Start script for Railway deployment
# Replaces PORT placeholder in nginx config and starts nginx

# Use Railway's PORT or default to 80
PORT=${PORT:-80}

# Replace PORT in nginx config
sed "s/listen 80;/listen $PORT;/g" /etc/nginx/conf.d/default.conf > /tmp/nginx.conf
mv /tmp/nginx.conf /etc/nginx/conf.d/default.conf

# Start nginx
nginx -g "daemon off;"

#!/bin/sh

# Replace PORT in nginx config with actual PORT from Railway
sed -i "s/listen 8080;/listen ${PORT:-8080};/g" /etc/nginx/conf.d/default.conf

# Start nginx
nginx -g 'daemon off;'

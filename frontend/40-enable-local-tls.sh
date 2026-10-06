#!/bin/sh
set -eu
if [ -s /etc/nginx/certs/buymore.com.pem ] && [ -s /etc/nginx/certs/buymore.com-key.pem ]; then
  sed -i '/listen 80;/a\  listen 443 ssl;\n  ssl_certificate /etc/nginx/certs/buymore.com.pem;\n  ssl_certificate_key /etc/nginx/certs/buymore.com-key.pem;' /etc/nginx/conf.d/default.conf
fi

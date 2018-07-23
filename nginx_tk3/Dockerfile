FROM nginx:1.15.1

COPY nginx.conf.template /etc/nginx/nginx.conf.template

COPY entrypoint.sh /entrypoint.sh
RUN chmod a+x /entrypoint.sh

ENTRYPOINT /entrypoint.sh 

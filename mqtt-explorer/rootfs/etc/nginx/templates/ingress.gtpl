server {
    listen {{ .interface }}:{{ .port }} default_server;

    include /etc/nginx/includes/server_params.conf;
    include /etc/nginx/includes/proxy_params.conf;

    # The client addresses everything relative to the page it was loaded
    # from, its Socket.IO endpoint included, so nothing in the responses
    # needs rewriting for the path Ingress serves it under.
    location / {
        allow   172.30.32.2;
        deny    all;

        proxy_pass http://backend;
    }
}

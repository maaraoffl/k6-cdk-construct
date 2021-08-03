yum update -y
yum install -y docker
service docker start
usermod -a -G docker ec2-user

cat > /etc/yum.repos.d/grafana.repo << EOF
[grafana]
name=grafana
baseurl=https://packages.grafana.com/oss/rpm
repo_gpgcheck=1
enabled=1
gpgcheck=1
gpgkey=https://packages.grafana.com/gpg.key
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
EOF

mkdir -p /var/lib/grafana/dashboards
wget -O /var/lib/grafana/dashboards/k6.json https://grafana.com/api/dashboards/2587/revisions/3/download
sed -i 's/\${DS_K6}/k6influxdb/g' /var/lib/grafana/dashboards/k6.json

yum install -y grafana
cat > /etc/grafana/provisioning/dashboards/dashboard.yaml << EOF
apiVersion: 1
providers:
  - name: 'default'       
    org_id: 1             
    folder: ''            
    type: 'file'          
    options:
      path: /var/lib/grafana/dashboards
EOF
cat > /etc/grafana/provisioning/datasources/datasource.yaml << EOF
apiVersion: 1
datasources:
  - name: k6influxdb
    type: influxdb
    access: proxy
    database: k6
    url: http://$INFLUX_DB_URL:8086
    isDefault: true
EOF

systemctl daemon-reload
systemctl start grafana-server
systemctl enable grafana-server
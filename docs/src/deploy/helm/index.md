# GeoServer Cloud deployment with Helm

This example demonstrates how to deploy a simple **GeoserverCloud** setup in your local cluster using Helm.  
It's a basic setup with the `pgconfig` profile enabled (catalog stored in a database).
Based on the use of [camptocamp/helm-geoserver-cloud](https://github.com/camptocamp/helm-geoserver-cloud) chart.

## Considerations

Make sure to review this documentation as a reference [README.md](https://github.com/camptocamp/helm-geoserver-cloud/blob/master/examples/README.md) . A local Kubernetes cluster and kubectl are required to run this demo.

## Steps

### 1. Update dependencies  
Before installing the chart, make sure Helm dependencies are up to date:

```shell
helm dependency update
```

### 2. Install chart  
Now install (or upgrade) the chart in your cluster using the following command:

```shell
helm upgrade --install gs-cloud-pgconfig .
```

### 3. Setup alias  
To make it easier to access the services via a browser, we use the DNS name `gscloud.local`. You can map it to the local ingress IP by adding an entry to your `/etc/hosts` file:

```shell
kubectl get ingress --no-headers  gs-cloud-pgconfig-geoserver-host1 | awk '{printf("%s\t%s\n",$4,$3 )}' | sudo tee -a /etc/hosts
```

### 4. Check Pods execution  
Verify that all necessary pods have started and are running correctly:

```shell
kubectl get po
```

Expected output:
```text
NAME                                            READY   STATUS    RESTARTS   AGE
gs-cloud-pgconfig-gsc-gateway-87f5cf44c-bmcs5   1/1     Running   0          5m8s
gs-cloud-pgconfig-gsc-gwc-79697d48d5-2bwlg      1/1     Running   0          5m8s
gs-cloud-pgconfig-gsc-rest-bb4bb64c-jccdr       1/1     Running   0          5m8s
gs-cloud-pgconfig-gsc-wcs-5df8b5bd69-bfwms      1/1     Running   0          5m8s
gs-cloud-pgconfig-gsc-webui-84bb79b95f-zggkw    1/1     Running   0          5m8s
gs-cloud-pgconfig-gsc-wfs-59df48668c-x4lw2      1/1     Running   0          5m8s
gs-cloud-pgconfig-gsc-wms-d569fd94f-5vrmh       1/1     Running   0          5m8s
gs-cloud-pgconfig-gsc-wps-94cbdd747-n69lf       1/1     Running   0          5m8s
gs-cloud-pgconfig-postgresql-0                  1/1     Running   0          5m8s
gs-cloud-pgconfig-rabbitmq-0                    1/1     Running   0          5m8s
```

### 5. Initialize example catalog  
Run the following script to populate the database with an initial catalog for testing:

```shell
./init-catalog.sh
```

### 6. Access WebUI  
You can now access the GeoServer Cloud Web UI at:  
[http://gscloud.local/geoserver-cloud/web/](http://gscloud.local/geoserver-cloud/web/)  
Login with the default credentials:  
**Username:** `admin`  
**Password:** `geoserver`

## Uninstall chart  
To clean up your cluster and remove the deployment:

```shell
helm uninstall gs-cloud-pgconfig
```


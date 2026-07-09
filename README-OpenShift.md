# Run CDC CHCCLP REST server on OpenShift

The Dockerfile creates an image with the REST server that will connect with IDR CDC Access Server and execute commands.

# Building the CDC CHCCLP REST Server image

## Mandatory build arguments
None.

## Optional build arguments
None

## Build steps OpenShift
The below steps will build the container image and push it to the OpenShift image registry.

### Download CDC Access Server JAR file

For the REST service to work, you need a working copy of the IDR CDC Access Server. The REST service uses embedded CHCCLP which requires the Access Server jar files. You must copy all jar files from the Access Server into the `lib` directory of this repo.

### Build the CDC CHCCLP REST server image
```
oc new-project cdc
oc project cdc
oc new-build --binary=true --name=cdc-chcclp-rest
oc start-build cdc-chcclp-rest --no-cache --from-dir=. --follow 
```

By default the image tag is: `image-registry.openshift-image-registry.svc:5000/cdc/cdc-chcclp-rest:latest`. If ou prefer to use the image digest, you need to update the `cdc-chcclp-rest-server.yaml` file with the specific tag.

### Set the Access Server hostname and port (optional)
If you want to create a default connection to a CDC Access Server, create the `cdc-chcclp-rest-config` secret.

```
export CDC_ACCESS_SERVER_HOST=cdc-access-server
export CDC_ACCESS_SERVER_PORT=10101
oc create cm cdc-chcclp-rest-config
oc set data cm/cdc-chcclp-rest-config \
  --from-literal=CDC_ACCESS_SERVER_HOST=$CDC_ACCESS_SERVER_HOST \
  --from-literal=CDC_ACCESS_SERVER_PORT=$CDC_ACCESS_SERVER_PORT
```

If you do not set the default access server and host, you will need to specify these values when you connect to the REST service using the `/connect` API.

### Deploy the CDC CHCCLP REST Server

```
oc project cdc
oc apply -f cdc-chcclp-rest.yaml
```
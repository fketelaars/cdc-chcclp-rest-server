# CDC CHCCLP REST service

Interface into the CHCCLP command line interface using REST APIs. Later this will also be made available as an MCP server.

## TechZone reservation setup

* Request a TechZone environment **IBM Data Replication - CDC Demo Environment V2**: https://techzone.ibm.com/my/reservations/create/6867e07744d01770f44d78a6
* Host name and IP addresses for the different services are in the reservation
* Record host names, ports for the access server. You will need to configure this in the `.env` file

## Download the Access Server jar files

* The jar files that the REST service needs are in the Access Server `lib` directory: `/data/iidr/cdcaccess/lib`
* Copy all the jar files into the `lib` directory

## Set up the datastore locking

By default, the Access Server `Admin` user can only connect once to any data store. To avoid having to log out from the Management Console, do the following
* Open Management Console
* Select the Access Manager tab
* Right-click on the Db2LUW and Kafka data stores, select Properties
* Untick the **Require subscriptions to be locked** for both data store

## Create subscription FK1

* Create subscription FK1 to replicate from Db2LUW to Kafka
* Map a couple of tables

## Test the Sample1 program

* Compile the Sample1 class
* Update the `.env` file and specify the correct host, port, Admin user and password for Access Server
* Run the Sample1 class: `java -cp "target/cdc-chcclp-rest-server-1.0-SNAPSHOT.jar.original:lib/*" Sample1 2>&1`
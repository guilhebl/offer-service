# offer-service

Offer Service - is an open source project that aims to bring together several worldwide marketplaces and APIs and provide 
insightful data and analytics from a large variety of sources into a single place.

Current Supported Marketplaces:

- Amazon
- Walmart
- BestBuy
- Ebay

##### Checkout [Current LIVE beta](https://searchprod.com)

&nbsp;&nbsp;

*** 

##### Current stack:

. [Frontend](https://github.com/guilhebl/platform)

. [Backend](https://github.com/guilhebl/offer-service)


## Getting started

in order to get started you must first create your own API keys at each
of these company's website (Search for these and follow instructions on each website):

- Amazon Product Advertising API

- Ebay Product Search API

- Walmart API

- BestBuy API

After creating your api keys set the values in "app-config.properties" file replacing proper entries
with each provider's own key. 

### Running

Using SBT the following at the command prompt will start up Play in development mode:

```bash
sbt run
```

Play will start up on the HTTP port at <http://localhost:9000/>.   You don't need to deploy or reload anything -- changing any source code while the server is running will automatically recompile and hot-reload the application on the next HTTP request.


1. To start app run: ``` sbt run```
 
### Load Testing

2. To run all tests: ``` sbt test```

3. To run a single test: ``` sbt testOnly <package>.<ClassName>```


##### Load Testing


Start Play in production mode, by [staging the application](https://www.playframework.com/documentation/2.6.x/Deploying) and running the play script:s

```bash
sbt stage
cd target/universal/stage
bin/play-rest-api-example -Dplay.crypto.secret=testing
```

Then you'll start the Gatling load test up (it's already integrated into the project):

```bash
sbt gatling:test
```

For best results, start the gatling load test up on another machine so you do not have contending resources.  You can edit the [Gatling simulation](http://gatling.io/docs/2.2.2/general/simulation_structure.html#simulation-structure), and change the numbers as appropriate.

Once the test completes, you'll see an HTML file containing the load test chart:

```bash
 ./rest-api/target/gatling/gatlingspec-1472579540405/index.html
```

That will contain your load test results.


### Server deployment

For Play version 2.6: "Play now uses the Akka-HTTP server engine as the default backend."

Past versions were using Netty as default. It is still supported but must be explicitly configured.

For deploying your app you need to create a distribution package to do that run on your project home folder:

```sbt dist```

A binary distribution is created: "This produces a ZIP file containing all JAR files needed to run your application in the target/universal folder of your application.

To run the application, unzip the file on the target server, and then run the script in the bin directory. The name of the script is your application name, and it comes in two versions, a bash shell script, and a windows .bat script."

These are the steps to deploy your app:

1. Create a distribution package and copy the package to destination folder on target server (use FTP)
2. Generate your application secret run: playGenerateSecret
3. Log in the target server, create a user called api-service (check section below on server config)
4. Write down the app secret generated in step 2 and set it as an env var on the target server, on Linux under <HOME>/.bashrc
5. Copy and Unzip the package in your server and Run the app's binary start script and pass the secret as a param:
```unzip my-first-app-1.0.zip```

```my-first-app-1.0/bin/my-first-app -Dplay.http.secret.key=abcdefghijk```


Check [deployment strategy](https://www.playframework.com/documentation/2.6.x/Deploying)
for more details.

### Service configuration - Linux

In order to configure this app as a service on Linux run:

Create api-service user and group

```sudo useradd api-service```

Copy ./scripts/startup.sh to /etc/init.d:

```cp "/scripts/startup.sh" "/etc/init.d/api-service.sh"```

make the script executable:

```chmod +x /etc/init.d/api-service.sh```

make sure api-service user has ownership of app folder:

```sudo chown -R api-service /home/api-service/```

Install service to be run at boot-time:

```update-rc.d api-service.sh defaults```

To check service logs:

```journalctl -u api-service -b```

### MongoDB

To install MongoDb follow [instructions](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/)

To access Mongo Shell run:

```mongo```

To list Dbs: ```show dbs```

To Query offer collections: 
```use searchprod```


### Redis Cache

in order to install Redis follow instructions [here](https://redis.io/topics/quickstart)

after installing redis start the service using:

```sudo systemctl start redis```

to run Redis CLI:

```
redis-cli
```

To flush cache (using redis-cli)

```flushall```

Config redis.conf:

it is important to configure Redis to use the proper log file path and dump file

```
sudo mkdir /var/log/redis
sudo touch /var/log/redis/redis-server.log
sudo chown -R redis:redis /var/log/redis/
sudo chown -R redis:redis /etc/redis/
```

open redis.conf file located in etc/redis/ 

and set:

```
## SET LOG FILE LOCATION
logfile "/var/log/redis/redis-server.log"

## SET THIS LINE TO NO
stop-writes-on-bgsave-error no

```

Restart Redis after changing config: ```sudo systemctl restart redis```

To check where is Redis is storing its RDB (Snapshot file):

```
redis-cli
CONFIG GET dir
CONFIG GET dbfilename
```

To check logs:

```sudo tail -f -n2000 /var/log/redis/redis-server.log```

# offer-service

Searchprod offer-service (AKA Offer-backend) is an open source project that aims to bring together several world-wide marketplaces and provide best offers and results in a single consolidated platform.
It's goal is to unify diverse market sources in order to connect people directly with products and services that they need through a global marketplace platform.
Connecting users to local and international marketplaces and enabling the consumer to receive informative insights of a app.product or service in a transparent manner.
Find out about prices, availability, features, reviews and more searching in one place and receiving the best results from multiple sources.
Join the project and help make the world a better place for trade.


##### Checkout [Current LIVE beta](https://searchprod.com)

&nbsp;&nbsp;

##### Current stack:

. [Frontend](https://github.com/guilhebl/offer-web) -> Angular4, Typescript, NgRx, Gulp

. [Backend](https://github.com/guilhebl/offer-java) -> Java

##### Future stack:

. [Frontend](https://github.com/guilhebl/angular-starter) -> Angular5, Typescript, Webpack

. [Backend](https://github.com/guilhebl/offer-service) -> Scala

## Getting started

in order to get started you must first create your own API keys at:

- Amazon Product Advertising API

- Ebay Product Search API

- Walmart API

- BestBuy API

After creating your api keys set the values in "app-config.properties" file replacing proper entries

### Running

You need to download and install sbt for this application to run.

Once you have sbt installed, the following at the command prompt will start up Play in development mode:

```bash
sbt run
```

Play will start up on the HTTP port at <http://localhost:9000/>.   You don't need to deploy or reload anything -- changing any source code while the server is running will automatically recompile and hot-reload the application on the next HTTP request.

This is the RESTful API backend for offer-web based on [Scala Play REST sample](https://github.com/playframework/play-scala-rest-api-example).

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
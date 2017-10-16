---
layout: page
title:  "H2 database: Connecting external applications"
categories: [builtin]
---

This document presents several examples on how to access data contained in a H2 server instance managed by Eclipse Kura from external applications such as LibreOffice and Python scripts.

### Create a test database instance

The first step involves creating a test database instance and filling it with some data:

1. As explained [here](h2-database.html), create a new database instance named `TestDb` with the following settings:

* **db.connector.url**: `jdbc:h2:mem:testdb`
* **password**: `password`

{:start="2"}
2. Insert some data in the database, using Kura Wires. Create a Wire Graph as follows, where **Timer** is a `Timer` component with default settings and **Store** is a `DB Store` component configured to use the DB instance previously created by setting **db.service.pid** to `TestDb`. The Timer component will periodically emit a wire envelope containing a property named `TIMER` whose value is the current timestamp. The store component will create a column with the same name in the `WR_data` table of the database containing the received timestamps.

![Wire Graph]({{ site.baseurl }}/assets/images/database/wire_graph.png)

{:start="3"}
3. Start the H2 server, creating a new `H2DbServer` instance and configuring it to start in TCP mode as follows:

* **db.server.enabled** : `true`
* **db.server.type** : `TCP`
* **db.server.commandline** : `-tcpPort 9123 -tcpAllowOthers -ifExists`

### Accessing the data

An external application can connect to the H2 server in two ways:

* **Using the H2 JDBC Driver** : if the server is configured in TCP mode it exposes the data using a H2 specific protocol implemented by the H2 JDBC driver. In order to use this access mode the H2 jar must be available to the external application since it contains the JDBC driver.

* **Using the PostgreSQL network protocol** : H2 server also provides an experimental implementation of the PostgreSQL network protocol (see [here](http://www.h2database.com/html/advanced.html#odbc_driver)). This protocol can be enabled by configuring the server in PG mode. This mode should allow any application that supports the PostgreSQL network protocol to access the database.

#### Access the data using LibreOffice

The H2 JDBC Driver can be used to access database data using LibreOffice. The steps below have been tested using LibreOffice 5.3.3.2 on OSX.

**Import the H2 jar into LibreOffice classpath**

  1. Download H2 jar 1.4.192 from Maven (https://mvnrepository.com/artifact/com.h2database/h2/1.4.192)

  2. Open LibreOffice (any application) and open the preferences window.

  3. Select **Advanced** from the side menu under **LibreOffice** and press the **Class Path** button, the following window should appear:
  ![LibreOffice Classpath]({{ site.baseurl }}/assets/images/database/libreoffice_classpath.png)

  4. Click on the **Add Archive** button, select the h2 jar previously downloaded and press **Ok**

  5. Exit from LibreOffice.

{% include alerts.html message="LibreOffice needs a restart to update its classpath. It is important to make sure that the LibreOffice process is terminated before proceeding with the next steps." %}
 
**Connect to the database instance**

1. Open LibreOffice Base, the following wizard should appear.

![Base]({{ site.baseurl }}/assets/images/database/base.png)

{:start="2"}
2. Select **Connect to and existing database** and click next.

3. Set the JDBC driver and DB URL as follows:

  * **Datasource URL** : `jdbc:h2:tcp:<ip-address>:9123/mem:testdb`
        Replacing `<ip-address>` with the address of the device running Kura.

  * **JDBC Driver**: `org.h2.Driver`

![JDBC]({{ site.baseurl }}/assets/images/database/base_url.png)

{:start="4"}
4. Click on **Test Class** to make sure that the driver can be loaded successfully.

5. Set the credentials: enter `SA` in the user name field, check the **Password required** field and click **Test connection**. Then enter `password` in the **Password** field when required. Finally click **Next**. Leave the settings unchanged on the next screen, press **Finish** and save the database.

{% include alerts.html message="Make sure that the port 9123 is open on the device firewall before connecting." %}

![Credentials]({{ site.baseurl }}/assets/images/database/user_name.png)

{:start="6"}
6. View database contents: after the previous steps you should see the screen below. Double click on the `WR_data` table then its contents should be displayed. At this point LibreOffice should have created a Datasource representing the database, so it can be used for importing data from the database to other LibreOffice applications, like Calc.

![Main]({{ site.baseurl }}/assets/images/database/base_main.png)

![Query result]({{ site.baseurl }}/assets/images/database/query_result.png)

**Importing the data in Calc**

1. Open Calc, click on **View** > **Data sources** and the database file previously saved (in this case `test`) should be visible in the top left part of the screen. It should be possible to expand it and find the `TESTDB.PUBLIC.WR_data` table under **Tables**.

![Data Sources]({{ site.baseurl }}/assets/images/database/data_sources.png)

{:start="2"}
2. The data from the table can be imported in Calc by dragging and dropping the `TESTDB.PUBLIC.WR_data` table in an empty cell of the spreadsheet.

![Imported data]({{ site.baseurl }}/assets/images/database/imported_data.png)
    
#### Access the data using Python

This section describes how to access the data previously created using two different Python libraries. It will be assumed that the scripts are created and executed on a Raspberry PI running Raspbian and that the Kura instance hosting the database is running on the same machine.

**JayDeBeApi**

The JayDeBeApi library allows to use JDBC drivers from Python. In order to use this library the H2 jar is required, since it contains the needed JDBC driver.

1. Install the `pip` tool:

```bash
sudo apt-get install python3-pip
```

{:start="2"}
2. Install the `jaydebeapi` module using pip:

```bash
sudo pip3 install jaydebeapi
```

{:start="3"}
3. Download the H2 jar from Maven Central

```bash
curl "http://central.maven.org/maven2/com/h2database/h2/1.4.192/h2-1.4.192.jar" > h2.jar
```

{:start="4"}
4. Create and run the following Python 3 script from the same directory as the H2 jar:

```python
import jaydebeapi
conn = jaydebeapi.connect("org.h2.Driver", # driver class
                            "jdbc:h2:tcp:localhost:9123/mem:testdb", # JDBC url
                            ["SA", "password"], # credentials
                            "./h2.jar",) # location of H2 jar
try:
        curs = conn.cursor()
        # Fetch the last 10 timestamps
        curs.execute("SELECT TIMER FROM \"WR_data\" ORDER BY TIMER DESC LIMIT 10")
        for value in curs.fetchall():
                # the values are returned as wrapped java.lang.Long instances
                # invoke the toString() method to print them
                print(value[0].toString())
finally:
        if curs is not None:
                curs.close()
        if conn is not None:
                conn.close()
```

It should print on the console the latest 10 timestamp values generated by the Timer component.

**psycopg2**

The `psycopg2` Python module allows to connect to the server in PostgreSQL mode.

1. Change the H2 Server mode to Postgres:

  * **db.server.enabled** : `true`
  * **db.server.type** : `PG`
  * **db.server.commandline** : `-pgPort 9123 -pgAllowOthers -ifExists

{:start="2"}
2. Install the `pip` tool as in the previous section

3. Install `postgresql-server-dev-9.4` package:

```bash
sudo apt-get install postgresql-server-dev-9.4
```

{:start="4"}
4. Install the `psycopg2` module using pip:

```bash
sudo pip3 install psycopg2
```

{:start="5"}
5. Create and run the following Python 3 script:

```python
import psycopg2
conn = psycopg2.connect("dbname=mem:testdb user=sa password=password host=localhost port=9123")
try:
        curs = conn.cursor()
        # Fetch the last 10 timestamps
        curs.execute("SELECT TIMER FROM \"WR_data\" ORDER BY TIMER DESC LIMIT 10")
        print(curs.fetchall())
finally:
        if curs is not None:
                curs.close()
        if conn is not None:
                conn.close()
```

It should print on the console the latest 10 timestamp values generated by the Timer component like the previous script.



Tramchester 
===========
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/cartwrightian/tramchester_ic/tree/master.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/cartwrightian/tramchester_ic/tree/master)

Tramchester was built by [ThoughtWorks](http://www.thoughtworks.com) in Manchester

A Simple web app and server for planning journeys on Manchester's tram system.

There is a short article on the application
[here](https://www.thoughtworks.com/insights/blog/helping-people-navigate-public-tram-network)

License
-------
See LICENSE.txt

Data
----

"Contains public sector information licensed under the Open Government Licence v2.0 byData GM."

"Contains Transport for Greater Manchester data."

"ORDNANCE SURVEY DATA LICENCE
 
 Contains Ordnance Survey data © Crown copyright and database right 2020.
 
 Contains Royal Mail data © Royal Mail copyright and database right 2020.
 Contains National Statistics data © Crown copyright and database right 2020.
 
 February 2020"

"Timetable/Fares/London Terminals data under licence from [RSP](http://www.raildeliverygroup.com/)"

Links
-----

[TFGM GTFS Data Set](https://www.data.gov.uk/dataset/c3ca6469-7955-4a57-8bfc-58ef2361b797/gm-public-transport-schedules-gtfs-dataset) on data.gov.uk

Notes
-----
Fox fix issue with missing cert for tfgm data download

> openssl s_client -connect odata.tfgm.com:443 | openssl x509 -out tfgm.cert

> keytool -import -alias tfgm -file ./tfgm.cert -keystore $JAVA_HOME/lib/security/cacerts

Might need sudo depending on JAVA_HOME location.





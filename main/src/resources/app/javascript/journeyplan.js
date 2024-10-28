
const axios = require('axios');

import { createApp, ref } from 'vue'

import vueCookies from 'vue-cookies'

import 'vuetify/styles'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

require('file-loader?name=[name].[ext]!../index.html');

import { createVuetify } from 'vuetify'
const vuetify = createVuetify({components, directives})

import '@mdi/font/css/materialdesignicons.css'

import './../css/tramchester.css'

import Notes from "./components/Notes"
import Journeys from './components/Journeys'
import Footer from './components/Footer'
import Header from './components/Header';
import LiveDepartures from './components/LiveDepatures'
import LocationSelection from './components/LocationSelection'
import Closures from './components/Closures'

function getNow() {
    return new Date();
}

function getCurrentTime() {
    return getNow().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
}

function getCurrentDate() {
    const now = new Date().toISOString();
    return now.substr(0,  now.indexOf("T")); // iso-8601 date part only as YYYY-MM-DD
}

function selectModesEnabled(scope) {
    return scope.modes.length > 1;
}

// called post journey query
function displayLiveData(app) {
    const queryDate = new Date(app.date); 
    const today = getNow();
    // check live data for today only, todo 
    if (today.getMonth()==queryDate.getMonth()
        && today.getYear()==queryDate.getYear()
        && today.getDate()==queryDate.getDate()) {
        const notesStations = getCallingStationsFrom(app.journeys);
        queryLiveData(app, notesStations);
    } else {
        app.liveDepartureResponse = null;
    }
}

function getCallingStationsFrom(journeys) {
    var stations = new Set();
    journeys.forEach(journeyPlan => {
        const journey = journeyPlan.journey;
        const changes = journey.changeStations;
        changes.forEach(change => {
            if (change.locationType=='Station') {
                stations.add(change.id);
            }
        })
        stations.add(journey.destination.id);
    })
    return Array.from(stations);
}

function getLastStationId(app) {

    if (app.journeys==null) {
        return ""
    }

    var stations = new Set();

    app.journeys.forEach(journeyPlan => {
        const journey = journeyPlan.journey;
        const dest = journey.destination;
        if (dest.locationType=="Station") {
            stations.add(dest.id);
        }
    })

    if (stations.length==1) {
        return stations.pop();
    }

    return "";

}

function getFirstDestinationsFor(app) {

    if (app.journeys==null) {
        return [];
    }

    var stations = new Set();

    app.journeys.forEach(journeyPlan => {
        const journey = journeyPlan.journey;
        const stages = journey.stages;
        if (stages.length>1) {
            journey.changeStations.forEach(changeStation => stations.add(changeStation.id))
        } 
        stations.add(journey.destination.id)
        
    })
    return Array.from(stations);
}

function getLocationType(stop) {
    if (stop==null) {
       return app.myLocation.locationType;
    } else {
        return stop.locationType;
    }
}

function queryLiveData(app, callingStations) {

    var modes;
    var startLocationType = getLocationType(app.startStop);
    if (startLocationType==app.myLocation.locationType) {
        modes = app.selectedModes;
    } else {
        modes = app.startStop.transportModes;
    }

    var startLocationId; 
    const place = app.location;
    if (place != null) {
        if (startLocationType == app.myLocation.locationType) { 
            startLocationId = place.coords.latitude + ',' + place.coords.longitude
        } else {
            startLocationId = app.startStop.id;
        }
    } else {
        // no location available
        startLocationId = app.startStop.id;
    }

    // locations we should request notes for
    var getNotesFor = callingStations.slice();
    getNotesFor.push(startLocationId);

    var journeyList;
    if (app.journeys==null) {
        journeyList = []
    } else {
        journeyList = app.journeys;
    }

    const query = {
        time: app.time,
        locationType: startLocationType,
        locationId: startLocationId,
        notes: true,
        notesFor: getNotesFor,
        modes: modes,
        journeys: journeyList
    }

    axios.post( '/api/departures/location', query, { timeout: 11000 }).
        then(function (response) {
            app.liveDepartureResponse = addParsedDatesToLive(response.data);
            app.notes = response.data.notes;
            app.networkError = false;
            app.liveInProgress = false;
        }).
    catch(function (error) {
        app.liveInProgress = false;
        reportError(error);
    });

}

function getFeedinfo(app) {
    axios.get('/api/datainfo')
        .then(function (response) {
            app.networkError = false;
            app.feedinfo = response.data;
        })
        .catch(function (error) {
            reportError(error);
        });
}

function getTransportModesThenStations(app, beta) {
    var url = '/api/version/config';
    if (beta) {
        url = url + "?beta";
    }
    axios.get(url)
        .then(function (response) {
            app.networkError = false;
            app.modes = response.data.modes;
            app.selectedModes = app.modes; // start with all modes selected
            app.postcodesEnabled = response.data.postcodesEnabled;
            app.numberJourneysToDisplay = response.data.numberJourneysToDisplay;
            getStations(app);
        })
        .catch(function (error) {
            reportError(error);
        });
}

function getStations(app) {
    app.location = null;
    if (app.hasGeo) {
        navigator.geolocation.getCurrentPosition(pos => {
            app.location = pos;
            app.myLocation = {
                name: "My Location", id: pos.coords.latitude + "," + pos.coords.longitude,
                locationType: "MyLocation"
            };
            app.stops.currentLocation.push(app.myLocation);
            getStationsFromServer(app);
        }, err => {
            console.log("Location disabled: " + err.message);
            getStationsFromServer(app);
        });
    } else {
        console.log("Location disabled");
        getStationsFromServer(app);
    }
}

async function getStationsFromServer(app) {

    var gets = [];

    app.modes.forEach(mode => {
        gets.push(axios.get('/api/locations/mode/'+mode));
    });

    if (gets.length==0) {
        console.error("No modes?");
    }

    await Promise.allSettled(gets).then(function(results) {
        app.stops.allStops = new Map();
        results.forEach(result => {
            var receivedStops = result.value.data;
            receivedStops.forEach(stop => app.stops.allStops.set(stop.id, stop))
        });
        app.stops.allStops = Object.freeze(app.stops.allStops); //performance, still needed?
        app.ready = true;
    });

    getRecentAndNearest(app);
    app.networkError = false;
 }

 async function getNearest(app) {
    var place = app.location;

    var gets = [];
    
    app.modes.forEach(mode => {
        const url = '/api/locations/near/'+mode+'?lat=' + place.coords.latitude + '&lon=' + place.coords.longitude;
        gets.push(axios.get(url));
    });

    if (gets.length==0) {
        console.error("No modes?");
    }

    await Promise.allSettled(gets).then(function(results) {
        app.stops.nearestStops = [];
        results.forEach(result => {
            var receivedStops = result.value.data;
            receivedStops.forEach(stop => app.stops.nearestStops.push(stop))
        });
        app.ready = true;
    });

}

async function getRecentAndNearest(app) {
    await axios
        .get('/api/locations/recent?modes='+app.modes)
        .then(function (response) {
            app.networkError = false;
            app.stops.recentStops = response.data;
        })
        .catch(function (error) {
            reportError(error);
        });
    if (app.hasGeo && app.location!=null) {
        getNearest(app);
    }
}

// json parses dates to string
function addParsedDatesToJourney(journeysArray) {
    journeysArray.forEach(item => {
        item.journey.queryDateAsDate = new Date(item.journey.queryDate);
        item.journey.firstDepartureTimeAsDate = new Date(item.journey.firstDepartureTime);
        item.journey.expectedArrivalTimeAsDate = new Date(item.journey.expectedArrivalTime);
    })
    return journeysArray;
}

// json parses dates to string
function addParsedDatesToLive(liveData) {
    liveData.departures.forEach(item => {
        item.dueTimeAsDate = new Date(item.dueTime);
    })
    return liveData;
}

function queryServerForJourneysPost(app, startStop, endStop, queryTime, queryDate, queryArriveBy, changes) {

    var query = { 
        startId: startStop.id, destId: endStop.id, time: queryTime, date: queryDate, 
        arriveBy: queryArriveBy, maxChanges: changes,
        startType: startStop.locationType,
        destType: endStop.locationType,
        modes: app.selectedModes
    };

    axios.post('/api/journey/', query, {timeout: 60000 }).
        then(function (response) {
            app.networkError = false;
            app.journeys = addParsedDatesToJourney(response.data.journeys);
            // get from call to get departures
            //app.notes = response.data.notes;
            getRecentAndNearest(app);
            displayLiveData(app);
            app.searchInProgress = false;
            app.ready = true;
        }).
        catch(function (error) {
            app.ready = true;
            app.searchInProgress = false;
            reportError(error);
        });
}

 function reportError(error) {
    app.networkError = true;
    console.log(error.message);
    console.log("File: " + error.fileName);
    console.log("Line:" + error.lineNumber);
    if (error.request!=null) {
        console.log("URL: " + error.request.responseURL);
    }

 }

 function data() {

    var data = {
        ready: false,                   // ready to respond
        stops: {
            allStops: null,        // (station) id->station
            nearestStops: [],
            recentStops: [],
            currentLocation: []
        },
        startStop: null,
        endStop: null,
        arriveBy: false,
        time: getCurrentTime(),
        date: getCurrentDate(),
        maxChanges: 3,                  // todo from server side
        journeys: null,
        notes: [],
        liveDepartureResponse: null,
        feedinfo: [],
        modes: [],
        selectedModes: [],
        numberJourneysToDisplay: 0,
        searchInProgress: false,    // searching for routes
        liveInProgress: false,      // looking for live data
        networkError: false,        // network error on either query
        hasGeo: false,
        location: null,             // gps locatiion, set if hasGeo
        myLocation: null,           // represents a stop for Current Location, set if hasGeo
        postcodesEnabled: false,
        beta: false,
        timeModal: false, // todo still used?
        cookieDialog: false
    }
    return data;
}

const app = createApp({
        data:  data,
        components: {
            'notes' : Notes,
            'journeys' : Journeys,
            'app-footer' : Footer,
            'live-departures' : LiveDepartures,
            'location-selection': LocationSelection,
            'closures' : Closures,
            'app-header' : Header
        },
        methods: {
            plan(event){
                if (event!=null) {
                    event.preventDefault(); // stop page reload on form submission
                }
                app.searchInProgress = true;
                app.ready = false;
                this.$nextTick(function () {
                    app.queryServer();
                });
            },
            changeTime(newTime) {
                app.time = newTime;
                app.plan(null);
            },
            networkErrorOccured() {
                app.networkError = true;
            },
            queryNearbyTrams() {
                app.liveInProgress = true;
                this.$nextTick(function () {
                    queryLiveData(app,[]);
                });
            },
            queryServer() {
                queryServerForJourneysPost(app, this.startStop, this.endStop, this.time,
                    this.date, this.arriveBy, this.maxChanges);
            },
            timeToNow() {
                app.time = getCurrentTime();
            },
            dateToNow() {
                app.date = getCurrentDate();
            },
            swap() {
                let temp = app.endStop;
                app.endStop = app.startStop;
                app.startStop = temp;
            }
        },
        mounted () {
            getFeedinfo(this);
            let urlParams = new URLSearchParams(window.location.search);
            let betaRaw = urlParams.get('beta');
            let beta = betaRaw != null;
            getTransportModesThenStations(this, beta);
       
        },
        created() {
            if("geolocation" in navigator) {
                this.hasGeo = true;
            }
        },
        computed: {
            havePos: function () {
                return this.hasGeo && (this.location!=null);
            },
            selectChangesEnabled: function() {
                return selectChangesEnabled(this);
            },
            selectModesEnabled: function() {
                return selectModesEnabled(this);
            }
        }
    }).use(vuetify).use(vueCookies).mount('#journeyplan')





const axios = require('axios');

import Vue from 'vue'

import vueCookies from 'vue-cookies'

Vue.use(vueCookies)

require('file-loader?name=[name].[ext]!../index.html');

import vuetify from './plugins/vuetify' // from file in plugins dir
import '@mdi/font/css/materialdesignicons.css'

// todo move into require above to get auto min or full version?

// todo still needed?
import 'jquery/dist/jquery.slim.js'

import './../css/tramchester.css'

import Notes from "./components/Notes";
import Journeys from './components/Journeys';
import Footer from './components/Footer';
import LiveDepartures from './components/LiveDepatures'
import LocationSelection from './components/LocationSelection';
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

function displayLiveData(app) {
    const queryDate = new Date(app.date); 
    const today = getNow();
    // check live data for today only 
    if (today.getMonth()==queryDate.getMonth()
        && today.getYear()==queryDate.getYear()
        && today.getDate()==queryDate.getDate()) {
        queryLiveData(app, true);
    } else {
        app.liveDepartureResponse = null;
    }
}

function queryLiveData(app, includeNotes) {

    var modes;
    var locationType;
    if (app.startStop==null) {
        locationType = 'MyLocation';
        modes = app.selectedModes;
    } else {
        locationType = app.startStop.locationType;
        modes = app.startStop.transportModes;
    }

    var locationId; // = app.startStop.id;
    if (app.myLocation != null) {
        if (locationType == app.myLocation.locationType) { 
            const place = app.location; // should not have location place holder without a valid location
            locationId = place.coords.latitude + ',' + place.coords.longitude
        } else {
            locationId = app.startStop.id;
        }
    } else {
        locationId = app.startStop.id;
    }

    const query = {
        time: app.time,
        locationType: locationType,
        locationId: locationId,
        notes: includeNotes,
        modes: modes
    }

    axios.post( '/api/departures/location', query, { timeout: 11000 }).
        then(function (response) {
            app.liveDepartureResponse = addParsedDatesToLive(response.data);
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
    var url = '/api/version/modes';
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
    if (app.postcodesEnabled) {
        gets.push(axios.get("/api/postcodes", { timeout: 30000}));
    }
    app.modes.forEach(mode => {
        gets.push(axios.get('/api/stations/mode/'+mode));
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
        const url = '/api/stations/near/'+mode+'?lat=' + place.coords.latitude + '&lon=' + place.coords.longitude;
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
        .get('/api/stations/recent?modes='+app.modes)
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
            getRecentAndNearest(app);
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
    cookieDialog: false,
    disclaimerDialog: false
}

var app = new Vue({
        vuetify,
        data:  data,
        components: {
            'notes' : Notes,
            'journeys' : Journeys,
            'app-footer' : Footer,
            'live-departures' : LiveDepartures,
            'location-selection': LocationSelection,
            'closures' : Closures,
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
                    queryLiveData(app, false);
                });
            },
            queryServer() {
                queryServerForJourneysPost(app, this.startStop, this.endStop, this.time,
                    this.date, this.arriveBy, this.maxChanges);
                displayLiveData(app);
            },
            setCookie() {
                var cookie = { 'visited' : true };
                this.$cookies.set("tramchesterVisited", cookie, "128d", "/", null, false, "Strict");
                app.cookieDialog = false
            },
            showDisclaimer() {
                app.disclaimerDialog = true
            },
            dismissDisclaimer() {
                app.disclaimerDialog = false
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
            var cookie = this.$cookies.get("tramchesterVisited");
            if (cookie==null) {
                this.cookieDialog = true
                // var modal = new bootstrap.Modal(this.$refs.cookieModal,{});
                // modal.show();
            }
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
    }).$mount('#journeyplan')




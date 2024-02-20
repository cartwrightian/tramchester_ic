
const axios = require('axios');

import Vue from 'vue'

var L = require('leaflet');
import 'leaflet-arrowheads'


require('file-loader?name=[name].[ext]!../diag.html');

import vuetify from './plugins/vuetify' // from file in plugins dir
import '@mdi/font/css/materialdesignicons.css'

import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

import Notes from "./components/Notes";
import Journeys from './components/Journeys';
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
            app.numberJourneysToDisplay = response.data.numberJourneysToDisplay;
            getStations(app);
        })
        .catch(function (error) {
            reportError(error);
        });
}

function getStations(app) {
    app.location = null;
    console.log("Location disabled");
    getStationsFromServer(app);
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

function reasonsToText(reasons) {
    var result = "";
    reasons.forEach(reason => {
        result = result + reason.text + "<br>"
    })
    return result;
}

function addLinks(node, layer, origin, maxEdgeReasons) {
    
    const linksToDisplay = node.links;
    linksToDisplay.forEach(link => {
        var steps = [];
        steps.push([origin.latLong.lat, origin.latLong.lon]);
        steps.push([link.towards.latLong.lat, link.towards.latLong.lon]);
        const lineWidth = (node.reasons.length / maxEdgeReasons) * 20;
        const line = L.polyline(steps);
    
        line.setStyle({color: "red", opacity: 0.6, weight: lineWidth })

        line.bindTooltip(reasonsToText(link.reasons));       
        layer.addLayer(line);
    });

}

function addNodeToMap(node, app, maxNodeReasons, maxEdgeReasons) {
    const station = node.begin;
    const lat = station.latLong.lat;
    const lon = station.latLong.lon;
    const location = L.latLng(lat, lon);
    const radius = (node.reasons.length / maxNodeReasons) * 20;
    const marker = new L.circleMarker(location, 
        { fill: true, fillColor: "LightBlue", fillOpacity: 0.8, title: station.name, radius: radius, color: "LightBlue" });

    var stationText = station.name + " '" + station.id + "<br>";
    stationText = stationText + reasonsToText(node.reasons);
    marker.bindTooltip(stationText);

    app.stationLayerGroup.addLayer(marker);

    addLinks(node, app.edgesLayerGroup, station, maxEdgeReasons)

}

function updateMapWithDiag(app, diagnostics) {
    if (diagnostics==null) {
        return;
    }

    const nodes = diagnostics.dtoList;
    const maxNodeReasons = new Number(diagnostics.maxNodeReasons);
    const maxEdgeReasons = new Number(diagnostics.maxEdgeReasons);
    nodes.forEach(node => {
        addNodeToMap(node, app, maxNodeReasons, maxEdgeReasons)
    })

}

function queryServerForJourneysPost(app, startStop, endStop, queryTime, queryDate, queryArriveBy, changes) {

    var query = { 
        startId: startStop.id, destId: endStop.id, time: queryTime, date: queryDate, 
        arriveBy: queryArriveBy, maxChanges: changes,
        startType: startStop.locationType,
        destType: endStop.locationType,
        modes: app.selectedModes,
        diagnostics: true
    };

    axios.post('/api/journey/', query, {timeout: 60000 }).
        then(function (response) {
            app.networkError = false;
            app.journeys = addParsedDatesToJourney(response.data.journeys);
            updateMapWithDiag(app, response.data.diagnostics);
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
    notes: [],
    modes: [],
    selectedModes: [],
    numberJourneysToDisplay: 0,
    searchInProgress: false,    // searching for routes
    networkError: false,        // network error on either query
    myLocation: null,           // represents a stop for Current Location, set if hasGeo
    beta: false,
    timeModal: false, // todo still used?
    map: null,
    bounds: null,
    stationLayerGroup: null,
    edgesLayerGroup: null
}

var app = new Vue({
        vuetify,
        data:  data,
        components: {
            'notes' : Notes,
            'journeys' : Journeys,
            'location-selection': LocationSelection,
            'closures' : Closures,
        },
        methods: {
            findAndSetMapBounds: function(map, bounds) {
                var bottomLeft = bounds.bottomLeft;
                var topRight = bounds.topRight;
            
                var corner1 = L.latLng(bottomLeft.lat, bottomLeft.lon);
                var corner2 = L.latLng(topRight.lat, topRight.lon);
                var bounds = L.latLngBounds(corner1, corner2);
                map.fitBounds(bounds);
            },
            addBounds: function(map, bounds) {
                const boundsLayer = L.layerGroup();
                const bottomLeft = bounds.bottomLeft;
                const topRight = bounds.topRight;
                const boundsForRect = [ [bottomLeft.lat, bottomLeft.lon], [topRight.lat, topRight.lon] ];
                const box = L.rectangle(boundsForRect, { color: "blue", stroke: true, weight: 1, fill: false });
                boundsLayer.addLayer(box);
                boundsLayer.addTo(map);
            },
            draw() {
                const map = app.map;

                app.findAndSetMapBounds(map, app.bounds);
            
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                }).addTo(map);
    
                app.stationLayerGroup = L.layerGroup();
                app.edgesLayerGroup = L.layerGroup();

                app.addBounds(map, app.bounds);
                app.edgesLayerGroup.addTo(map);
                app.stationLayerGroup.addTo(map);
            },
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
            queryServer() {
                app.stationLayerGroup.clearLayers();
                app.edgesLayerGroup.clearLayers();
                queryServerForJourneysPost(app, this.startStop, this.endStop, this.time,
                    this.date, this.arriveBy, this.maxChanges);
            },
            timeToNow() {
                app.time = getCurrentTime();
            },
            dateToNow() {
                app.date = getCurrentDate();
            }
        },
        mounted () {
            this.map = L.map('leafletMap');

            let urlParams = new URLSearchParams(window.location.search);
            let betaRaw = urlParams.get('beta');
            let beta = betaRaw != null;
            getTransportModesThenStations(this, beta);

            axios.all([
                axios.get("/api/geo/quadrants"),
                axios.get("/api/geo/bounds"),
                axios.get("/api/geo/areas"),
            ]).then(axios.spread((quadResp, boundsResp, areasResp) => {
                    app.networkError = false;
                    //app.quadrants = quadResp.data;
                    app.bounds = boundsResp.data;
                    //app.areas = areasResp.data;
                    app.draw();
                })).catch(error => {
                    app.networkError = true;
                    console.log(error);
                });
       
        },
        created() {
            // if("geolocation" in navigator) {
            //     this.hasGeo = true;
            // }
        },
        computed: {
            havePos: function () {
                return false;
            },
            selectChangesEnabled: function() {
                return selectChangesEnabled(this);
            },
            selectModesEnabled: function() {
                return selectModesEnabled(this);
            }
        }
    }).$mount('#journeyplan')




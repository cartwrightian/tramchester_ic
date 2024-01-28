
const axios = require('axios');
const oboe = require('oboe');

import Vue from 'vue'

import vueCookies from 'vue-cookies'

Vue.use(vueCookies)

var L = require('leaflet');

require('file-loader?name=[name].[ext]!../traveltimes.html');

import vuetify from './plugins/vuetify' // from file in plugins dir
import '@mdi/font/css/materialdesignicons.css'

import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';
import Routes from './components/Routes';

function getCurrentDate() {
    const now = new Date().toISOString();
    return now.substr(0,  now.indexOf("T")); // iso-8601 date part only as YYYY-MM-DD
}

function boxClicked(event) {
    mapApp.journeyLayer.clearLayers();
    var journey = this.journey.journey;
    var steps = [];

    journey.path.forEach(item => {
        steps.push([item.latLong.lat, item.latLong.lon]);
    })
    var line = L.polyline( steps, { color: 'red' });
    mapApp.journeyLayer.addLayer(line);
    mapApp.journeyLayer.addTo(mapApp.map);
}

function addBoxWithCost(boxWithCost) {
    const bounds = [[boxWithCost.bottomLeft.lat, boxWithCost.bottomLeft.lon], 
        [boxWithCost.topRight.lat, boxWithCost.topRight.lon]];

    const colour = getColourForCost(boxWithCost);     
    const rectangle = L.rectangle(bounds, {weight: 1, color: colour, fillColor: colour, fill: true, fillOpacity: 0.5});
    if (boxWithCost.minutes>0) {
        rectangle.bindTooltip('cost ' + boxWithCost.minutes);
        rectangle.on('click', boxClicked, boxWithCost);
    }
    rectangle.addTo(mapApp.costsLayer);
}

function getColourForCost(boxWithCost) {
    if (boxWithCost.minutes==0) {
        return "#0000ff";
    }
    if (boxWithCost.minutes < 0) {
        return "#ff0000";
    }
    var greenString = "00";
    if (boxWithCost.minutes > 0) {
        const red = Math.floor((255 / 112) * (112 - boxWithCost.minutes));
        greenString = red.toString(16);
        if (greenString.length == 1) {
            greenString = '0' + greenString;
        }
    }
    return '#00'+greenString+'00';
}

function queryForGrid(gridSize, destinationType, stationId, departureTime, departureDate, maxChanges, maxDuration) {
    var query = {
            destType: destinationType, destId: stationId, gridSize: gridSize, departureTime: departureTime, departureDate: departureDate, 
            maxChanges: maxChanges, 
            maxDuration: maxDuration}

    queryServerForGrid(query);
}

// TODO Oboe is now unmaintained, but axios can't (won't?) support streaming, need an alternative
// https://github.com/axios/axios/issues/479
function queryServerForGrid(query) {     
    oboe({
        'url': '/api/grid/query',
        'method': 'POST',
        'body' : query
    }).node('BoxWithCost', function (box) {
        addBoxWithCost(box);
    })
    .fail(function (errorReport) {
        console.log("Failed to load grid '" + errorReport.toString() + "'");
    });
}

function getTransportModesThenStations(app) {
    var url = '/api/version/modes';

    axios.get(url)
        .then(function (response) {
            app.networkError = false;
            app.modes = response.data.modes;
            getStationsFromServer(app);
        })
        .catch(function (error) {
            reportError(error);
        });
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
        app.allStops = [];
        results.forEach(result => {
            var receivedStops = result.value.data;
            receivedStops.forEach(stop => app.allStops.push(stop))
        });
        app.ready = true;
    });

    app.networkError = false;
 }

 var data = {
    modes: [],
    allStops: [],
    map: null,
    grid: null,
    destination: null,
    journeyLayer: null,
    costsLayer: null,
    networkError: false,
    routes: [],
    maxChanges: 2,
    feedinfo: []
 }

var mapApp = new Vue({
    vuetify,
    components: {
        'app-footer' : Footer
    },
    data: data,
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        draw() {
            Routes.findAndSetMapBounds(mapApp.map, mapApp.routes);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            mapApp.journeyLayer = L.featureGroup();
            mapApp.costsLayer = L.featureGroup();
            mapApp.costsLayer.addTo(mapApp.map);
        },
        compute() {
            mapApp.costsLayer.clearLayers();
            queryForGrid(1000, mapApp.destination.locationType, mapApp.destination.id , "07:30", getCurrentDate(), 
                mapApp.maxChanges, "60");
        }
    },
    mounted () {
        this.map = L.map('leafletMap');

        getTransportModesThenStations(this);

        axios.get('/api/datainfo')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.feedinfo = response.data;
            }).catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });

        axios.get("/api/routes")
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.routes = response.data;
                mapApp.draw();
            }).catch(function (error){
                mapApp.networkError = true;
                console.log(error);
            });
    }, 
    computed: {
        havePos: function () {
            return false; // needed for display in footer
        }
    }
}).$mount('#tramMap')






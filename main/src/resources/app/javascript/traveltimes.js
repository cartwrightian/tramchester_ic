import { createApp, ref } from 'vue'

import vueCookies from 'vue-cookies'

import 'vuetify/styles'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { createVuetify } from 'vuetify'
const vuetify = createVuetify({components, directives})

const axios = require('axios');

var L = require('leaflet');

require('file-loader?name=[name].[ext]!../traveltimes.html');

import '@mdi/font/css/materialdesignicons.css'

import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';
import Header from './components/Header';
import Routes from './components/Routes';

function getCurrentDate() {
    const now = new Date().toISOString();
    return now.substring(0,  now.indexOf("T")); // iso-8601 date part only as YYYY-MM-DD
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
}

function addBoxWithCost(boxWithCost) {
    if (boxWithCost==null) {
        console.error("empty box received");
        return;
    }
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

function queryForGrid(app, gridSize, destinationType, stationId, time, date, maxChanges, maxDuration) {
    var query = {
            destType: destinationType, destId: stationId, gridSize: gridSize, departureTime: time, 
            departureDate: date, 
            maxChanges: maxChanges, 
            maxDuration: maxDuration}

    queryServerForGrid(query, app);
}


async function queryServerForGrid(query,app) {
    const queryJSON = JSON.stringify(query);
    const headers = new Headers({"Content-Type" : "application/json"});
    const request = new Request('/api/grid/chunked', {
            method: 'POST',
            headers: headers,
            body: queryJSON
    });

    if (app.abortController!=null) {
        app.abortController.abort();
    }

    app.abortController = new AbortController();
    const signal = app.abortController.signal;
    const response = fetch(request, { signal });
    (await response).body.pipeThrough(new TextDecoderStream()).
        pipeThrough(bufferedLines()).
        pipeThrough(parseJSON()).
        pipeTo(createBox());
}

const createBox = () => {
    return new WritableStream({
        write(item) {
            addBoxWithCost(item.BoxWithCost);
        }
    });
}

const bufferedLines = () => {
    const newLine = '\n';
    var buffer = '';
    return new TransformStream({
      transform(textChunk, controller) {
        buffer += textChunk;
        const lines = buffer.split(newLine);
        lines.slice(0, -1).forEach((line) => controller.enqueue(line));
        const leftOver = lines[lines.length - 1];
        buffer = leftOver;
      },
      flush(controller) {
        if (buffer.length>0) {
            controller.enqueue(buffer);
        }
      }
    });
  };

  const parseJSON = () => {
    return new TransformStream({
      transform(line, controller) {
        if (line.length==0) {
            // skip newline
        } else {
            controller.enqueue(JSON.parse(line));
        }
      }
    });
  };

function getTransportModesThenStations(app) {
    var url = '/api/version/config';

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

 function data() {
    return {
        modes: [],
        allStops: [],
        map: null,
        grid: null,
        date: getCurrentDate(),
        destination: null,
        journeyLayer: null,
        costsLayer: null,
        networkError: false,
        routes: [],
        maxChanges: 2,
        maxDuration: 60,
        time: "09:30",
        abortController: null,
        feedinfo: []
    }
}

var mapApp = createApp({
    vuetify,
    components: {
        'app-footer' : Footer,
        'app-header' : Header
    },
    data: data,
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        displayStop(stop) {
            return { title: stop.name }
        },
        draw() {
            Routes.findAndSetMapBounds(mapApp.map, mapApp.routes);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            mapApp.costsLayer = L.featureGroup();
            mapApp.costsLayer.addTo(mapApp.map);
            mapApp.journeyLayer = L.featureGroup();
            mapApp.journeyLayer.addTo(mapApp.map);
        },
        compute(event) {
            //this.cancel();
            if (event!=null) {
                event.preventDefault(); // stop page reload on form submission
            }
            mapApp.costsLayer.clearLayers();
            mapApp.journeyLayer.clearLayers();
            this.$nextTick(function () {
                queryForGrid(mapApp, 1000, mapApp.destination.locationType, mapApp.destination.id , mapApp.time, 
                    mapApp.date, 
                    mapApp.maxChanges, mapApp.maxDuration);
            });
        },
        dateToNow() {
            mapApp.date = getCurrentDate();
        },
        cancel() {
            if (mapApp.abortController!=null) {
                mapApp.abortController.abort();
            }
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
}).use(vueCookies).use(vuetify).mount('#tramMap')






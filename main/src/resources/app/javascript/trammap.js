
const axios = require('axios');

import { createApp, ref } from 'vue'

import vueCookies from 'vue-cookies'

import 'vuetify/styles'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { createVuetify } from 'vuetify'
const vuetify = createVuetify({components, directives})

var L = require('leaflet');
require('leaflet-arrowheads')
require('leaflet-polylineoffset')

require('file-loader?name=[name].[ext]!../trammap.html');

import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Routes from './components/Routes'
import Footer from './components/Footer'

function addStations() {
    mapApp.routes.forEach(route => {
        var stationIcon = L.divIcon({className: 'station-icon '+ Routes.classForRoute(route), iconSize:[12,12]});
        addStationsForRoute(route, stationIcon);
    })
}

function addStationsForRoute(route, stationIcon) {
    var stationLayerGroup = L.featureGroup();

    if (route.tram) {
        route.stations.forEach(station => {
            var lat = station.latLong.lat;
            var lon = station.latLong.lon;
            var marker = new L.marker(L.latLng(lat,lon), { title: station.name, icon: stationIcon })
            stationLayerGroup.addLayer(marker);
        });
    }

    stationLayerGroup.addTo(mapApp.map);
}

function refreshTrams() {
    axios.get('/api/positions')
        .then(function (response) {
            mapApp.networkError = false;
            mapApp.positionsList = response.data.positionsList;
            addTrams();
        }).catch(function (error) {
            mapApp.networkError = true;
            console.log(error);
        });
}

function addTrams() {
   
    mapApp.tramLayerGroup.clearLayers();
    mapApp.positionsList.forEach(position => {
        var latBegin = position.first.latLong.lat;
        var lonBegin = position.first.latLong.lon;
        var latNext = position.second.latLong.lat;
        var lonNext = position.second.latLong.lon;
        
        // unit vector based on cost between the stations
        var vectorLat = (latNext-latBegin)/position.cost;
        var vectorLon = (lonNext-lonBegin)/position.cost;

        position.trams.forEach(tram => {
            var dist = (position.cost-tram.wait); // approx current dist travelled based on wait at next station
            var latCurrent = latBegin + ( dist * vectorLat );
            var lonCurrent = lonBegin + ( dist * vectorLon );
            if (tram.wait>0) {
                var latTorwards = latCurrent + (vectorLat*0.8);
                var lonTowards = lonCurrent + (vectorLon*0.8);
                var linePoints = [ [latCurrent,lonCurrent] , [latTorwards,lonTowards] ];

                var line = L.polyline(linePoints, { color: 'black', opacity: 0.6, weight: 4, pane: mapApp.tramPane}).
                    arrowheads({ opacity: 1, fill: false, size: '8px', pane: mapApp.tramPane, yaw: 90 });
                line.bindTooltip(getTramTitle(tram, position));
                mapApp.tramLayerGroup.addLayer(line);
            } else {
                var circle = L.circle([latCurrent, lonCurrent], {radius: 50, color: 'black', weight: 2, pane: mapApp.tramPane});
                circle.bindTooltip(getTramTitle(tram, position));
                mapApp.tramLayerGroup.addLayer(circle);
            }
        })
        mapApp.tramLayerGroup.addTo(mapApp.map);
    });
}

function getTramTitle(tram, position) {
    if (tram.status==='Arrived') {
        return tram.destination + ' tram at ' + position.second.name;
    } else {
        return tram.destination + ' tram ' + tram.status + ' at ' + position.second.name + ' in ' + tram.wait;
    }
}

const mapApp = createApp({
    components: {
        'app-footer' : Footer
    },
    data() {
        return {
            map: null,
            positionsList: null,
            networkError: false,
            routes: [],
            postcodes: [],
            tramLayerGroup: null,
            feedinfo: [],
            tramPane: null
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        draw() {
            Routes.findAndSetMapBounds(mapApp.map, mapApp.routes);
            mapApp.tramPane = mapApp.map.createPane("tramPane");
            mapApp.tramPane.style.zIndex = 610; // above marker plane, below popups and tooltips
            mapApp.tramLayerGroup = L.featureGroup();
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            Routes.addRoutes(mapApp.map,mapApp.routes);
            addStations();
            refreshTrams();
            setInterval(function() {
                refreshTrams();
            }, 10 * 1000);
        }
    },
    mounted () {
        this.map = L.map('leafletMap');

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
}).use(vueCookies).use(vuetify).mount('#tramMap');



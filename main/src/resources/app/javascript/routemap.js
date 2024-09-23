
const axios = require('axios');

import { createApp, ref } from 'vue'

import vueCookies from 'vue-cookies'

import 'vuetify/styles'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { createVuetify } from 'vuetify'
const vuetify = createVuetify({components, directives})

var L = require('leaflet');

require('file-loader?name=[name].[ext]!../routes.html');

import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'
import 'leaflet-polylineoffset/leaflet.polylineoffset'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Routes from './components/Routes';
import Footer from './components/Footer';
import Header from './components/Header';

function getCurrentDate() {
    const now = new Date().toISOString();
    return now.substr(0,  now.indexOf("T")); // iso-8601 date part only as YYYY-MM-DD
}

var mapApp = createApp({
    components: {
        'app-footer' : Footer,
        'app-header' : Header
    },
    data() {
        return {
            map: null,
            networkError: false,
            routes: [],
            feedinfo: [],
            date: getCurrentDate(),
            hours: [ 10, 11],
            routesLayer: null,
            stationsLayer: null
        }
    },
    methods: {
        update() {
            mapApp.map.removeLayer(mapApp.routesLayer);
            mapApp.map.removeLayer(mapApp.stationsLayer);
            getRoutes(mapApp);
        },
        draw() {
            //getRoutes(mapApp); done by callers
            Routes.findAndSetMapBounds(mapApp.map, mapApp.routes);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            mapApp.routesLayer = Routes.addRoutes(mapApp.map, mapApp.routes);
            mapApp.stationsLayer = Routes.addStations(mapApp.map, mapApp.routes);
        }, 
        dateToNow() {
            mapApp.date = getCurrentDate();
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

        getRoutes(this);
    }, 
    computed: {
        havePos: function () {
            return false; // needed for display in footer
        }
    }
}).use(vueCookies).use(vuetify).mount('#routeMap');


function getRoutes(mapApp) {
    axios.get("/api/routes/filtered?date="+mapApp.date)
        .then(function (response) {
            mapApp.networkError = false;
            mapApp.routes = response.data;
            mapApp.draw();
        }).catch(function (error) {
            mapApp.networkError = true;
            console.log(error);
        });
}


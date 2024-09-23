
const axios = require('axios');

import { createApp, ref } from 'vue'

import vueCookies from 'vue-cookies'

import 'vuetify/styles'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { createVuetify } from 'vuetify'
const vuetify = createVuetify({components, directives})

var L = require('leaflet');
import 'leaflet-arrowheads'

require('file-loader?name=[name].[ext]!../links.html');

import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';
import Header from './components/Header';

function getColourFor(station) {
    const modes = station.transportModes;
    if (modes.length>1) {
        return "purple";
    }
    const mode = modes[0];
    switch(mode) {
        case "Bus": return "Green";
        case "Train": return "DarkBlue";
        case "Tram": return "LightBlue";
        default: return "Orange";
    }
}

function display(boundary) {
    const bottomLeft = boundary.bottomLeft;
    const topRight = boundary.topRight;
    const prec = 8;
    return "<br>[("
        +bottomLeft.lat.toFixed(prec)+","+bottomLeft.lon.toFixed(prec)+"),("
        +topRight.lat.toFixed(prec)+","+topRight.lon.toFixed(prec)
        +")]";
}

function addStationToMap(station, stationLayerGroup, isInterchange) {
    const lat = station.latLong.lat;
    const lon = station.latLong.lon;
    const colour = getColourFor(station);

    var marker;
    if (isInterchange) {
        // todo really want a different kind of marker here
        marker = new L.circleMarker(L.latLng(lat, lon), { title: station.name, radius: 3, color: colour });
    } else {
        marker = new L.circleMarker(L.latLng(lat, lon), { title: station.name, radius: 1, color: colour });
    }
    var stationText = station.name + "<br> '" + station.id + "' (" + station.transportModes + ")";
    if (isInterchange) {
        stationText = stationText + "<br>interchange"
    }
    if (station.isMarkedInterchange) {
        stationText = stationText + "<br>marked interchange at source"
    }
    marker.bindTooltip(stationText);

    //station.platforms.forEach(platform => addPlatformsToMap(platform, stationLayerGroup));

    stationLayerGroup.addLayer(marker);
}

// function addPlatformsToMap(platform, stationLayerGroup) {
//     const lat = platform.latLong.lat;
//     const lon = platform.latLong.lon;

//     var marker = new L.circleMarker(L.latLng(lat, lon), { title: platform.name, radius: 1, color: "black" });
//     marker.bindTooltip("Platform " +platform.id+ "<br>Name " + platform.name);
//     stationLayerGroup.addLayer(marker);
// }

function createPolyForArea(area, colour) {
    const boundary = area.points;
    var points = [];
    boundary.forEach(latLong => points.push([latLong.lat, latLong.lon]));
    var polygon = L.polygon(points, { stroke: true, weight: 1, fill: true, fillOpacity: 0.2, color: colour });
    return polygon;
}

var mapApp = createApp({
    //el: '#routeMap',
    components: {
        'app-footer' : Footer,
        'app-header' : Header
    },
    data() {
        return {
            map: null,
            networkError: false,
            neighbours: [],
            quadrants: [],
            feedinfo: [],
            areas: [],
            stations: [],
            interchanges: [], // list of station id's
            stationsBoundary: [],
            links: [],
            bounds: null
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        addStations: function(map, stations, interchanges) {
            var stationLayerGroup = L.layerGroup();
            stations.forEach(station => {
                const isInterchange = interchanges.includes(station.id);
                addStationToMap(station, stationLayerGroup, isInterchange);
            })
            stationLayerGroup.addTo(map);
        },
        addAreas: function(map, areas) {
            var areaLayerGroup = L.layerGroup();
            
            areas.forEach(area => {
                var polygon = createPolyForArea(area, "purple");
                const areaId = area.areaId;
                polygon.bindTooltip("area " + areaId + "<br> " + area.areaName, { sticky: true}); // + "<br>" + area.type);
                areaLayerGroup.addLayer(polygon);
            })

            areaLayerGroup.addTo(map);
        },
        addStationsBoundary: function(map, area) {
            var polygon = createPolyForArea(area, "red");
            polygon.bindTooltip("loaded stations boundary", { sticky: true});
            polygon.addTo(map);
        },
        addGroups: function(map, groups) {
            const groupLayer = L.layerGroup();
            groups.forEach(group => {
                const steps = [];
                group.contained.forEach(station => {
                    steps.push([station.latLong.lat, station.latLong.lon]);
                })
                const first = group.contained[0];
                steps.push([first.latLong.lat, first.latLong.lon]);
                const shape = L.polygon(steps);
                shape.setStyle({color: "pink", opacity: 0.7, fill: true, fillOpacity: 0.5});
                groupLayer.addLayer(shape);
            })
            groupLayer.addTo(map);
        },
        addQuadrants: function(map, quadrants) {
            const quadrantLayer = L.layerGroup();
            quadrants.forEach(quadrant => {
                const bottomLeft = quadrant.bottomLeft;
                const topRight = quadrant.topRight;
                const bounds = [ [bottomLeft.lat, bottomLeft.lon], [topRight.lat, topRight.lon] ];
                const box = L.rectangle(bounds, { color: "black", stroke: false });
                box.bindTooltip("quadrant" + display(quadrant));
                quadrantLayer.addLayer(box);
            });
            quadrantLayer.addTo(map);
        },
        addBounds: function(map, bounds) {
            const boundsLayer = L.layerGroup();
            const bottomLeft = bounds.bottomLeft;
            const topRight = bounds.topRight;
            const boundsForRect = [ [bottomLeft.lat, bottomLeft.lon], [topRight.lat, topRight.lon] ];
            const box = L.rectangle(boundsForRect, { color: "blue", stroke: true, weight: 1, fill: false });
            box.bindTooltip("config bounds " + display(bounds), { sticky: true});
            boundsLayer.addLayer(box);
            boundsLayer.addTo(map);
        },
        addLinks: function(map, linksToDisplay) {
            var linkLayerGroup = L.layerGroup();
            
            linksToDisplay.forEach(link => {
                var steps = [];
                steps.push([link.begin.latLong.lat, link.begin.latLong.lon]);
                steps.push([link.end.latLong.lat, link.end.latLong.lon]);
                var line = L.polyline(steps); // hurts performance .arrowheads({ size: '5px', frequency: 'endonly' });
                if (link.linkType==="Linked") {
                    line.bindTooltip("Link " + link.begin.id + " and " + link.end.id + "<br> " + link.transportModes, 
                        { sticky: true});
                    line.setStyle({color: "blue", opacity: 0.3});
                } else if (link.linkType==="Neighbours") {
                    line.bindTooltip("Neighbours " + link.begin.id + " and " + link.end.id + "<br> " + link.transportModes, 
                        { sticky: true});
                    line.setStyle({color: "yellow", opacity: 0.6});
                }
                linkLayerGroup.addLayer(line);
            });

            linkLayerGroup.addTo(map);
        },
        findAndSetMapBounds: function(map, bounds) {
            var bottomLeft = bounds.bottomLeft;
            var topRight = bounds.topRight;
        
            var corner1 = L.latLng(bottomLeft.lat, bottomLeft.lon);
            var corner2 = L.latLng(topRight.lat, topRight.lon);
            var bounds = L.latLngBounds(corner1, corner2);
            map.fitBounds(bounds);
        },
        draw() {
            const map = mapApp.map;
            mapApp.findAndSetMapBounds(map, mapApp.bounds);
            
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(map);

            mapApp.addBounds(map, mapApp.bounds);
            mapApp.addStationsBoundary(map, mapApp.stationsBoundary);
            mapApp.addQuadrants(map, mapApp.quadrants);
            mapApp.addAreas(map, mapApp.areas);
            mapApp.addLinks(map, mapApp.neighbours);
            mapApp.addLinks(map, mapApp.links);
            mapApp.addStations(map, mapApp.stations, mapApp.interchanges);
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

        axios.all([
            axios.get("/api/geo/neighbours"),
            axios.get("/api/geo/quadrants"),
            axios.get("/api/geo/bounds"),
            axios.get("/api/geo/areas"),
            axios.get("/api/stations/all"),
            axios.get("/api/interchanges/all"),
            axios.get("/api/geo/stationsboundary"),
            axios.get("/api/geo/links")
        ]).then(axios.spread((neighboursResp, quadResp, boundsResp, areasResp, stationsResp, interchangeResp, 
            stationBounadryResp, linksResp) => {
                mapApp.networkError = false;
                mapApp.neighbours = neighboursResp.data;
                mapApp.quadrants = quadResp.data;
                mapApp.bounds = boundsResp.data;
                mapApp.areas = areasResp.data;
                mapApp.stations = stationsResp.data;
                mapApp.interchanges = interchangeResp.data;
                mapApp.stationsBoundary = stationBounadryResp.data;
                mapApp.links = linksResp.data;
                mapApp.draw();
            })).catch(error => {
                mapApp.networkError = true;
                console.log(error);
            });

    }, 
    computed: {
        havePos: function () {
            return false; // needed for display in footer
        }
    }
}).use(vueCookies).use(vuetify).mount('#linksMap');




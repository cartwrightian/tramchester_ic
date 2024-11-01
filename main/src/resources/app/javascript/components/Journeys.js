

function diffInDays(dateA, dateB) {
    const justDateA = new Date(dateA.toDateString());
    const justDateB = new Date(dateB.toDateString());
    const diffTime = Math.abs(justDateB - justDateA); // millis
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24)); 
    return diffDays;
}

function formatDate(queryDate, journeyDateTime) {
    const time = toHourAndMins(journeyDateTime); 
    // next day?
    //var diff = daysSinceEpoch(journeyDateTime) - daysSinceEpoch(queryDate);
    const diff = diffInDays(queryDate, journeyDateTime);
    if (diff>0) {
        return time + ' +' + diff + 'd';
    }
    return time;
}

function earliestDepartTime(journeys) {

    var earliestDepart = null;
    journeys.forEach(item => {
        var currnet = item.journey.firstDepartureTimeAsDate;
        if (earliestDepart==null) {
            earliestDepart = currnet;
        }
        if (currnet < earliestDepart) {
            earliestDepart = currnet;
        }
    })
    return earliestDepart;
}

function getKeyFromChanges(changeStations) {
    var result = "";
    changeStations.forEach(station => result = result + station.id);
    return result;
}

function lastDepartTime(journeys) {

    var lastDepart = null;
    journeys.forEach(item => {
        const currnet = item.journey.firstDepartureTimeAsDate;
        if (lastDepart==null) {
            lastDepart = currnet;
        }
        if (currnet > lastDepart) {
            lastDepart = currnet;
        }
    })
    return lastDepart;
}

function timeSort(a,b) {
    if (a>b) {
        return 1;
    }
    if (b>a) {
        return -1;
    }
    return 0;
}

function toHourAndMins(date) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
}

export default { 
    data: function () {
        return {
            currentPage: 1,
            headers: [
                {key: 'data-table-expand', title:'' },
                {key: 'journey.firstDepartureTimeAsDate',title:'Depart', sortable:true, sort: (a,b) => { return timeSort(a,b) } },
                {key: 'journey.begin', title:'From', sortable:true}, 
                {key: 'journey.expectedArrivalTimeAsDate', title:'Arrive', sortable:true, sort: (a,b) => { return timeSort(a,b) } }, 
                {key: 'journey.changeStations', title:'Change' }
                ],
            stageHeaders : [
                { key: 'firstDepartureTime', title: '' , sortable:false }, 
                { key: 'action', title: '' , sortable:false }, 
                { key: 'actionStation', title: 'Station' , sortable:false }, 
                { key: 'platform.platformNumber', title: 'Plat', sortable:false }, 
                { key: 'headSign', title: 'Toward' , sortable:false }, 
                { key: 'passedStops', title: 'Stops', sortable:false  }, 
                { key: 'expectedArrivalTime', title: 'Arrive' , sortable:false } 
                ],

            expanded: [],
            sortBy: [{ key: 'journey.expectedArrivalTimeAsDate', order: 'asc'}]
        }
      },
    props: ['journeysresponse','numjourneystodisplay'],
    computed: { 
        journeys: function() {
            if (this.journeysresponse==null) {
                return [];
            }
            var seen = [];
            var result = []
            this.journeysresponse.forEach(item => {
                var journey = item.journey;
                var depart = journey.firstDepartureTime;
                var arrive = journey.expectedArrivalTime;
                var changes = getKeyFromChanges(journey.changeStations);
                var key = depart+"_"+arrive+"_"+changes;
                if (!seen.includes(key)) {
                    result.push(item);
                    seen.push(key);
                }

            })
            return result;
        },
        noJourneys: function() {
            if (this.journeysresponse==null) {
                return false; // no query has been done
            }
            return this.journeysresponse.length==0;
        },
        itemsPerPage: function() {
            return this.numjourneystodisplay;
        }
    },
    methods: {
        // expandStages(row,index) {
        //     row._showDetails = !row._showDetails;
        // },
        earlier() {
            const current = earliestDepartTime(this.journeysresponse); 
            var newTime = new Date(current.getTime() - 24*60*1000); 
            const newDepartTime = toHourAndMins(newTime); 
            this.$emit('earlier-tram', newDepartTime);
        },
        later() {
            const current = lastDepartTime(this.journeysresponse); 
            var newTime = new Date(current.getTime() + 60*1000); 
            const newDepartTime = toHourAndMins(newTime); 
            this.$emit('later-tram', newDepartTime);
        }, 
        getStageRowProps(row) {
           return { class: 'stageSummary' }
        },
        dateTimeFormatter(item, index) {
            const queryDate = this.journeysresponse[index].journey.queryDateAsDate;
            return formatDate(queryDate, item)
        },
        stageDateTimeFormatter(item, queryDate) {
            const stageDateTime = new Date(item);
            const stageQueryDate = new Date(queryDate);
            return formatDate(stageQueryDate, stageDateTime)
        },
        changesFormatter(value) {
            if (value.length==0) {
                return "Direct";
            }
            var result = "";
            value.forEach(change => {
                if (result.length>0) result = result.concat(", ");
                result = result.concat(change.name)});
            return result;
        },
        actionFormatter(stage) {
            const action = stage.action;
            if (action=='Walk to' || action=='Walk from') {
                return action;
            }
            const transportMode = stage.mode; //currentlyExpandedJourney(this).stages[stageIndex].mode;
            return action + ' ' + transportMode;
        },
        stationURL(item) {
            return 'https://www.google.com/maps/search/?api=1&query='+ item.latLong.lat + ',' + item.latLong.lon;
        }, 
        platformFormatter(item) {
            if (item==null) {
                return "";
            }
            const platform = item.platform;
            if (platform==null) {
                return "";
            }
            return platform.platformNumber;
        },
        routeFormatter(route) {
            return route.shortName;
            // if (route.transportMode==='Train') {
            //     return route.shortName;
            // } else {
            //     return route.routeName;
            // }
        },
        routeClass(route) {
            const prefix = 'RouteClass';
            const mode = route.transportMode;
            var result = prefix + mode;
            if (mode=='Tram') {
                result = prefix + route.shortName.replace(/\s+/g, '');
            }
            return [ result, 'lineClass'];
        },
        passedStopsFormatter(stage) {
            if (stage.action=='Walk to' || stage.action=='Walk from') {
                return '';
            }
            return stage.passedStops;
        },
        stageHeadsignClass(stage) {
            if (stage.action=='Walk to' || stage.action=='Walk from') {
                return 'walkingHeadSign';
            }
            if (stage.actionStation.transportModes.includes('Train')) {
                return 'trainHeadSign';
            }
            return "headsign";
        }
    },
    template: `
    <div id="journeysComponent">
        <v-container v-if="journeys.length>0">
            <v-card>
                <v-card-title>Journey Results</v-card-title>
                <v-data-table id="results"
                    :mobile-breakpoint="0"
                    :headers="headers"
                    :items="journeys"
                    item-value="journey.index"
                    v-model:expanded="expanded"
                    show-expand=true
                    dense
                    v-model:sort-by="sortBy"
                    hide-default-footer
                    class="elevation-1 journeys-table">
                        <template v-slot:item.journey.firstDepartureTimeAsDate="{ item, index }">
                            <div class="departTime">{{ dateTimeFormatter(item.journey.firstDepartureTimeAsDate, index) }}</div>
                        </template>
                        <template v-slot:item.journey.expectedArrivalTimeAsDate="{ item, index }">
                            <div class="arriveTime">{{ dateTimeFormatter(item.journey.expectedArrivalTimeAsDate, index) }}</div>
                        </template>
                        <template v-slot:item.journey.begin="{ item }">
                            <div>{{ item.journey.begin.name }}</div>
                        </template>
                        <template v-slot:item.journey.changeStations="{ item, index }">
                            <div class="changes">{{ changesFormatter(item.journey.changeStations, index) }}</div>
                        </template>
                        <template v-slot:expanded-row="{ columns, item, index }">
                            <td :colspan="columns.length">
                                <v-data-table :items=item.journey.stages :headers="stageHeaders" 
                                :mobile-breakpoint="0"
                                id="stages" :row-props="getStageRowProps"
                                dense
                                hide-default-footer>
                                    <template v-slot:item.firstDepartureTime="{ item }">
                                        <div class="departTime">{{ stageDateTimeFormatter(item.firstDepartureTime, item.queryDate) }}</div>
                                    </template>
                                    <template v-slot:item.expectedArrivalTime="{ item }">
                                        <div>{{ stageDateTimeFormatter(item.expectedArrivalTime, item.queryDate) }}</div>
                                    </template>
                                    <template v-slot:item.action="{ item, index }">
                                        <div  class="action">{{ actionFormatter(item) }}</div>
                                    </template>
                                    <template v-slot:item.platform.platformNumber="{ item }">
                                        <div  class="platform">{{ platformFormatter(item) }}</div>
                                    </template>
                                    <template v-slot:item.actionStation="{ item }">
                                        <a class="actionStation" :href="stationURL(item.actionStation)" target="_blank">{{ item.actionStation.name }}</a>
                                    </template>
                                    <template v-slot:item.headSign="{ item }">
                                        <div class="headsign" :class="stageHeadsignClass(item)">{{ item.headSign }}</div>
                                    </template>
                                    <template v-slot:item.route="{ item }">
                                        <div class="lineClass" :class="routeClass(item.route)">{{ routeFormatter(item.route) }}</div>
                                    </template>
                                    <template v-slot:item.passedStops="{ item }">
                                        <div class="passedStops">{{ passedStopsFormatter(item) }}</div>
                                    </template>
                                </v-data-table>
                            </td>
                        </template>
                </v-data-table>
            </v-card>
        </v-container>

        <v-container id="earlierLater" v-if="journeys.length>0">
            <v-row >
                <v-col class="text-left">
                    <button type="button" id="earlierButton" class="btn btn-outline-primary" v-on:click="earlier()">« Earlier</button>
                </v-col>
                <v-col class="text-right">
                    <button type="button" id="laterButton" class="btn btn-outline-primary" v-on:click="later()">Later »</button>
                </v-col>
            </v-row>
        </v-container>

        <v-container id="noResults" selectable v-if="noJourneys" class="w-75 tramchesterApp">
            <v-card>
                <v-card-title>No Journeys found</v-card-title>
                <v-card-text>
                    <p class="card=text">
                        No suggested routes were found for this date and time
                    </p>
                </v-card-text>
            </v-card>
        </v-container>
    </div>
    `
}



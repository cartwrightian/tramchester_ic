const axios = require('axios');

export default { 
    data: function() {
        return {
            closures: []
        }
    },
    mounted() {
        var that = this
        axios.get('/api/stations/closures')
        .then(function (response) {
            that.closures = response.data;})
        .catch(function (error) {
            this.$emit('network-error')
            console.log(error)
        });
    },
    methods: {
        displayStations(stations) {
            if (stations.length==1) {
                return stations[0].name + " is"
            }

            var result = "";
            stations.forEach(station => {
                if (result.length!=0) {
                    result += ", ";
                }
                result += station.name;
            });
            result += " are"
            return result;
        },
        displayDates(begin, end) {
            if (begin==end) {
                return " on " + end;
            } else {
                return "between " + begin + " and " + end;
            }
        }
    },
    template: `
    <div id="closuresComponent">
        <v-container v-if="closures.length>0">
            <v-card>
                <v-card-title>Station Closures</v-card-title>
                <v-card-subtitle>Visit <a href="https://www.tfgm.com/">www.tfgm.com</a> for full details</v-card-subtitle>
                <v-card-text>
                    <ul id="ClosureList" class="Closures list-group list-group-flush">
                        <li v-for="closed in closures" id="ClosedItem">
                            <div v-if="closed.fullyClosed == true">
                                {{displayStations(closed.stations)}} closed {{displayDates(closed.begin, closed.end)}}
                            </div>
                            <div v-else>
                                {{displayStations(closed.stations)}} partially closed {{displayDates(closed.begin, closed.end)}}
                            </div>
                        </li>
                    </ul>
                </v-card-text>
            </v-card>
        </v-container>
    </div>
    `
}


const axios = require('axios');

export default { 
    data: function() {
        return {
            buildnumber: []
        }
    },
    props: ['havepos','feedinfo'],
    mounted() {
        var that = this
        axios.get('/api/version')
        .then(function (response) {
            that.buildnumber = response.data.buildNumber;})
        .catch(function (error) {
            this.$emit('network-error')
            console.log(error)
        });
    },
    methods: {
        showDisclaimer(){
            this.$emit('disclaimer-clicked')
        }
    },
    template: `
    <div id="footer">
        <v-card class="model-body">
            <v-container>
                 <v-row justify="center" no-gutters>
                    <v-col>Build
                        <div id="buildNumber">{{buildnumber}}</div>
                    </v-col>
                    <v-col>Data Version:
                        <div id="dataVersion">{{feedinfo.version}}</div>
                    </v-col>
                    <v-col>Geo:
                        <div id="havepos">{{havepos}}</div>
                    </v-col>
                </v-row>
                <v-row justify="center" no-gutters>
                    <v-col>
                    Originally Built by <a href="http://www.thoughtworks.com" target="_blank">ThoughtWorks</a> in Manchester
                    </v-col>
                </v-row>
                <v-row justify="center" no-gutters>
                    <v-col id="copyright">&copy; 2016-2023 ThoughtWorks Ltd. & Ian Cartwright 2023-2024</v-col>
                </v-row>
                <v-row justify="center" no-gutters>
                    <v-col id="license">
                        Contains Transport for Greater Manchester data.
                        Contains public sector information licensed under the Open Government Licence v2.0 by
                        <a href="http://www.datagm.org.uk/" target="_blank">Data GM.</a>
                        Contains public sector information licensed under the Open Government Licence v3.0.
                        Timetable/Fares/London Terminals data under licence from <a href="http://www.raildeliverygroup.com/">RSP</a>.
                    </v-col>
                </v-row>
                <v-row justify="center" no-gutters>
                    <v-col>
                        <v-btn id="disclaimerButton" @click="showDisclaimer()" small>Disclaimer</v-btn>
                    </v-col>
                </v-row>
            </v-container>
        </v-card>
    </div>
    `
}
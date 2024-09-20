const axios = require('axios');

export default { 
    data: function() {
        return {
            buildnumber: [], 
            disclaimerDialog: false
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
        // showDisclaimer(){
        //     //this.$emit('disclaimer-clicked')
        // },
        showDisclaimer() {
            this.disclaimerDialog = true
        },
        dismissDisclaimer() {
            this.disclaimerDialog = false
        },
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
        <v-dialog v-model="disclaimerDialog" width="auto">
            <v-card  id="modaldisclaimer">
            <v-card-item>
                <v-card-title>Disclaimer</v-card-title>
            </v-card-item>
            <v-card-text class="model-body">
                    <p>Whilst every effort has been made to ensure that the contents of this website are correct,
                        the maintainer does not guarantee and makes no warranty, express or implied, as to
                        the quality, accuracy, completeness, timeliness, appropriateness, or suitability of the
                        information we provide on this website.</p>

                    <p>The maintainer also takes no responsibility for the accuracy of information contained on
                        external sources linked to this site.</p>

                    <p>The maintainer assumes no obligation to update the information and the information may
                        be changed from time to time without notice.</p>

                    <p>To the extent permitted by law the maintainer expressly disclaims all liability for any direct,
                        indirect or consequential loss or damage occasioned from the use or inability to use this
                        websites, whether directly or indirectly resulting from inaccuracies, defects, viruses, errors -
                        whether typographical or otherwise, omissions, out of date information or otherwise.</p>

                    <p>Accessing and using the website and the downloading of material from the site (if
                        applicable) is done entirely at the user's own risk. The user will be entirely responsible for
                        any resulting damage to software or computer systems and/or any resulting loss of data.</p>
            </v-card-text>
            <v-card-actions>
                <v-btn id="disclaimerAgreeButton" @click="dismissDisclaimer()">Ok</v-btn>
            </v-card-actions>
            </v-card>
        </v-dialog>
    </div>
    `
}
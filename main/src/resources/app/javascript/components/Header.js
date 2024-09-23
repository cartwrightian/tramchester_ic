
export default { 
    data: function() {
        return {
            cookieDialog: false
        }
    },
    props: ['header'],
    mounted() {
        var cookie = this.$cookies.get("tramchesterVisited");
        if (cookie==null) {
            this.cookieDialog = true
        }
    },
    methods: {
        setCookie() {
            var cookie = { 'visited' : true };
            this.$cookies.set("tramchesterVisited", cookie, "128d", "/", null, false, "Strict");
            this.cookieDialog = false
        }
    },
    template: `
    <div id="header">
       <h1 class="tramchesterHeading">{{header}}</h1>
    </div>
        <v-dialog v-model="cookieDialog" width="auto">
            <v-card id ="modal-cookieConsent">
                <v-card-item>
                    <v-card-title>Cookies</v-card-title>
                    <v-card-subtitle>
                        Welcome to Tramchester, a quick and easy way to plan your travel in Manchester by Metrolink tram.
                    </v-card-subtitle>
                </v-card-item>
                <v-card-text>
                    Tramchester uses <b>cookies</b> to store information in your browser about previous usages and recent journeys.
                </v-card-text>
                <v-card-actions>
                <v-btn id="cookieAgreeButton" @click="setCookie()">I agree</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    `
}
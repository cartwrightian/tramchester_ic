

export default { 
    props: ['livedataresponse'],
    data: function () {
        return {
            itemsPerPage: 5,
            page: 1,
            headers: [
                {value:'transportMode', text:'type', sortable: true},
                {value:'from', text:'From', sortable:true},
                {value:'dueTimeAsDate', text:'Time', sortable: true}, 
                {value:'carriages', text:''},
                {value:'status', text:'Status'},
                {value:'destination', text:'Towards', sortable:true}
            ],
            sortBy: [{ key: 'dueTimeAsDate', order: 'asc' }]
        }
    },
    computed: { 
        localDueTrams: function() {
            if (this.livedataresponse==null) {
                return [];
            }
            return this.livedataresponse.departures;
        },
        noLiveResults: function() {
            if (this.livedataresponse==null) {
                return false;
            }
            return this.livedataresponse.departures.length==0;
        },
         pageCount () {
             return Math.ceil(this.localDueTrams.length / this.itemsPerPage)
           },
    },
    methods: {
        dueTimeFormatter(value) {
            return value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
        }
    },
    template: `
    <v-container id="departuesView">

        <div id="departuresTable" v-if="localDueTrams.length>0">
            <v-card>
                <v-card-title>Live Departures</v-card-title>
                <v-data-table id="departures"
                    :items="localDueTrams"
                    :page.sync="page"
                    :items-per-page="itemsPerPage"
                    :headers="headers" 
                    dense
                    v-model:sort-by="sortBy"
                    hide-default-footer
                    class="elevation-1">
                    <template v-slot:item.dueTimeAsDate="{ item }">
                        <div>{{ dueTimeFormatter(item.dueTimeAsDate) }}</div>
                    </template>
                </v-data-table>
            </v-card>

        </div>

        <div class="text-center pt-2" v-if="localDueTrams.length>0">
            <v-pagination v-model="page" :length="pageCount"></v-pagination>
        </div>
            
        <v-card id="noLiveResults" v-if="noLiveResults">
            <v-card-title class="card-header">Live Departures</v-card-title>
            <v-card-text>
                <p class="card-text">No live departure information was found for the date and time</p>
            </v-card-text>
        </v-card>

    <v-container>


    `
}
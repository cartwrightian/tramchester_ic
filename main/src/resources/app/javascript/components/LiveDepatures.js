

export default { 
    props: ['livedataresponse'],
    data: function () {
        return {
            itemsPerPage: 5,
            page: 1,
            headers: [
                {key:'from.name', title:'From', sortable:true},
                {key:'dueTimeAsDate', title:'Time', sortable: true, width: '1px'}, 
                {key:'carriages', title:''},
                {key:'status', title:'Status'},
                {key:'destination.name', title:'Towards', sortable:true}
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
        },
        rowFormater(item) {
            if (item.item.matchesJourney) {
                return { class: 'font-weight-bold' }
            } else {
                return {  }
            }
        }
    },
    template: `
    <v-container id="departuesView">

        <div id="departuresTable" v-if="localDueTrams.length>0">
            <v-card>
                <v-card-title>Live Departures</v-card-title>
                <v-data-table id="departures"
                    :mobile-breakpoint="0"
                    :items="localDueTrams"
                    :row-props="rowFormater"
                    v-model:page="page"
                    :items-per-page="itemsPerPage"
                    :headers="headers" 
                    dense
                    v-model:sort-by="sortBy"
                    hide-default-footer
                    class="elevation-1 live-departures-table">
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

    </v-container>

    `
}
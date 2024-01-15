

function sort(stopMap, alreadyDisplayed, requestedModes) {

    if (stopMap==null) {
        return [];
    }

    var stops = Array.from(stopMap.values());

    var filtered = filterStops(stops, requestedModes, alreadyDisplayed);

    return filtered.sort((a,b) => {
        var x = a.name.toLowerCase();
        var y = b.name.toLowerCase();
        return x.localeCompare(y);
    });
}

function filterStops(stops, requestedModes, alreadyDisplayed) {
    var results = stops.
        filter(stop => Array.from(stop.transportModes).filter(stopMode => requestedModes.includes(stopMode)).length > 0).
        filter(stop => !alreadyDisplayed.includes(stop.id));

    return results;
}

function getStop(stopId, app) {
    return app.stops.allStops.get(stopId);
}

export default {
    components: {
       
    },
    /// NOTE: don't camel case these, browser will treat them as all lowercase....
    // TODO Add whether selection is for a dropoff or a pickup
    props: ['value','other','name','modes','stops','geo','disabled'], 
    data: function () {
        return {
            //current: this.value
            currentId: null
        }
    },
    watch: {
        value() {
            // so the swap button works as we don't bind the prop value to the native component, but bind currentId instead
            if (this.value!=null) {
                this.currentId = this.value.id;
            }
        }
    },
    methods: {
        updateValue(event) {
            const option = event.target.options[event.target.selectedIndex];
            if (option.className=='MyLocation') {
                const myLocation = this.stops.currentLocation[0];
                this.currentId = myLocation.id;
                this.$emit('input', myLocation);
            } else {
                const stopId = event.target.value;
                this.currentId = stopId;
                const toSend = getStop(stopId, this);
                this.$emit('input', toSend);
            }
        },
        changedValue(event) {
            // no-op
        },
        serialize: function(station) {
            if (station==null) {
                return "";
            }
            return station.name + ' (' + station.transportModes + ')';
        }
    },
    mounted () {
      this.currentId = null;  
    },
    computed: {
        remainingStops: function () {
            return sort(this.stops.allStops, this.alreadyDisplayed, this.modes);
        },
        alreadyDisplayed: function () {

            var results = [];

            this.stops.recentStops.forEach(stop => {
                results.push(stop.id)
            });

            this.stops.nearestStops.forEach(stop => {
                results.push(stop.id)
            });

            return results;
        },
        otherId: function() {
            return this.other ? this.other.id : "";
        },
        bus: function() {
            return this.modes.includes('Bus');
        },
        recentStops: function() {
            return this.stops.recentStops;
        },
        currentLocationStops: function () {
            return this.stops.currentLocation;
        },
        nearestStops: function() {
            return this.stops.nearestStops;
        },
        myLocation: function() {
            return this.stops.currentLocation;
        }
    },
    template: `
    <div>
    <!-- Dropdown selection mode -->
    <!-- note need input, change and model here because dynamically change contents of opt-groups -->
        <select :id="name+'Stop'"
                :disabled="disabled"
                v-on:input="updateValue($event)"
                v-on:change="changedValue($event)"
                v-model="currentId"
                required >
            <option :value="null" selected>Please select {{name}}</option>
            <optgroup label="Nearby" name="Nearby" :id="name+'GroupNearby'" v-if="geo">
                <option class="MyLocation" v-for="stop in myLocation" :value="stop.id" :key="stop.id"
                    :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
            <optgroup label="Nearest Stops" name="Nearest Stops" :id="name+'GroupNearestStops'" v-if="geo">
                <option class="stop" v-for="stop in nearestStops" :value="stop.id" :key="stop.id"
                    :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
            <optgroup label="Recent" name="Recent" :id="name+'GroupRecent'">
                <option class="stop" v-for="stop in recentStops" :value="stop.id" :key="stop.id"
                    :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
            <optgroup label="All Stops" name="All Stops" :id="name+'GroupAllStops'">
                <option class="stop" v-for="stop in remainingStops" :value="stop.id" :key="stop.id"
                    :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
        </select>
    </div>
    `
}


export default { 
    props: ['journeys','livedataresponse']
    ,
    computed: { 
        notes: function() {

            var haveJourneys = this.journeys!=null;

            var allNotes = [];
            if (haveJourneys && this.journeys.length>0) {
                this.journeys.forEach(item => {
                    item.journey.notes.forEach(note => {
                        allNotes.push(note);
                    })
                });    
            } else if (this.livedataresponse!=null) {
                this.livedataresponse.notes.forEach(note => {
                    allNotes.push(note);
                });   
            }
            
            return allNotes;
        },
        liveMessages: function() {
            var messages = [];

            this.notes.forEach(note => {
                var result = note.text;
                if (note.noteType=='Live') {
                    const countPlaces = note.displayedAt.length;
                    result = "'" + note.text + "' - Metrolink";
                    if (countPlaces<3) {
                        for (let index = 0; index < countPlaces; index++) {
                            result = result + ", " + note.displayedAt[index].name;
                        }
                    } 
                }
                messages.push({noteType: note.noteType, text: result})
            })

            return messages;
        }
    },
    template: `
    <div id="notesComponent">
        <v-container v-if="notes.length>0">
            <v-card>
                <v-card-title>Journey Notes</v-card-title>
                <v-card-text>
                    <ul id="NotesList" class="card-text JourneyNotes list-group list-group-flush">
                        <li v-for="note in liveMessages" id="NoteItem">
                            <span :id="note.noteType" v-html="note.text"></span>
                        </li>
                    </ul>
                </v-card-text>
            </v-card>
        </v-container>
    </div>
    `
}


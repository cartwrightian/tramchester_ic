

export default { 
    props: ['receivednotes','livedataresponse']
    ,
    computed: { 
        notes: function() {
            return this.receivednotes;
        },
        liveMessages: function() {
            var messages = [];

            this.notes.forEach(note => {
                var result = note.text;
                if (note.noteType=='Live') {
                    const countPlaces = note.displayedAt.length;
                    result = "'" + note.text + "' - Metrolink.";
                    if (countPlaces<3) {
                        result = result + "(";
                        for (let index = 0; index < countPlaces; index++) {
                            if (index>0) {
                                result = result + ", ";
                            }
                            result = result + note.displayedAt[index].name;
                        }
                        result = result + ")"
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


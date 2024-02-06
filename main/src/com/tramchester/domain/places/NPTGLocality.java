package com.tramchester.domain.places;

import com.tramchester.dataimport.nptg.xml.NPTGLocalityXMLData;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;

public class NPTGLocality implements CoreDomain, HasId<NPTGLocality> {
    private final IdFor<NPTGLocality> id;
    private final String localityName;
    private final String parentLocalityName;
    private final IdFor<NPTGLocality> parentLocalityId;
    private final LatLong latLong;

    public NPTGLocality(NPTGLocalityXMLData item, String parentLocalityName) {
        this.id = createId(item.getNptgLocalityCode());
        this.localityName = item.getLocalityName();
        this.latLong = item.getLatLong();

        String parentLocalityRef = item.getParentLocalityRef();
        if (parentLocalityRef==null) {
            this.parentLocalityId = InvalidId();
        } else {
            if (parentLocalityRef.isEmpty()) {
                this.parentLocalityId = InvalidId();
            } else {
                this.parentLocalityId = createId(parentLocalityRef);
            }
        }

        this.parentLocalityName = parentLocalityName;
    }

    public static IdFor<NPTGLocality> createId(String text) {
        return StringIdFor.createId(text, NPTGLocality.class);
    }

    public static IdFor<NPTGLocality> InvalidId() {
        return StringIdFor.invalid(NPTGLocality.class);
    }

    public IdFor<NPTGLocality> getId() {
        return id;
    }

    public String getLocalityName() {
        return localityName;
    }

    public String getParentLocalityName() {
        return parentLocalityName;
    }

    public IdFor<NPTGLocality> getParentLocalityId() {
        return parentLocalityId;
    }

    public boolean hasParentLocalityId() {
        return parentLocalityId.isValid();
    }

    public LatLong getLatLong() {
        return latLong;
    }
}

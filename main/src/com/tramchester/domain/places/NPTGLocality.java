package com.tramchester.domain.places;

import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;

public class NPTGLocality implements CoreDomain {
    private final IdFor<NPTGLocality> id;
    private final String localityName;
    private final String parentLocalityName;

    public NPTGLocality(NPTGData item) {
        this.id = createId(item.getNptgLocalityCode());
        this.localityName = item.getLocalityName();
        this.parentLocalityName = item.getParentLocalityName();
    }

    public static IdFor<NPTGLocality> createId(String text) {
        return StringIdFor.createId(text, NPTGLocality.class);
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
}

package com.tramchester.unit.cloud.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tramchester.cloud.data.LiveDataClientForS3;
import com.tramchester.cloud.data.S3Keys;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.livedata.cloud.UploadsLiveDataToS3;
import com.tramchester.livedata.domain.DTO.StationDepartureInfoDTO;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.unit.repository.LiveDataMarshallerTest;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UploadsLiveDataToS3Test extends EasyMockSupport {

    private LiveDataClientForS3 clientForS3;
    private UploadsLiveDataToS3 uploadsLiveDataToS3;
    private List<TramStationDepartureInfo> liveData;
    private StationDepartureMapper mapper;
    private S3Keys s3Keys;
    private LocalDateTime lastUpdateTime;
    private LiveDataMarshaller liveDataMarshaller;

    @BeforeEach
    void beforeEachTestRuns() {
        lastUpdateTime = LocalDateTime.parse("2018-11-15T15:06:32");

        clientForS3 = createStrictMock(LiveDataClientForS3.class);
        mapper = createStrictMock(StationDepartureMapper.class);

        s3Keys = createMock(S3Keys.class);

        liveDataMarshaller = createStrictMock(LiveDataMarshaller.class);

        uploadsLiveDataToS3 = new UploadsLiveDataToS3(clientForS3, mapper, s3Keys, liveDataMarshaller);

        liveData = new LinkedList<>();
        liveData.add(LiveDataMarshallerTest.createDepartureInfoWithDueTram(lastUpdateTime, "displayId",
                "platforId", "messageTxt", TramStations.NavigationRoad.fake()));

    }

    @Test
    void shouldConvertToJsonStringAndThenUploadIfNotDuplicate() throws JsonProcessingException {

        List<StationDepartureInfoDTO> dtos = new ArrayList<>();
        dtos.add(new StationDepartureInfoDTO(liveData.get(0)));

        EasyMock.expect(s3Keys.createPrefix(lastUpdateTime.toLocalDate())).andReturn("prefix");
        EasyMock.expect(s3Keys.create(lastUpdateTime)).andReturn("key");

        EasyMock.expect(clientForS3.isEnabled()).andReturn(true);
        EasyMock.expect(clientForS3.itemExists("prefix","key")).andReturn(false);
        EasyMock.expect(mapper.map(dtos)).andReturn("someJson");

        ZonedDateTime zonedLastUpdatetime = ZonedDateTime.of(lastUpdateTime, ZoneOffset.UTC);
        EasyMock.expect(clientForS3.upload("key", "someJson", zonedLastUpdatetime)).andReturn(true);

        replayAll();
        boolean result = uploadsLiveDataToS3.seenUpdate(liveData);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldNotUploadIfKeyExists() {

        EasyMock.expect(s3Keys.createPrefix(lastUpdateTime.toLocalDate())).andReturn("prefix");
        EasyMock.expect(s3Keys.create(lastUpdateTime)).andReturn("key");

        EasyMock.expect(clientForS3.isEnabled()).andReturn(true);
        EasyMock.expect(clientForS3.itemExists("prefix", "key")).andReturn(true);

        replayAll();
        boolean result = uploadsLiveDataToS3.seenUpdate(liveData);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldNotUploadIfEmptyList() {
        EasyMock.expect(clientForS3.isEnabled()).andReturn(true);

        replayAll();
        boolean result = uploadsLiveDataToS3.seenUpdate(Collections.emptyList());
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldNotSubscribeIfS3NotStarted() {
        EasyMock.expect(clientForS3.isEnabled()).andReturn(false);

        replayAll();
        uploadsLiveDataToS3.start();
        verifyAll();
    }

    @Test
    void shouldSubscribeIfS3NotStarted() {
        EasyMock.expect(clientForS3.isEnabled()).andReturn(true);

        liveDataMarshaller.addSubscriber(uploadsLiveDataToS3);
        EasyMock.expectLastCall();

        replayAll();
        uploadsLiveDataToS3.start();
        verifyAll();
    }
}

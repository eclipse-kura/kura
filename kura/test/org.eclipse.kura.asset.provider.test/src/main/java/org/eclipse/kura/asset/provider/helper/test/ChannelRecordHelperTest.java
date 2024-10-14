package org.eclipse.kura.asset.provider.helper.test;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.channel.ScaleOffsetType;
import org.eclipse.kura.internal.asset.provider.helper.ChannelRecordHelper;
import org.eclipse.kura.type.DataType;
import org.junit.Test;

public class ChannelRecordHelperTest {

    private Channel channel;
    private ChannelRecord channelRecord;

    @Test
    public void shouldCreateChannelRecorWithDoubleValueType() {
        givenChannel(new Channel("test-channel", ChannelType.READ, DataType.INTEGER, ScaleOffsetType.DOUBLE, 3.3d, 3.1d,
                Collections.emptyMap()));

        whenCreatedChannelRecord();

        thenChannelRecordValueTypeIs(DataType.DOUBLE);
    }

    @Test
    public void shouldCreateChannelRecorWithIntegerValueType() {
        givenChannel(new Channel("test-channel", ChannelType.READ, DataType.INTEGER,
                ScaleOffsetType.DEFINED_BY_VALUE_TYPE, 3.3d, 3.1d, Collections.emptyMap()));

        whenCreatedChannelRecord();

        thenChannelRecordValueTypeIs(DataType.INTEGER);
    }

    @Test
    public void shouldCreateChannelRecorWithLongValueType() {
        givenChannel(new Channel("test-channel", ChannelType.READ, DataType.INTEGER, ScaleOffsetType.LONG, 3l, 4l,
                Collections.emptyMap()));

        whenCreatedChannelRecord();

        thenChannelRecordValueTypeIs(DataType.LONG);
    }

    private void thenChannelRecordValueTypeIs(DataType datatype) {
        assertEquals(datatype, this.channelRecord.getValueType());

    }

    private void givenChannel(Channel channel) {
        this.channel = channel;

    }

    private void whenCreatedChannelRecord() {
        this.channelRecord = ChannelRecordHelper.createModifiedChannelRecord(this.channel);
    }
}

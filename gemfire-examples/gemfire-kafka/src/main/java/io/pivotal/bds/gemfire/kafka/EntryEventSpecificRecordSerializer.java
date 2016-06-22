package io.pivotal.bds.gemfire.kafka;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serializer;

import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.Operation;

public class EntryEventSpecificRecordSerializer implements Serializer<EntryEvent<Object, SpecificRecord>> {

    private static final EncoderFactory factory = new EncoderFactory();

    @Override
    public byte[] serialize(String topic, EntryEvent<Object, SpecificRecord> data) {
        try {
            Operation op = data.getOperation();
            SpecificRecord rec = op.isCreate() || op.isUpdate() ? data.getNewValue() : data.getOldValue();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BinaryEncoder encoder = factory.directBinaryEncoder(bos, null);
            SpecificDatumWriter<SpecificRecord> writer = new SpecificDatumWriter<SpecificRecord>(rec.getSchema());
            writer.write(rec, encoder);
            byte[] b = bos.toByteArray();
            return b;
        } catch (Exception x) {
            throw new IllegalArgumentException(x.toString(), x);
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

}

package org.logstash.plugins.inputs.http.promtail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.Timestamp;
import org.xerial.snappy.Snappy;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PromtailHandler {

    // compilation check
    org.xerial.snappy.SnappyNative a = null;

    public String toUTF8String(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
    public String toIsoString(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    public List<Map<String, String>> decode_str(String payload) throws Exception {
        return decode(payload.getBytes(StandardCharsets.ISO_8859_1));
    }

    public List<Map<String, String>> decode(byte[] payload) throws Exception {
        byte[] uncompressed = Snappy.uncompress(payload);
        return parseMap(Logproto.PushRequest.parseFrom(uncompressed));
    }

    private List<Map<String, String>> parseMap(Logproto.PushRequest pushRequest) {

        List<Map<String, String>> out = new ArrayList<>();
        final ObjectMapper mapper = new ObjectMapper();

        for (Logproto.StreamAdapter stream : pushRequest.getStreamsList()) {
            Map<String, String> labels = parse(stream.getLabels(), mapper);
            for (Logproto.EntryAdapter entry : stream.getEntriesList()) {
                Map<String, String> event = new HashMap<>(labels);
                if (entry.hasTimestamp()) {
                    event.put("timestamp", entry.getTimestamp().toString());
                }
                event.put("message", entry.getLine());
                out.add(event);
            }
        }
        return out;
    }

    private Map<String, String> parse(String json, ObjectMapper mapper) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            Map<String, String> event = new HashMap<>();
            event.put("labels_all", json);
            return event;
        }
    }

    public String compress(String message) throws IOException {
        Logproto.PushRequest pushRequest = Logproto.PushRequest.newBuilder()
                .addStreams(Logproto.StreamAdapter.newBuilder().addEntries(
                        Logproto.EntryAdapter.newBuilder().setTimestamp(Timestamp.newBuilder().setSeconds(200).build())
                                .setLine(message)
                                .build())).build();

        return new String(Snappy.compress(pushRequest.toByteArray()), StandardCharsets.ISO_8859_1);
    }

    public List<Byte> convertBytesToList(byte[] bytes) {
        final List<Byte> list = new ArrayList<>();
        for (byte b : bytes) {
            list.add(b);
        }
        return list;
    }

    public String sendLogHttp(String uri, String message, String tenant) throws IOException {

        String ret = "OK";

        URL url = new URL(uri);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);

        byte[] out = compress(message).getBytes(StandardCharsets.ISO_8859_1);
        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/x-protobuf");
        if (tenant != null && tenant.length() > 0) {
            http.setRequestProperty("X-Scope-OrgID", tenant);
            http.setRequestProperty("tenant", tenant);
        }
        http.connect();
        try(OutputStream os = http.getOutputStream()) {
            os.write(out);
        }
        try(InputStream is = http.getInputStream()) {
            ret = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        http.disconnect();

        return ret;
    }

}

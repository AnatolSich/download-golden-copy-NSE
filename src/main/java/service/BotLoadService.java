package service;

import download.ApacheHttpClient;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

@SuppressWarnings("DuplicatedCode")
@RequiredArgsConstructor
public class BotLoadService {

    private static final Logger log = Logger.getLogger(BotLoadService.class);

    private final ApacheHttpClient apacheHttpClient;
    private final Properties appProps;


    public JSONObject downloadBodJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            int version = getUrlVersion();
            if (version == 0) {
                return loadBodJson();
            }
            String response = apacheHttpClient.sendGet(
                    String.format(
                            appProps.getProperty("upstox.goldencopy.bodjson.url"),
                            version
                    ));
            JSONParser parser = new JSONParser();
            jsonObject = (JSONObject) parser.parse(response);
        } catch (ParseException | IOException e) {
            log.error(e.getMessage());
        }
        return jsonObject;
    }

    private Integer getUrlVersion() {
        boolean flag = false;
        int initialVersion = Integer.parseInt(appProps.getProperty("upstox.goldencopy.bodjson.initial.version"));
        int maxVersion = Integer.parseInt(appProps.getProperty("upstox.goldencopy.bodjson.max.version"));

        int version = initialVersion;
        try {
            JSONParser parser = new JSONParser();
            String response = apacheHttpClient.sendGet(appProps.getProperty("upstox.goldencopy.checkversion.url"));
            JSONObject jsonObject = (JSONObject) parser.parse(response);
            long tForCheck = Long.parseLong(jsonObject.get("t").toString());
            log.info("tForCheck = " + tForCheck);

            for (; version < maxVersion; version++) {
                response = apacheHttpClient.sendGet(
                        String.format(
                                appProps.getProperty("upstox.goldencopy.bodjson.url"),
                                version
                        ));
                if (response == null || response.isBlank()) continue;
                jsonObject = (JSONObject) parser.parse(response);
                if (jsonObject.get("t") == null) continue;
                long tToCheck = Long.parseLong(jsonObject.get("t").toString());
                log.info("tToCheck = " + tToCheck);
                if (tForCheck < tToCheck) {
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                version = initialVersion - 1;
                for (; version > 0; version--) {
                    response = apacheHttpClient.sendGet(
                            String.format(
                                    appProps.getProperty("upstox.goldencopy.bodjson.url"),
                                    version
                            ));
                    if (response == null || response.isBlank()) continue;
                    jsonObject = (JSONObject) parser.parse(response);
                    if (jsonObject.get("t") == null) continue;
                    long tToCheck = Long.parseLong(jsonObject.get("t").toString());
                    log.info("tToCheck = " + tToCheck);
                    if (tForCheck < tToCheck) {
                        flag = true;
                        break;
                    }
                }
            }

            if (!flag) version = 0;

        } catch (ParseException | IOException e) {
            log.error(e.getMessage());
        }
        log.info("version: " + version);
        return version;
    }

    private JSONObject loadBodJson() {
        String jsonPath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("bod_v_9.json")).getPath();
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        try (FileReader reader = new FileReader(jsonPath)) {
            jsonObject = (JSONObject) parser.parse(reader);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}

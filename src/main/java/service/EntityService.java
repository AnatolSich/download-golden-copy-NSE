package service;

import lombok.RequiredArgsConstructor;
import model.Candle;
import model.Entity;
import model.ReportRecord;
import org.apache.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class EntityService {

    private static final Logger log = Logger.getLogger(EntityService.class);

    private final Properties appProps;

    public Map<String, List<Entity>> parseFromRedisMap(Map<String, List<String>> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> this.convertList(entry.getKey(), entry.getValue())));
    }

    private List<Entity> convertList(String key, List<String> list) {
        List<Entity> convertedList = list.stream().map(this::parseFromRedisString).collect(Collectors.toList());
        return addKey(convertedList, key);
    }

    private Entity parseFromRedisString(String value) {
        String[] strs = value.split(",");
        return Entity.builder()
                .open((int) (Double.parseDouble(strs[1]) * 100))
                .low((int) (Double.parseDouble(strs[3]) * 100))
                .high((int) (Double.parseDouble(strs[2]) * 100))
                .close((int) (Double.parseDouble(strs[4]) * 100))
                .build();
    }

    private List<Entity> addKey(List<Entity> list, String key) {
        return list.stream().peek(entity -> entity.setSymbol_token(key)).collect(Collectors.toList());
    }

    public List<ReportRecord> getReportRecords(Map<String, List<Entity>> niftyCandles, Map<String, List<Entity>> redisCandles) {

        log.info("Matching results...");
        List<ReportRecord> reportRecords = new ArrayList<>();

        if (niftyCandles.size() == 0 || redisCandles.size() == 0) {
            log.info("Not enough data for matching. Nifty candles: " + niftyCandles.size() + "Redis candles: " + redisCandles.size());
            return reportRecords;
        }

        //matched candles
        for (Map.Entry<String, List<Entity>> niftyEntry : niftyCandles.entrySet()
        ) {
            String key = niftyEntry.getKey();
            if (redisCandles.containsKey(key)) {
                reportRecords.add(
                        buildRecord(
                                niftyEntry.getValue().size() != 0 ? niftyEntry.getValue().get(0) : null,
                                redisCandles.get(key).size() != 0 ? redisCandles.get(key).get(0) : null
                        ));
            }
        }
        log.info("Matched candles: " + reportRecords.size());
        int i = reportRecords.size();

        //unmatched nifty candles
        for (Map.Entry<String, List<Entity>> niftyEntry : niftyCandles.entrySet()
        ) {
            String key = niftyEntry.getKey();
            if (!redisCandles.containsKey(key)) {
                reportRecords.add(
                        buildRecord(
                                niftyEntry.getValue().size() != 0 ? niftyEntry.getValue().get(0) : null,
                                null
                        ));
            }
        }

        log.info("Unmatched nifty candles: " + (reportRecords.size() - i));
        i = reportRecords.size();

        //unmatched redis candles
        for (Map.Entry<String, List<Entity>> redisEntry : redisCandles.entrySet()
        ) {
            String key = redisEntry.getKey();
            if (!niftyCandles.containsKey(key)) {
                reportRecords.add(
                        buildRecord(
                                null,
                                redisEntry.getValue().size() != 0 ? redisEntry.getValue().get(0) : null
                        ));
            }
        }
        log.info("Unmatched redis candles: " + (reportRecords.size() - i));

        log.info("REPORT size, candles: " + reportRecords.size());
        return reportRecords;
    }

    @SuppressWarnings("ConstantConditions")
    private ReportRecord buildRecord(Entity entity1, Entity entity2) {
        if (entity1 != null && entity2 == null) {
            return ReportRecord.builder()
                    .time(LocalDateTime.now(ZoneId.of(appProps.getProperty("upstox.goldencopy.time-zone"))))
                    .name(entity1.getSymbol())
                    .token(entity1.getToken())
                    .nseCandle(generateCandle(entity1, entity2))
                    .build();
        } else if (entity1 == null && entity2 != null) {
            return ReportRecord.builder()
                    .time(LocalDateTime.now(ZoneId.of(appProps.getProperty("upstox.goldencopy.time-zone"))))
                    .name(entity2.getSymbol())
                    .token(entity2.getToken())
                    .asCandle(generateCandle(entity1, entity2))
                    .build();
        } else {
            return ReportRecord.builder()
                    .time(LocalDateTime.now(ZoneId.of(appProps.getProperty("upstox.goldencopy.time-zone"))))
                    .name(entity1 != null ? entity1.getSymbol() : "No name")
                    .token(entity1 != null ? entity1.getToken() : 0)
                    .nseCandle(generateCandle(entity1, null))
                    .asCandle(generateCandle(null, entity2))
                    .diff(generateCandle(entity1, entity2))
                    .build();
        }

    }

    private Candle generateCandle(Entity entity1, Entity entity2) {
        if (entity1 != null && entity2 != null)
            return Candle.builder()
                    .open(entity1.getOpen() - entity2.getOpen())
                    .low(entity1.getLow() - entity2.getLow())
                    .high(entity1.getHigh() - entity2.getHigh())
                    .close(entity1.getClose() - entity2.getClose())
                    .build();
        if (entity1 == null && entity2 != null)
            return Candle.builder()
                    .open(entity2.getOpen())
                    .low(entity2.getLow())
                    .high(entity2.getHigh())
                    .close(entity2.getClose())
                    .build();
        else return Candle.builder()
                .open(entity1 != null ? entity1.getOpen() : 0)
                .low(entity1 != null ? entity1.getLow() : 0)
                .high(entity1 != null ? entity1.getHigh() : 0)
                .close(entity1 != null ? entity1.getClose() : 0)
                .build();
    }
}

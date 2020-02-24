package service;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class DateTimeService {

    private static final DateTimeFormatter formatterNifty = DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm.ss");

    @SuppressWarnings("CanBeFinal")
    @Getter
    private ZoneId zoneId;

    public DateTimeService(Properties appProps) {
        String timeZone = appProps.getProperty("upstox.goldencopy.time-zone");
        this.zoneId = ZoneId.of(timeZone);
    }

    public String getFormattedNiftyCurrentLocalDateTime() {
        return LocalDateTime.now(zoneId).format(formatterNifty);
    }

}

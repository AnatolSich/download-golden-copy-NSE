package model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportRecord {

    LocalDateTime time;
    String name;
    int token;
    Candle nseCandle;
    Candle asCandle;
    Candle diff;
}

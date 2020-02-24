package model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Candle {
    private int open;
    private int low;
    private int high;
    private int close;
}

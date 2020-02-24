package model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class Entity {

    private String symbol_token;

    private String symbol;
    @Builder.Default
    private int token = 0;

    private int open;
    private int low;
    private int high;
    private int close;

}

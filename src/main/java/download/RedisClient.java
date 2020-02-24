package download;


import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import service.MatchService;
import service.Types;

import java.io.IOException;
import java.util.*;

public class RedisClient {

    private static final Logger log = Logger.getLogger(RedisClient.class);


    private final MatchService matchService;
    private final Properties appProps;
    @SuppressWarnings("CanBeFinal")
    private Jedis jedis;

    public RedisClient(MatchService matchService, Properties appProps) {
        this.matchService = matchService;
        this.appProps = appProps;
        jedis = this.getConnectionToRedis(0);
    }

    @SuppressWarnings("SameParameterValue")
    private Jedis getConnectionToRedis(int dbIndex) {
        Jedis jedis = new Jedis(appProps.getProperty("upstox.goldencopy.redis.host"), Integer.parseInt(appProps.getProperty("upstox.goldencopy.redis.port")));
        jedis.select(dbIndex);
        log.info("Connected to Redis DB" + dbIndex);
        return jedis;
    }

    public Map<String, List<String>> getMapFromRedisData() throws IOException {
        Set<String> keys = jedis.keys("*");
        log.info("Redis candle quantity: " + keys.size());
        Map<String, List<String>> map = new HashMap<>();
        for (String key : keys
        ) {
            Pipeline pipeline = jedis.pipelined();
            Response<List<String>> response = pipeline.lrange(key, 0, -1);
            pipeline.close();
            key = key.replace('|', ',');
            String[] splittedKey = key.split(",");
            Types type = Types.valueOf(splittedKey[0].split("_")[0]);
            String token = splittedKey[1];
            String min = splittedKey[3];
            if (!"1".equalsIgnoreCase(min)) {
                continue;
            }
            String symbol = matchService.matchSymbolByToken(type, token);
            map.put(symbol + "__" + token, response.get());
        }
        log.info("Minutes candle quantity: " + map.size());
        return map;
    }
}

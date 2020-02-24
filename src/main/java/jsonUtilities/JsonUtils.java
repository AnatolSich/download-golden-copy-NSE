package jsonUtilities;

import lombok.RequiredArgsConstructor;
import model.Entity;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import service.MatchService;
import service.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class JsonUtils {

    private static final Logger log = Logger.getLogger(JsonUtils.class);

    private final MatchService matchService;

    public Map<String, List<Entity>> parseFromNiftyMap(JSONObject jsonObject) {
        JSONArray jsonArray = (JSONArray) jsonObject.get("data");
        Map<String, List<Entity>> map = new HashMap<>();

        for (Object object : jsonArray) {
            JSONObject jsonObjectTemp = (JSONObject) object;
            Entity entity = this.parseJsonToEntity(jsonObjectTemp);
            List<Entity> list = new ArrayList<>();
            list.add(entity);
            map.put(entity.getSymbol_token(), list);
        }
        return map;
    }

    private Entity parseJsonToEntity(JSONObject jsonObject) {
        String token = matchService.matchTokenBySymbol(Types.NSE, jsonObject.get("symbol").toString());
        return Entity.builder()
                .symbol_token(jsonObject.get("symbol").toString() + "__" + token)
                .open((int) (Double.parseDouble(checkJsonParams(jsonObject.get("open"))) * 100))
                .low((int) (Double.parseDouble(checkJsonParams(jsonObject.get("low"))) * 100))
                .high((int) (Double.parseDouble(checkJsonParams(jsonObject.get("high"))) * 100))
                .close((int) (Double.parseDouble(checkJsonParams(jsonObject.get("lrP"))) * 100))
                .build();
    }

    private String checkJsonParams(Object object) {
        if (object == null || object.toString().isBlank()) return "0";
        return object.toString().replaceAll(",", "");
    }

    public JSONObject parseStringToJson(String string) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = (JSONObject) parser.parse(string);
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return jsonObject;
    }
}

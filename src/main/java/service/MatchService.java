package service;

import lombok.RequiredArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@RequiredArgsConstructor
public class MatchService {

    private final JSONObject bodJson;

    public String matchSymbolByToken(Types type, String token) {
        if (bodJson == null || type == null || token == null) return "No symbol found by token " + token;
        JSONObject typeLevel = (JSONObject) bodJson.get(type.name().toLowerCase());
        JSONArray equityLevel = new JSONArray();
        if (type != Types.MCX && typeLevel != null) {
            equityLevel = (JSONArray) typeLevel.get("eq");
        }
        for (Object object : equityLevel
        ) {
            String[] tempArray = object.toString().split(",");
            if (token.equalsIgnoreCase(tempArray[0])) {
                return tempArray[1];
            }
        }
        return "No symbol found by token " + token;
    }

    public String matchTokenBySymbol(Types type, String symbol) {
        if (bodJson == null || type == null || symbol == null) return "No token found by symbol " + symbol;
        JSONObject typeLevel = (JSONObject) bodJson.get(type.name().toLowerCase());
        JSONArray equityLevel = new JSONArray();
        if (type != Types.MCX && typeLevel != null) {
            equityLevel = (JSONArray) typeLevel.get("eq");
        }
        for (Object object : equityLevel
        ) {
            String[] tempArray = object.toString().split(",");
            if (symbol.equalsIgnoreCase(tempArray[1])) {
                return tempArray[0];
            }
        }
        return "No token found by symbol " + symbol;
    }


}

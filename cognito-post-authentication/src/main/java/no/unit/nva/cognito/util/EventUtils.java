package no.unit.nva.cognito.util;

import java.util.Map;

public final class EventUtils {

    private EventUtils() {

    }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> getMap(Map<String,Object> map, String key) {
        return (Map<String,Object>)map.get(key);
    }

    @SuppressWarnings("unchecked")
    public static String getStringValue(Map<String,Object> map, String key) {
        return (String)map.get(key);
    }
}

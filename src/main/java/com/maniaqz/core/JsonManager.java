package com.maniaqz.core;

import com.maniaqz.configuration.JsonConfigKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JsonManager {
    /**
     * For saving converted SQL
     */
    protected JSONObject json;

    protected JsonManager() {
        this.json = new JSONObject((Comparator<String>) (a, b) -> {
            String jsonKey;

            jsonKey = JsonConfigKey.CRUD;
            if(a.equals(jsonKey) && b.equals(jsonKey))
                return 0;
            else if(a.equals(jsonKey))
                return -1;
            else if(b.equals(jsonKey))
                return 1;

            jsonKey = JsonConfigKey.DISTINCT;
            if(a.equals(jsonKey) && b.equals(jsonKey))
                return 0;
            else if(a.equals(jsonKey))
                return -1;
            else if(b.equals(jsonKey))
                return 1;

            return a.compareTo(b);
        });
    }

    /**
     * Injects new Json
     *
     * @param json specified json object from outside
     */
    protected void injectJson(JSONObject json) {
        this.json = json;
    }

    /**
     * Adds json to value if key already exists, else makes new list and adds value
     *
     * @param key   com.maniaqz.configuration.JsonConfigKey
     * @param value List value as String
     */
    protected void putToJson(String key, String value) {
        List<String> list = getConvertedJsonArray(key);
        if (list == null)
            list = new ArrayList<>();

        list.add(value);
        try {
            this.json.put(key, list);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Used for sub query (and analyse), adds index to json-key
     * if json-key "SUB QUERY 1" exists, makes new key with next index
     * for example: "SUB QUERY 2"
     *
     * @param key   com.maniaqz.configuration.JsonConfigKey
     * @param idx   fixed value with 1, only used in cases of recursion
     * @param value List value as string
     *
     */
    protected void putToJson(String key, int idx, String value) {
        List<String> list = getConvertedJsonArray(key + idx);
        if (list != null)
            putToJson(key + (idx + 1), value);
        else
            putToJson(key + idx, value);
    }

    /**
     * Used for sub query (and analyse), adds index to json-key
     * if json-key "SUB QUERY 1" exists, makes new key with next index
     * for example: "SUB QUERY 2"
     *
     * @param key  com.maniaqz.configuration.JsonConfigKey
     * @param idx  fixed value with 1, only used in cases of recursion
     * @param json analysed sub query json object
     */
    protected void putToJson(String key, int idx, JSONObject json) {
        try {
            this.json.getJSONObject(key + idx); // only used for exception check
            putToJson(key, (idx + 1), json);
        } catch (Exception e) {
            putToJson(key + idx, json);
        }
    }

    /**
     * Adds json object as json value: used with recursive sub query analyse
     *
     * @param key  com.maniaqz.configuration.JsonConfigKey
     * @param json json object of an analysed sub query
     */
    protected void putToJson(String key, JSONObject json) {
        try {
            this.json.put(key, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * Converts Json Array to Java List
     *
     * @param key com.maniaqz.configuration.JsonConfigKey
     * @return java.util.list, or null if failed to convert / json key doesn't exist
     */
    private List<String> getConvertedJsonArray(String key) {
        try {
            JSONArray jsonArray = this.json.getJSONArray(key);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); ++i)
                list.add((String) jsonArray.get(i));
            return list;
        } catch (Exception e) {
            return null;
        }
    }
}

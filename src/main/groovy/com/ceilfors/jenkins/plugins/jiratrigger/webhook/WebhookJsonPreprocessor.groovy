package com.ceilfors.jenkins.plugins.jiratrigger.webhook

import org.codehaus.jettison.json.JSONException
import org.codehaus.jettison.json.JSONObject
import org.codehaus.jettison.json.JSONArray

/**
 * Preprocesses webhook JSON payloads to handle Jira's new format with null values.
 * 
 * Jira has changed from sending empty objects to sending objects with null values:
 * Old: "timetracking": {}
 * New: "timetracking": { "originalEstimateSeconds": null, ... }
 * 
 * This causes JSONException in the JRJC parser when it tries to call getInt() on null values.
 * 
 * This preprocessor recursively removes all null values from the JSON structure.
 */
class WebhookJsonPreprocessor {

    /**
     * Recursively cleans all null values from a webhook JSON payload.
     * 
     * @param webhookEvent The original webhook JSON object
     * @return A new JSON object with all null values removed
     * @throws JSONException if the JSON structure is invalid
     */
    static JSONObject cleanNullTimetracking(JSONObject webhookEvent) throws JSONException {
        JSONObject cleanedEvent = new JSONObject(webhookEvent.toString())
        cleanNullValues(cleanedEvent)
        return cleanedEvent
    }
    
    /**
     * Recursively removes all null values from a JSON object.
     * 
     * @param jsonObject The JSON object to clean
     * @throws JSONException if the JSON structure is invalid
     */
    private static void cleanNullValues(JSONObject jsonObject) throws JSONException {
        def keysToRemove = []
        
        // Find all null values to remove
        Iterator<String> keys = jsonObject.keys()
        while (keys.hasNext()) {
            String key = keys.next()
            if (jsonObject.isNull(key)) {
                keysToRemove.add(key)
            } else {
                Object value = jsonObject.get(key)
                if (value instanceof JSONObject) {
                    cleanNullValues((JSONObject) value)
                } else if (value instanceof JSONArray) {
                    cleanNullValues((JSONArray) value)
                }
            }
        }
        
        // Remove null values
        keysToRemove.each { key ->
            jsonObject.remove(key)
        }
    }
    
    /**
     * Recursively removes all null values from a JSON array.
     * 
     * @param jsonArray The JSON array to clean
     * @throws JSONException if the JSON structure is invalid
     */
    private static void cleanNullValues(JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i)
            if (value instanceof JSONObject) {
                cleanNullValues((JSONObject) value)
            } else if (value instanceof JSONArray) {
                cleanNullValues((JSONArray) value)
            }
        }
    }
} 
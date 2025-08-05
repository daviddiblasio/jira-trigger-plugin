package com.ceilfors.jenkins.plugins.jiratrigger.webhook

import org.codehaus.jettison.json.JSONException
import org.codehaus.jettison.json.JSONObject

/**
 * Preprocesses webhook JSON payloads to handle Jira's new format with null timetracking values.
 * 
 * Jira has changed from sending empty timetracking objects to sending objects with null values:
 * Old: "timetracking": {}
 * New: "timetracking": { "originalEstimateSeconds": null, ... }
 * 
 * This causes JSONException in the JRJC parser when it tries to call getInt() on null values.
 */
class WebhookJsonPreprocessor {

    /**
     * Cleans null timetracking values from a webhook JSON payload.
     * 
     * @param webhookEvent The original webhook JSON object
     * @return A new JSON object with null timetracking values removed
     * @throws JSONException if the JSON structure is invalid
     */
    static JSONObject cleanNullTimetracking(JSONObject webhookEvent) throws JSONException {
        JSONObject cleanedEvent = new JSONObject(webhookEvent.toString())
        
        // Handle both changelog and comment events
        if (cleanedEvent.has('issue')) {
            cleanIssueTimetracking(cleanedEvent.getJSONObject('issue'))
        }
        
        return cleanedEvent
    }
    
    /**
     * Cleans null timetracking values from an issue JSON object.
     * 
     * @param issue The issue JSON object
     * @throws JSONException if the JSON structure is invalid
     */
    private static void cleanIssueTimetracking(JSONObject issue) throws JSONException {
        if (!issue.has('fields')) {
            return
        }
        
        JSONObject fields = issue.getJSONObject('fields')
        
        // Clean timetracking object if it exists
        if (fields.has('timetracking')) {
            JSONObject timetracking = fields.getJSONObject('timetracking')
            cleanTimetrackingObject(timetracking)
        }
        
        // Also clean individual timetracking fields that might exist at the fields level
        cleanTimetrackingFields(fields)
    }
    
    /**
     * Cleans null values from a timetracking JSON object.
     * 
     * @param timetracking The timetracking JSON object
     * @throws JSONException if the JSON structure is invalid
     */
    private static void cleanTimetrackingObject(JSONObject timetracking) throws JSONException {
        // List of timetracking fields that should be cleaned if null
        def timetrackingFields = [
            'originalEstimate',
            'remainingEstimate', 
            'timeSpent',
            'originalEstimateSeconds',
            'remainingEstimateSeconds',
            'timeSpentSeconds'
        ]
        
        timetrackingFields.each { field ->
            if (timetracking.has(field) && timetracking.isNull(field)) {
                timetracking.remove(field)
            }
        }
        
        // If the timetracking object is now empty, remove it entirely
        if (timetracking.length() == 0) {
            // Note: We can't remove the field from the parent here, so we'll leave an empty object
            // The JRJC parser should handle empty objects gracefully
        }
    }
    
    /**
     * Cleans individual timetracking fields that might exist at the fields level.
     * 
     * @param fields The fields JSON object
     * @throws JSONException if the JSON structure is invalid
     */
    private static void cleanTimetrackingFields(JSONObject fields) throws JSONException {
        // List of individual timetracking fields that might exist at the fields level
        def individualTimetrackingFields = [
            'timespent',
            'timeoriginalestimate',
            'aggregatetimespent',
            'aggregatetimeestimate'
        ]
        
        individualTimetrackingFields.each { field ->
            if (fields.has(field) && fields.isNull(field)) {
                fields.remove(field)
            }
        }
    }
} 
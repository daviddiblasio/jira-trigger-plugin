package com.ceilfors.jenkins.plugins.jiratrigger.webhook

import org.codehaus.jettison.json.JSONObject
import spock.lang.Specification

/**
 * Test for WebhookJsonPreprocessor to verify it correctly handles null timetracking values.
 */
class WebhookJsonPreprocessorTest extends Specification {

    def 'Should clean null timetracking values from webhook JSON'() {
        given:
        def webhookJson = '''
        {
          "timestamp": 1451136000000,
          "webhookEvent": "jira:issue_updated",
          "issue": {
            "id": "11120",
            "key": "TEST-136",
            "fields": {
              "timetracking": {
                "originalEstimate": null,
                "remainingEstimate": null,
                "timeSpent": null,
                "originalEstimateSeconds": null,
                "remainingEstimateSeconds": null,
                "timeSpentSeconds": null
              },
              "timespent": null,
              "timeoriginalestimate": null,
              "aggregatetimespent": null,
              "aggregatetimeestimate": null
            }
          }
        }
        '''
        def webhookJsonObject = new JSONObject(webhookJson)

        when:
        def cleanedWebhook = WebhookJsonPreprocessor.cleanNullTimetracking(webhookJsonObject)

        then:
        noExceptionThrown()
        cleanedWebhook != null
        
        // Check that null timetracking fields are removed
        def issue = cleanedWebhook.getJSONObject('issue')
        def fields = issue.getJSONObject('fields')
        def timetracking = fields.getJSONObject('timetracking')
        
        // The timetracking object should be empty after cleaning
        timetracking.length() == 0
        
        // Individual timetracking fields should be removed
        !fields.has('timespent')
        !fields.has('timeoriginalestimate')
        !fields.has('aggregatetimespent')
        !fields.has('aggregatetimeestimate')
    }

    def 'Should handle webhook with mixed null and valid timetracking values'() {
        given:
        def webhookJson = '''
        {
          "timestamp": 1451136000000,
          "webhookEvent": "jira:issue_updated",
          "issue": {
            "id": "11120",
            "key": "TEST-136",
            "fields": {
              "timetracking": {
                "originalEstimate": "5m",
                "remainingEstimate": null,
                "timeSpent": "2h",
                "originalEstimateSeconds": 300,
                "remainingEstimateSeconds": null,
                "timeSpentSeconds": 7200
              },
              "timespent": 7200,
              "timeoriginalestimate": 300,
              "aggregatetimespent": 7200,
              "aggregatetimeestimate": 300
            }
          }
        }
        '''
        def webhookJsonObject = new JSONObject(webhookJson)

        when:
        def cleanedWebhook = WebhookJsonPreprocessor.cleanNullTimetracking(webhookJsonObject)

        then:
        noExceptionThrown()
        cleanedWebhook != null
        
        // Check that valid values are preserved and null values are removed
        def issue = cleanedWebhook.getJSONObject('issue')
        def fields = issue.getJSONObject('fields')
        def timetracking = fields.getJSONObject('timetracking')
        
        // Valid values should remain
        timetracking.getString('originalEstimate') == '5m'
        timetracking.getString('timeSpent') == '2h'
        timetracking.getInt('originalEstimateSeconds') == 300
        timetracking.getInt('timeSpentSeconds') == 7200
        
        // Null values should be removed
        !timetracking.has('remainingEstimate')
        !timetracking.has('remainingEstimateSeconds')
        
        // Individual fields should remain
        fields.getInt('timespent') == 7200
        fields.getInt('timeoriginalestimate') == 300
        fields.getInt('aggregatetimespent') == 7200
        fields.getInt('aggregatetimeestimate') == 300
    }

    def 'Should handle webhook without timetracking data'() {
        given:
        def webhookJson = '''
        {
          "timestamp": 1451136000000,
          "webhookEvent": "jira:issue_updated",
          "issue": {
            "id": "11120",
            "key": "TEST-136",
            "fields": {
              "summary": "Test issue"
            }
          }
        }
        '''
        def webhookJsonObject = new JSONObject(webhookJson)

        when:
        def cleanedWebhook = WebhookJsonPreprocessor.cleanNullTimetracking(webhookJsonObject)

        then:
        noExceptionThrown()
        cleanedWebhook != null
        
        // The webhook should remain unchanged
        def issue = cleanedWebhook.getJSONObject('issue')
        def fields = issue.getJSONObject('fields')
        fields.getString('summary') == 'Test issue'
    }
} 
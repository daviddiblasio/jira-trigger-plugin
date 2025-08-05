package com.ceilfors.jenkins.plugins.jiratrigger.webhook

import org.codehaus.jettison.json.JSONObject
import spock.lang.Specification

/**
 * Test for WebhookJsonPreprocessor to verify it correctly handles null values recursively.
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

    def 'Should clean null comment visibility fields'() {
        given:
        def webhookJson = '''
        {
          "timestamp": 1451136000000,
          "webhookEvent": "comment_created",
          "issue": {
            "id": "11120",
            "key": "TEST-136",
            "fields": {
              "summary": "Test issue"
            }
          },
          "comment": {
            "id": "10000",
            "body": "Test comment",
            "visibility": null
          }
        }
        '''
        def webhookJsonObject = new JSONObject(webhookJson)

        when:
        def cleanedWebhook = WebhookJsonPreprocessor.cleanNullTimetracking(webhookJsonObject)

        then:
        noExceptionThrown()
        cleanedWebhook != null
        
        // Check that null visibility field is removed
        def comment = cleanedWebhook.getJSONObject('comment')
        comment.getString('body') == 'Test comment'
        !comment.has('visibility')
    }

    def 'Should preserve valid comment visibility fields'() {
        given:
        def webhookJson = '''
        {
          "timestamp": 1451136000000,
          "webhookEvent": "comment_created",
          "issue": {
            "id": "11120",
            "key": "TEST-136",
            "fields": {
              "summary": "Test issue"
            }
          },
          "comment": {
            "id": "10000",
            "body": "Test comment",
            "visibility": {
              "type": "role",
              "value": "Developers"
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
        
        // Check that valid visibility field is preserved
        def comment = cleanedWebhook.getJSONObject('comment')
        comment.getString('body') == 'Test comment'
        comment.has('visibility')
        comment.getJSONObject('visibility').getString('type') == 'role'
        comment.getJSONObject('visibility').getString('value') == 'Developers'
    }
    
    def 'Should clean null values from nested structures'() {
        given:
        def webhookJson = '''
        {
          "timestamp": 1640995200000,
          "webhookEvent": "jira:issue_updated",
          "issue": {
            "id": "12345",
            "key": "TEST-123",
            "fields": {
              "summary": "Test Issue",
              "comment": [
                {
                  "id": "10001",
                  "author": {
                    "name": "testuser",
                    "displayName": "Test User"
                  },
                  "body": "This is a test comment",
                  "visibility": null
                },
                {
                  "id": "10002", 
                  "author": {
                    "name": "testuser2",
                    "displayName": "Test User 2"
                  },
                  "body": "This is another test comment",
                  "visibility": {
                    "type": "role",
                    "value": "Developers"
                  }
                }
              ],
              "timetracking": {
                "originalEstimate": null,
                "remainingEstimate": null,
                "timeSpent": null,
                "originalEstimateSeconds": null,
                "remainingEstimateSeconds": null,
                "timeSpentSeconds": null
              }
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
        
        // Check that comments array is processed correctly
        def comments = cleanedWebhook.getJSONObject('issue').getJSONObject('fields').getJSONArray('comment')
        comments.length() == 2
        
        // First comment should have visibility removed
        !comments.getJSONObject(0).has('visibility')
        
        // Second comment should have visibility preserved
        comments.getJSONObject(1).has('visibility')
        comments.getJSONObject(1).getJSONObject('visibility').getString('type') == 'role'
        comments.getJSONObject(1).getJSONObject('visibility').getString('value') == 'Developers'
        
        // Timetracking should be empty
        def timetracking = cleanedWebhook.getJSONObject('issue').getJSONObject('fields').getJSONObject('timetracking')
        timetracking.length() == 0
    }
    
    def 'Should handle any null values anywhere in the JSON structure'() {
        given:
        def webhookJson = '''
        {
          "timestamp": 1451136000000,
          "webhookEvent": "jira:issue_updated",
          "nullField": null,
          "issue": {
            "id": "11120",
            "key": "TEST-136",
            "nullId": null,
            "fields": {
              "summary": "Test issue",
              "nullSummary": null,
              "nested": {
                "value": "test",
                "nullValue": null
              }
            }
          },
          "changelog": {
            "id": "67890",
            "nullId": null,
            "items": [
              {
                "field": "status",
                "nullField": null,
                "from": "To Do",
                "to": "In Progress"
              }
            ]
          }
        }
        '''
        def webhookJsonObject = new JSONObject(webhookJson)

        when:
        def cleanedWebhook = WebhookJsonPreprocessor.cleanNullTimetracking(webhookJsonObject)

        then:
        noExceptionThrown()
        cleanedWebhook != null
        
        // Top-level null fields should be removed
        !cleanedWebhook.has('nullField')
        
        // Issue null fields should be removed
        def issue = cleanedWebhook.getJSONObject('issue')
        !issue.has('nullId')
        
        // Fields null fields should be removed
        def fields = issue.getJSONObject('fields')
        !fields.has('nullSummary')
        
        // Nested null fields should be removed
        def nested = fields.getJSONObject('nested')
        !nested.has('nullValue')
        nested.getString('value') == 'test'
        
        // Changelog null fields should be removed
        def changelog = cleanedWebhook.getJSONObject('changelog')
        !changelog.has('nullId')
        
        // Items array null fields should be removed
        def items = changelog.getJSONArray('items')
        items.length() == 1
        !items.getJSONObject(0).has('nullField')
        items.getJSONObject(0).getString('field') == 'status'
    }
} 
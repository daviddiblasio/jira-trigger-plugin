package com.ceilfors.jenkins.plugins.jiratrigger.webhook

import com.atlassian.jira.rest.client.api.domain.ChangelogGroup
import com.atlassian.jira.rest.client.api.domain.ChangelogItem
import com.atlassian.jira.rest.client.internal.json.ChangelogItemJsonParser
import com.atlassian.jira.rest.client.internal.json.IssueJsonParser
import com.atlassian.jira.rest.client.internal.json.JsonObjectParser
import com.atlassian.jira.rest.client.internal.json.JsonParseUtil
import org.codehaus.jettison.json.JSONException
import org.codehaus.jettison.json.JSONObject

import static com.ceilfors.jenkins.plugins.jiratrigger.webhook.WebhookJsonParserUtils.satisfyRequiredKeys
import static com.ceilfors.jenkins.plugins.jiratrigger.webhook.WebhookJsonParserUtils.putIfAbsent

/**
 * @author ceilfors
 */
class WebhookChangelogEventJsonParser implements JsonObjectParser<WebhookChangelogEvent> {

    /**
     * Not using ChangelogJsonParser because it is expecting "created" field which is not
     * being supplied from webhook event.
     */
    private static final DATE_FIELD_NOT_EXIST = '1980-01-01T00:00:00.000+0000'
    private static final ISSUE_KEY = 'issue'

    private final ChangelogItemJsonParser changelogItemJsonParser = new ChangelogItemJsonParser()
    private final IssueJsonParser issueJsonParser = new IssueJsonParser(new JSONObject([:]), new JSONObject([:]))

    /**
     * Fills details needed by JRC JSON Parser that are missing in JIRA Cloud Webhook events.
     */
    private static void satisfyCloudRequiredKeys(JSONObject webhookEvent) {
        JSONObject fields = webhookEvent.getJSONObject(ISSUE_KEY).getJSONObject('fields')
        putIfAbsent(fields, 'created', DATE_FIELD_NOT_EXIST)
        putIfAbsent(fields, 'updated', DATE_FIELD_NOT_EXIST)
    }

    @Override
    WebhookChangelogEvent parse(JSONObject webhookEvent) throws JSONException {
        satisfyRequiredKeys(webhookEvent)
        satisfyCloudRequiredKeys(webhookEvent)

        // Clean null timetracking values before parsing
        JSONObject cleanedWebhookEvent = WebhookJsonPreprocessor.cleanNullTimetracking(webhookEvent)

        Collection<ChangelogItem> items = JsonParseUtil.parseJsonArray(
                cleanedWebhookEvent.getJSONObject('changelog').getJSONArray('items'), changelogItemJsonParser)
        new WebhookChangelogEvent(
                cleanedWebhookEvent.getLong('timestamp'),
                cleanedWebhookEvent.getString('webhookEvent'),
                issueJsonParser.parse(cleanedWebhookEvent.getJSONObject('issue')),
                new ChangelogGroup(null, null, items)
        )
    }
}

import json
import boto3

client = boto3.client('events')

def lambda_handler(event, context):
    customerId = event.get("request", {}).get("userAttributes", {}).get('custom:customerId')
    if customerId is None:
        console.log("missing customerId")
        eventbridge_put_event(event)
    else:
        console.log("has customerid")
    return event

def eventbridge_put_event(event):
    console.log("eventbridge_put_event start")
    response = client.put_events(
        Entries=[
            {
                'Source': 'nva.cognito',
                'DetailType': 'updateUserAtributes',
                'Detail': 'string'
            },
        ]
    )
    console.log("eventbridge_put_event, message sendt")
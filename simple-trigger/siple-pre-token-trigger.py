import requests
import json

def lambda_handler(event, context):
    # TODO implement

    if not (event['request']['userAttributes']['custom:customerId']):
        print("missing customerId")
        requests.post("/upsertuser", event)
    else:
        print("has customerid")
    return {
        'statusCode': 200,
        'body': event
    }

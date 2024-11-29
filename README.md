# Sample FungibleToken App

This is a sample FungibleToken Cordapp implementing TokenSelectionAPI, FungibleState, IssuableState, and OwnableState provided in Corda5.

### How to use

In Corda 5, flows are triggered from `POST /flow/{holdingidentityshorthash}`, and the flow status can be checked at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`.

* holdingidentityshorthash: This is the ID of network participants. The IDs of all participants in the network can be checked with the `ListVNodes` task.
* clientrequestid: This is the ID given to the process of the flow to be executed.

#### Step 1: Let's Issue Tokens

We'll issue 100 units of USD to Bob.
Go to `POST /flow/{holdingidentityshorthash}` and POST with the following Request Body:

```
{
  "clientRequestId": "issue-1",
  "flowClassName": "com.r3.token.fungible.workflows.IssueFungibleTokenFlow",
  "requestBody": {
    "issuer": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
    "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
    "quantity": 100,
    "symbol": "USD",
    "tag": ""
  }
}
```

Check the flow status at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`.

#### Step 2: Let's Transfer Tokens

We'll transfer 50 units of USD issued to Bob in Step 1 to Alice.
Go to `POST /flow/{holdingidentityshorthash}` and POST with the following Request Body:
The ownerId, issuerId, and ownerId are...

```
{
  "clientRequestId": "transfer-1",
  "flowClassName": "com.r3.token.fungible.workflows.TransferFungibleTokenFlow",
  "requestBody": {
    "issuer": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
    "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
    "newOwner": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
    "quantity": 50,
    "symbol": "USD",
    "tag": ""
  }
}
```

As usual, check the flow status at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`.

#### Step 3: Let's Redeem Tokens

We'll redeem 50 units of USD issued to Bob in Step 1.
Go to `POST /flow/{holdingidentityshorthash}` and POST with the following Request Body:
The ownerId, issuerId, and ownerId are...

```
{
  "clientRequestId": "redeem-1",
  "flowClassName": "com.r3.token.fungible.workflows.RedeemFungibleTokenFlow",
  "requestBody": {
    "issuer": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
    "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
    "quantity": 50,
    "symbol": "USD",
    "tag": ""
  }
}
```

As usual, check the flow status at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`.

#### Step 4: Let's Check the Balance

We'll check Bob's Token balance.
Go to `POST /flow/{holdingidentityshorthash}` and POST with the following Request Body:
The ownerId, issuerId, and ownerId are...

```
{
    "clientRequestId": "get-1",
    "flowClassName": "com.r3.token.fungible.workflows.GetTokenBalanceFlow",
    "requestBody": {
        "issuer": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
        "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "symbol": "USD"
    }
}
```

As usual, check the flow status at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`.
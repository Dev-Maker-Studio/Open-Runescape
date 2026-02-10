# LoginManager.kt Deobfuscation Notes

## Login Protocol Flow (RS #727)

The login flow is a state machine driven by `LoginStage` enum:

```
IDLE -> OPENING_CONNECTION -> AWAITING_INIT_RESPONSE
  -> [social network path] -> READING_SOCIAL_RESPONSE_SIZE -> READING_SOCIAL_URL_DATA
     -> AWAITING_SOCIAL_AUTH_CONFIRM -> READING_SOCIAL_TOKENS
  -> SENDING_LOGIN_REQUEST -> AWAITING_LOGIN_RESPONSE
     -> response 2: READING_PAYLOAD_SIZE -> READING_LOGIN_PAYLOAD
        -> [world login]: READING_PACKET_HEADER -> READING_PACKET_OPCODE -> READING_FIRST_GAME_PACKET
     -> response 15: READING_RECONNECT_RESPONSE (reconnect path)
     -> response 21: READING_QUEUE_POSITION (world full)
     -> response 29/45: READING_RESPONSE_DETAIL (error details)
     -> response 1: AWAITING_CLIENT_CONFIRM -> SENDING_LOGIN_CONTINUE -> AWAITING_LOGIN_RESPONSE
     -> response 42: AWAITING_TIME_SYNC -> AWAITING_LOGIN_RESPONSE
```

## Login Response Codes

| Code | Meaning | Handler |
|------|---------|---------|
| 0 | Init success (non-social) | Continue to SENDING_LOGIN_REQUEST |
| 1 | "Please wait" / confirm | AWAITING_CLIENT_CONFIRM (CS2 triggers continue) |
| 2 | Login success | Read payload, enter lobby or game |
| 15 | Reconnect success | Read player data, set LOGGED_INGAME |
| 21 | World full / queue | Read queue position multiplier |
| 23 | Login server error (retry) | Retry up to 3 times |
| 29 | Error with detail byte | Read 1 detail byte |
| 35 | Social network fallback | Switch to social network login |
| 42 | Time sync required | Read 2-byte server time offset |
| 45 | Error with detail + extra | Read 1 detail byte + 2 extra bytes |

## Remaining Unidentified Fields

### anInt8307 (ushort)
- Read from lobby login response after EMAIL_STATUS
- Pushed to CS2 stack by method1357
- Position in lobby response: after email status byte
- Hypothesis (70%): Account creation days or billing cycle days
- Need: CS2 script analysis showing which interface uses this value

### anInt1526 (int)
- Read from lobby login response after username
- Pushed to CS2 stack by method8869
- Position in lobby response: after username, before aBool7323
- Hypothesis (60%): Some account ID or balance (4 bytes = could be anything)
- Need: CS2 script analysis or RS protocol docs

### aBool5556 (bool)
- Read from lobby login response between FileOnDisk data and username
- Pushed to CS2 stack by method12352
- Hypothesis (55%): Has display name set, or some account flag
- Need: More context about what CS2 scripts check this for

## External Field References

| LoginManager field | Files referencing it |
|---|---|
| loginQueuePosition | CS2Interpreter.kt |
| socialNetworkToken | StaticMethods.kt |
| membershipTimeCorrection | CS2Interpreter.kt |
| loginResponseDetail | Client.kt, CS2Interpreter.kt |
| loginResponseExtraInfo | Client.kt, CS2Interpreter.kt |
| anInt8307 | CS2Interpreter.kt |
| anInt1526 | CS2Interpreter.kt |
| aBool5556 | CS2Interpreter.kt |

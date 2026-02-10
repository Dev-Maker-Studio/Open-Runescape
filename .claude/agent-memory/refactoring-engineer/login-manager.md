# LoginManager.kt + AccountCreationResponseOpcodes.kt Deobfuscation (COMPLETE)

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

## All Renames Completed

### AccountCreationResponseOpcodes (8 enum constants + 1 method)
| Old | New | Opcode |
|-----|-----|--------|
| aClass4835737 | NONE | -1 |
| aClass4835730 | AWAITING_RESPONSE | -2 |
| aClass4835729 | NOT_STARTED | -3 |
| aClass4835732 | CONNECTION_ERROR | -4 |
| aClass4835731 | CONNECTION_FAILED | -5 |
| aClass4835735 | NAME_UNAVAILABLE | 3 |
| aClass4835736 | ACCOUNT_CREATION_DISABLED | 7 |
| aClass4835733 | ALREADY_LOGGED_IN | 9 |
| method1489 | getReceivedResponse | (method) |

### LoginStage enum (20 constants)
| Old | New |
|-----|-----|
| NONE_2 | IDLE |
| UNK_18 | OPENING_CONNECTION |
| UNK_35 | AWAITING_INIT_RESPONSE |
| UNK_46 | READING_SOCIAL_RESPONSE_SIZE |
| UNK_57 | READING_SOCIAL_URL_DATA |
| UNK_65 | AWAITING_SOCIAL_AUTH_CONFIRM |
| UNK_75 | READING_SOCIAL_TOKENS |
| UNK_80 | SENDING_LOGIN_REQUEST |
| UNK_97 | AWAITING_LOGIN_RESPONSE |
| UNK_105 | AWAITING_CLIENT_CONFIRM |
| UNK_115 | SENDING_LOGIN_CONTINUE |
| UNK_128 | READING_QUEUE_POSITION |
| UNK_133 | READING_PAYLOAD_SIZE |
| UNK_140 | READING_LOGIN_PAYLOAD |
| UNK_157 | READING_FIRST_GAME_PACKET |
| UNK_167 | READING_PACKET_HEADER |
| UNK_173 | READING_PACKET_OPCODE |
| UNK_180 | READING_RESPONSE_DETAIL |
| UNK_196 | READING_RECONNECT_RESPONSE |
| UNK_201 | AWAITING_TIME_SYNC |

### LoginManager fields (13 fields)
| Old | New | Evidence |
|-----|-----|----------|
| anInt1105 | loginResponseCode | Stores response code 29/45 for subsequent detail reading |
| anInt1965 | loginPayloadSize | Size byte for login response payload |
| anInt90 | connectionTickCount | Ticks since connection attempt, compared to timeout |
| anInt104 | connectionRetryCount | Number of retries, max 3 |
| anInt103 | loginQueuePosition | CS2: instr6700 (queue position * 50 from response 21) |
| anInt112 | loginResponseDetail | Detail byte from response 29/45, stored to lastDisconnectArg2 |
| anInt94 | loginResponseExtraInfo | Extra 2-byte info from response 45, stored to lastDisconnectArg1 |
| aLong77 | socialNetworkToken | Social network auth token (long) |
| aLong86 | socialNetworkNonce | Second long from social network auth, written to seed block |
| anInt8307 | lobbyCreditCardExpiry | CS2: USERDETAIL_LOBBY_CCEXPIRY |
| aLong420 | membershipTimeCorrection | Time correction for membership remaining calculation |
| aBool5556 | isDobRequested | CS2: USERDETAIL_LOBBY_DOBREQUESTED |
| anInt1526 | lobbyPlayAge | CS2: USERDETAIL_LOBBY_PLAYAGE |

### LoginManager methods (16 methods)
| Old | New | Purpose |
|-----|-----|---------|
| method3360 | isLoginInProgress | Returns loginStage != IDLE |
| method4964 | lobbyLoginWithStoredCredentials | Lobby login using stored SN credentials |
| method6160 | lobbyLoginWithBase64Url | Lobby login using base64 URL credentials |
| method5921 | startLobbyLogin | Dispatches to right lobby login method |
| method7558 | initializeLogin | Sets all login fields, starts state machine |
| method13784 | setLoginResponse | Sets lobby or world login response code |
| method1774 | confirmLoginContinue | AWAITING_CLIENT_CONFIRM -> SENDING_LOGIN_CONTINUE |
| method2635 | switchWorldAndLogin | Disconnects lobby, sets creds, transitions to world login |
| method5558 | resetLoginState | Resets login stage and response codes |
| method5118 | resetCredentials | Resets SN token and selected world |
| method7777 | returnToLoginScreen | Transitions from UNK_0 to UNK_7 |
| method1384 | startWorldLogin | Dispatches to right world login method |
| method1478 | cancelLogin | Closes connection, resets login state |
| method537 | worldLoginWithStoredCredentials | World login using stored credentials |
| method7022 | worldLoginWithBase64Url | World login using base64 URL credentials |
| method8350 | onReconnect | Handles reconnect: resets state, sets LOGGED_INGAME |

## Cross-file references updated
- CS2Interpreter.kt (7 references: loginQueuePosition, loginResponseDetail, loginResponseExtraInfo, lobbyCreditCardExpiry, membershipTimeCorrection, isDobRequested, lobbyPlayAge + 6 method refs)
- Client.kt (loginResponseDetail, loginResponseExtraInfo, isLoginInProgress, startWorldLogin, resetLoginState)
- GameState.kt (isLoginInProgress)
- StaticMethods.kt (socialNetworkToken)
- AccountCreationStage.kt (NOT_STARTED, CONNECTION_FAILED)

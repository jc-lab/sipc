## Frame Structure

```
┌───────────────────────────────────┐
│ FRAME_SIZE │ FRAME_TYPE │ PAYLOAD │
└───────────────────────────────────┘
```

Max Frame Size : 16777215 bytes

### FRAME_SIZE

* 24 bits Little Endian (MSB MUST ZERO)
* sizeof(FRAME_SIZE, FRAME_TYPE, PAYLOAD)

### FRAME_TYPE

* 8 bits
* See [Frames](#Frames)

### PAYLOAD

* data

## Frames

### Alert Frame (0xf1)

* Proto : AlertFrame
* Error Alert

### Client Hello Frame (0x11)

* Proto : ClientHelloFrame

* ephemeral_public_key
* client_nonce

### Server Hello Frame (0x12)

* Proto : ServerHelloFrame

* server_nonce
* WrappedData encrypted_header

### Wrapped Data (0x1a)

* Proto : WrappedData

## Cryptography

### master_secret

master_secret = HMAC(key = ecdh_derived_key, data = server_nonce + client_nonce)

### Wrapped Data's Keys

**Server To Client:**

```
server_counter = N { 64-bits LE }
iv = HMAC { key=master_secret, data="siv" + server_counter }
key = HMAC { key=master_secret, data="sky" + server_counter }
```

**Client To Server:**

```
client_counter = N { 64-bits LE }
iv = HMAC { key=master_secret, data="civ" + client_counter }
key = HMAC { key=master_secret, data="cky" + client_counter }
```

import argparse
import json
from pathlib import Path
import socket
import threading
import time

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / 'login' / 'tests' / 'fixtures'
REQUESTS = {'lobby': 'lobby-username.wire', 'world': 'world-normal.wire'}
WORLD_STATE = 'world-state-standard-104.payload'


def exact(connection, count):
    output = bytearray()
    while len(output) < count:
        part = connection.recv(count - len(output))
        if not part:
            raise ValueError('fixture connection truncated')
        output.extend(part)
    return bytes(output)


def incoming_packets():
    words = (FIXTURES / 'isaac-incoming.words').read_bytes()
    position = 0

    def packet(opcode, payload=b'', first=False):
        nonlocal position
        if not 0 <= opcode <= 32767 or len(payload) > 65535:
            raise ValueError('fixture packet outside native bounds')
        if not first and opcode not in (29, 51, 85, 103):
            raise ValueError('unsupported regular fixture opcode')
        if not first and opcode == 29 and payload:
            raise ValueError('fixture PING must be empty')
        raw = bytes([opcode]) if opcode < 128 else bytes([128 + (opcode >> 8), opcode & 255])
        encrypted = bytearray()
        for byte in raw:
            if position + 4 > len(words):
                raise ValueError('fixture ISAAC words exhausted')
            word = int.from_bytes(words[position:position + 4], 'big')
            position += 4
            encrypted.append((byte + word) & 255)
        if first or opcode in (51, 85, 103):
            encrypted.extend(len(payload).to_bytes(2, 'big'))
        return bytes(encrypted) + payload

    return packet


def lobby_stream():
    packet = incoming_packets()
    return b''.join([
        (FIXTURES / 'lobby-success.wire').read_bytes(),
        packet(103, (FIXTURES / 'world-list-basic.payload').read_bytes()),
        packet(29),
        packet(103, (FIXTURES / 'world-list-counts.payload').read_bytes()),
    ])


def world_stream():
    packet = incoming_packets()
    success = bytearray((FIXTURES / 'world-success.wire').read_bytes())
    if len(success) < 10:
        raise ValueError('world success fixture is truncated')
    success[8:10] = (1).to_bytes(2, 'big')
    return b''.join([
        bytes(success),
        packet(85, (FIXTURES / WORLD_STATE).read_bytes(), first=True),
        packet(29),
    ])


def stream(role):
    if role == 'lobby':
        return lobby_stream()
    if role == 'world':
        return world_stream()
    raise ValueError('unknown fixture role')


def handle(connection, role, fragment_delay=0.02):
    with connection:
        connection.settimeout(30)
        if exact(connection, 1) != b'\x0e':
            raise ValueError('fixture handshake mismatch')
        connection.sendall(b'\x00')
        header = exact(connection, 3)
        request = header + exact(connection, int.from_bytes(header[1:], 'big'))
        if request != (FIXTURES / REQUESTS[role]).read_bytes():
            raise ValueError('fixture request mismatch')
        print(json.dumps({
            'nativeRequestMatched': True, 'fixtureOnly': True,
            'fixtureRole': role, 'requestFixture': REQUESTS[role],
        }), flush=True)
        wire = stream(role)
        at = 0
        for length in [1, 2, 5, 13, 127, len(wire)]:
            if at == len(wire):
                break
            part = wire[at:at + length]
            connection.sendall(part)
            at += len(part)
            if fragment_delay:
                time.sleep(fragment_delay)
        if at != len(wire):
            raise ValueError('fixture stream incomplete')
        print(json.dumps({
            'fixtureBytesSent': len(wire), 'fixtureOnly': True,
            'fixtureRole': role,
        }), flush=True)
        while connection.recv(65536):
            pass


def listener(port, role):
    listener = socket.socket()
    try:
        listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        listener.bind(('127.0.0.1', port))
        listener.listen(8)
        listener.settimeout(1)
    except OSError:
        listener.close()
        raise
    print(json.dumps({
        'fixtureListening': port, 'loopbackOnly': True,
        'fixtureRole': role, 'fixtureOnly': True,
    }), flush=True)
    return listener


def serve(listener, role, expires):
    def run(current):
        try:
            handle(current, role)
        except (ValueError, OSError):
            print(json.dumps({
                'fixtureConnectionEnded': True, 'fixtureRole': role,
                'fixtureOnly': True,
            }), flush=True)

    while time.monotonic() < expires:
        try:
            connection, _ = listener.accept()
        except TimeoutError:
            continue
        threading.Thread(target=run, args=(connection,), daemon=True).start()


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def decoded_opcode(wire, position, key):
    words = (FIXTURES / 'isaac-incoming.words').read_bytes()

    def byte():
        nonlocal position, key
        word = int.from_bytes(words[key * 4:key * 4 + 4], 'big')
        value = (wire[position] - word) & 255
        position += 1
        key += 1
        return value

    first = byte()
    opcode = first if first < 128 else ((first - 128) << 8) | byte()
    return opcode, position, key


def check_streams():
    lobby = lobby_stream()
    success = (FIXTURES / 'lobby-success.wire').read_bytes()
    require(lobby.startswith(success), 'lobby success differs')
    position = len(success)
    key = 0
    for expected_opcode, filename in [(103, 'world-list-basic.payload'), (29, None), (103, 'world-list-counts.payload')]:
        opcode, position, key = decoded_opcode(lobby, position, key)
        require(opcode == expected_opcode, 'lobby ISAAC opcode differs')
        if filename is not None:
            length = int.from_bytes(lobby[position:position + 2], 'big')
            position += 2
            require(lobby[position:position + length] == (FIXTURES / filename).read_bytes(), 'lobby payload differs')
            position += length
    require(position == len(lobby) and key == 3, 'lobby stream boundary differs')
    world = world_stream()
    success = (FIXTURES / 'world-success.wire').read_bytes()
    require(world[:8] == success[:8] and world[8:10] == b'\x00\x01' and world[10:len(success)] == success[10:], 'world success player-index patch differs')
    opcode, position, key = decoded_opcode(world, len(success), 0)
    require(opcode == 85 and key == 1, 'first world opcode did not use incoming word zero')
    length = int.from_bytes(world[position:position + 2], 'big')
    position += 2
    payload = (FIXTURES / WORLD_STATE).read_bytes()
    require(length == len(payload) and world[position:position + length] == payload, 'first world u16 length or payload differs')
    position += length
    opcode, position, key = decoded_opcode(world, position, key)
    require(opcode == 29 and position == len(world) and key == 2, 'ordinary world PING boundary or cipher differs')
    for opcode in (29, 128, 32767):
        packet = incoming_packets()(opcode, b'\x80\xff', first=True)
        decoded, position, key = decoded_opcode(packet, 0, 0)
        require(decoded == opcode and key == (1 if opcode < 128 else 2), 'first smart opcode differs')
        require(packet[position:] == b'\x00\x02\x80\xff', 'first packet must always have a plain u16 length')
    return len(lobby), len(world)


def check_connection(role, failure=None):
    client, server = socket.socketpair()
    errors = []

    def run():
        try:
            handle(server, role, fragment_delay=0)
        except (ValueError, OSError) as error:
            errors.append(error)

    worker = threading.Thread(target=run, daemon=True)
    worker.start()
    with client:
        client.settimeout(5)
        client.sendall(b'\x0e')
        require(exact(client, 1) == b'\x00', 'fixture handshake response differs')
        request = bytearray((FIXTURES / REQUESTS[role]).read_bytes())
        if failure == 'mismatch':
            request[-1] ^= 1
        if failure == 'truncated':
            request.pop()
        client.sendall(request)
        if failure is not None:
            client.shutdown(socket.SHUT_WR)
            require(client.recv(1) == b'', 'invalid fixture request received a success')
        else:
            wire = stream(role)
            require(exact(client, len(wire)) == wire, 'socketpair stream differs')
            client.shutdown(socket.SHUT_WR)
            require(client.recv(1) == b'', 'unexpected fixture suffix')
    worker.join(timeout=5)
    require(not worker.is_alive(), 'fixture handler did not stop')
    require(len(errors) == (0 if failure is None else 1), 'fixture request validation differs')
    if errors:
        require(isinstance(errors[0], ValueError), 'fixture rejected request with an unexpected socket error')


def check():
    lobby_bytes, world_bytes = check_streams()
    for role in REQUESTS:
        check_connection(role)
        check_connection(role)
        check_connection(role, 'mismatch')
        check_connection(role, 'truncated')
    print(json.dumps({
        'fixtureSelfTestPassed': True, 'socketPairOnly': True,
        'lobbyConnectionsChecked': 2, 'worldConnectionsChecked': 2,
        'rejectedRequests': 4, 'lobbyStreamBytes': lobby_bytes,
        'worldStreamBytes': world_bytes, 'fixtureOnly': True,
    }), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', '--lobby-port', dest='lobby_port', type=int, default=18200)
    parser.add_argument('--world-port', type=int, default=18201)
    parser.add_argument('--check', action='store_true')
    arguments = parser.parse_args()
    if arguments.check:
        check()
    else:
        if not (1 <= arguments.lobby_port <= 65535 and 1 <= arguments.world_port <= 65535):
            parser.error('fixture ports must be within 1..=65535')
        if arguments.lobby_port == arguments.world_port:
            parser.error('lobby and world fixture ports must differ')
        with listener(arguments.lobby_port, 'lobby') as lobby, listener(arguments.world_port, 'world') as world:
            expires = time.monotonic() + 600
            worker = threading.Thread(target=serve, args=(lobby, 'lobby', expires), daemon=True)
            worker.start()
            serve(world, 'world', expires)
            worker.join()

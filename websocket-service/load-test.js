import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const otdLatency = new Trend('ot_round_trip_latency');
const messageCounter = new Counter('total_messages_sent');

export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'ot_round_trip_latency': ['p(95) < 100'],
    },
};

// Helper function to build standard STOMP frames
function buildStompFrame(command, headers, body) {
    let frame = command + '\n';
    for (const key in headers) {
        frame += key + ':' + headers[key] + '\n';
    }
    frame += '\n';
    if (body) {
        frame += (typeof body === 'string' ? body : JSON.stringify(body));
    }
    frame += '\x00'; // STOMP frames must end with a null byte
    return frame;
}

export default function () {
    const url = 'ws://localhost:8082/ws/websocket';
    const token = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZ3JrcmlzaG5hMTIzQGdtYWlsLmNvbSIsImlhdCI6MTc5MTAyNjcxOSwiZXhwIjoxNzkxMDI3NjE5LCJ1c2VySWQiOiJhNjg4N2QwOS1hMjViLTQ5ODAtYWQ2ZC04YTdlY2Q1NGM4ZDIifQ.YaNyvPteWp3oeXEO2iBBN2AxRn2fXn3-2Nj63pxHyZIpBDRoJCU3buNBf_95cl94hdSGYvbCYRyXX5AuW_fHAg';
    const userId = 'a6887d09-a25b-4980-ad6d-8a7ecd54c8d2';

    // 1. Define 5 valid, distinct UUIDs for 5 different workspaces
    const workspaces = [
        '11111111-e89b-12d3-a456-426614174000',
        '22222222-e89b-12d3-a456-426614174000',
        '33333333-e89b-12d3-a456-426614174000',
        '44444444-e89b-12d3-a456-426614174000',
        '55555555-e89b-12d3-a456-426614174000'
    ];

    // 2. Deterministically assign this Virtual User (VU) to one of the 5 workspaces
    // __VU starts at 1, so we subtract 1 and use modulo to round-robin them.
    const workspaceId = workspaces[(__VU - 1) % workspaces.length];

    const params = {
        headers: {
            'Authorization': `Bearer ${token}`
        },
    };

    const res = ws.connect(url, params, function (socket) {

        let isStompConnected = false;

        socket.on('open', function () {
            console.log(`VU ${__VU}: STOMP Handshake Successful -> Joined Workspace ${workspaceId.split('-')[0]}`);

            const connectFrame = buildStompFrame('CONNECT', {
                'accept-version': '1.1,1.0',
                'heart-beat': '10000,10000',
                'Authorization': `Bearer ${token}`
            }, null);

            socket.send(connectFrame);
        });

        socket.on('message', function (message) {
            // Step 2: Listen for the server's CONNECTED response
            if (message.startsWith('CONNECTED')) {
                isStompConnected = true;
                console.log(`VU ${__VU}: STOMP Handshake Successful`);

                // Step 3: Start blasting Code Operations every 1 second
                socket.setInterval(function () {
                    if (!isStompConnected) return;

                    const payload = {
                        version: __ITER,
                        userId: userId,
                        operationType: 'INSERT',
                        position: 0,
                        code: 'H',
                        length: 1,
                        language: 'JAVA'
                    };

                    const sendFrame = buildStompFrame('SEND', {
                        // Notice the /app prefix required by your MessageBrokerRegistry
                        'destination': `/app/workspace/${workspaceId}/code`,
                        'content-type': 'application/json'
                    }, payload);

                    const startT = Date.now();
                    socket.send(sendFrame);
                    messageCounter.add(1);

                    socket.setTimeout(function () {
                        const duration = Date.now() - startT;
                        otdLatency.add(duration);
                    }, 50);

                }, 1000);
            }
        });

        socket.on('close', function () {
            // Graceful shutdown
        });

        socket.on('error', function (e) {
            if (e.error() != "websocket: close sent") {
                console.log(`VU ${__VU}: WebSocket Error: `, e.error());
            }
        });

        sleep(15);
    });

    check(res, { 'status is 101 (Switching Protocols)': (r) => r && r.status === 101 });
}
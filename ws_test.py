import asyncio
import websockets
import socket

def get_local_ip():
    # Get the local IP address of the machine
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Doesn't need to be reachable, just used to get local IP
        s.connect(('10.255.255.255', 1))
        local_ip = s.getsockname()[0]
    except Exception:
        local_ip = '127.0.0.1'
    finally:
        s.close()
    return local_ip

async def handler(websocket):
    async for message in websocket:
        print(f"Received message: {message}")
        
        if message.lower() == "ping":
            await websocket.send("pong")
            print("Sent: pong")
        else:
            await websocket.send(message)
            print(f"Echoed: {message}")

async def main():
    local_ip = get_local_ip()
    # Use "0.0.0.0" to bind to all network interfaces
    host = "0.0.0.0"
    port = 8080
    
    async with websockets.serve(handler, host, port):
        print(f"WebSocket server started on:")
        print(f"Local access: ws://localhost:{port}")
        print(f"LAN access: ws://{local_ip}:{port}")
        await asyncio.Future()

if __name__ == "__main__":
    asyncio.run(main())
# -*- coding: utf-8 -*-
"""nb-pc-bridge 公网中继服务器。

部署在有公网 IP 的服务器上，让手机在外网也能控制家里的电脑——
电脑端 bridge 主动反连中继挂为 agent，手机连同一个 room 当 client，
中继在 room 内双向透传消息，不解析业务内容（token 端到端鉴权仍在 bridge）。

连接 URL:
  agent (电脑):  ws://relay:8970/agent/<room>?key=<relay_key>
  client (手机): ws://relay:8970/client/<room>?key=<relay_key>

room 由用户自定义（建议用 bridge token 的指纹，保证唯一且不可猜）。
relay_key 是中继自身的准入口令，防止陌生人占用带宽。
一个 room 同时只允许一个 agent；client 可多次连接（断线重连）。

多手机复用：中继给每个 client 分配 cid，client→agent 转发时包成
{"_cid": id, "_data": <原文>}，agent 回 {"_cid": id, "_data": <原文>}，
中继按 cid 投递给对应手机；client 断开时通知 agent {"_cid": id, "_event":"close"}。
这样电脑端可把单条中继连接 demux 成多个独立会话，互不串台。
"""
import argparse
import asyncio
import json
import logging
import os
from urllib.parse import urlparse, parse_qs

import websockets

log = logging.getLogger("nb-relay")

# room -> {"agent": ws or None, "clients": {cid: ws}}
ROOMS = {}
_CID_SEQ = [0]


def _room(room_id):
    return ROOMS.setdefault(room_id, {"agent": None, "clients": {}})


def _next_cid():
    _CID_SEQ[0] += 1
    return f"c{_CID_SEQ[0]}"


async def _relay_to(targets, message):
    """把消息转发给一组目标连接，忽略已断开的。"""
    dead = []
    for ws in list(targets):
        try:
            await ws.send(message)
        except websockets.ConnectionClosed:
            dead.append(ws)
    return dead


async def handle(ws, relay_key):
    path = urlparse(ws.request.path)
    parts = [p for p in path.path.split("/") if p]
    qs = parse_qs(path.query)
    # 校验中继准入口令
    if relay_key and qs.get("key", [""])[0] != relay_key:
        await ws.close(code=4003, reason="relay key invalid")
        return
    if len(parts) != 2 or parts[0] not in ("agent", "client"):
        await ws.close(code=4000, reason="bad path, use /agent/<room> or /client/<room>")
        return
    role, room_id = parts
    room = _room(room_id)

    if role == "agent":
        if room["agent"] is not None:
            await ws.close(code=4009, reason="room already has an agent")
            return
        room["agent"] = ws
        log.info("agent 上线 room=%s", room_id)
        try:
            async for message in ws:
                # 电脑 → 指定手机：消息形如 {"_cid": id, "_data": <原文>}
                try:
                    env = json.loads(message)
                    cid = env.get("_cid")
                    data = env.get("_data")
                except (json.JSONDecodeError, AttributeError):
                    continue
                target = room["clients"].get(cid)
                # 电脑主动带码关闭某手机（如鉴权封禁），透传关闭码
                if env.get("_event") == "server_close":
                    if target is not None:
                        code = env.get("_code") or 1000
                        reason = (env.get("_reason") or "")[:120]
                        try:
                            await target.close(code=code, reason=reason)
                        except websockets.ConnectionClosed:
                            pass
                        room["clients"].pop(cid, None)
                    continue
                if target is not None and data is not None:
                    await _relay_to([target], data)
        except websockets.ConnectionClosed:
            pass
        finally:
            if room["agent"] is ws:
                room["agent"] = None
            log.info("agent 下线 room=%s", room_id)
    else:  # client
        cid = _next_cid()
        room["clients"][cid] = ws
        log.info("client 上线 room=%s cid=%s clients=%d", room_id, cid, len(room["clients"]))
        try:
            async for message in ws:
                # 手机 → 电脑：包上 cid 让电脑端 demux
                agent = room["agent"]
                if agent is None:
                    await ws.close(code=4004, reason="no agent online in this room")
                    return
                await _relay_to([agent], json.dumps({"_cid": cid, "_data": message}))
        except websockets.ConnectionClosed:
            pass
        finally:
            room["clients"].pop(cid, None)
            # 通知电脑端该手机已断开，清理对应虚拟会话
            agent = room["agent"]
            if agent is not None:
                await _relay_to([agent], json.dumps({"_cid": cid, "_event": "close"}))
            if not room["clients"] and room["agent"] is None:
                ROOMS.pop(room_id, None)
            log.info("client 下线 room=%s cid=%s", room_id, cid)


async def serve_forever(host, port, relay_key):
    async def handler(ws):
        await handle(ws, relay_key)
    log.info("nb-relay 监听 ws://%s:%d (room 配对中继)", host, port)
    # ping 保活：TCP 静默掉线时由 ping 超时快速发现并关闭，避免死 agent 套接字长期占着 room
    async with websockets.serve(handler, host, port, max_size=2 ** 22,
                                ping_interval=20, ping_timeout=20):
        await asyncio.Future()


def main():
    parser = argparse.ArgumentParser(description="nb-pc-bridge 公网中继服务器")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8970)
    parser.add_argument("--key", default=os.environ.get("NB_RELAY_KEY", ""),
                        help="中继准入口令（也可用环境变量 NB_RELAY_KEY）")
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    asyncio.run(serve_forever(args.host, args.port, args.key))


if __name__ == "__main__":
    main()

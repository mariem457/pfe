import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { BehaviorSubject } from 'rxjs';

export type TruckLocationMsg = {
  driverId?: number | string;
  truckCode?: string;

  lat?: number;
  lng?: number;
  latitude?: number;
  longitude?: number;

  speedKmh?: number;
  headingDeg?: number;
  speed?: number;
  heading?: number;

  timestamp?: string;
};

export type ConnectOpts = {
  wsUrl: string;
  topic: string;
};

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private client?: Client;

  private trucksSubject = new BehaviorSubject<Map<string, TruckLocationMsg>>(new Map());
  trucks$ = this.trucksSubject.asObservable();

  connect(opts: ConnectOpts): void {
    this.disconnect();

    this.client = new Client({
      brokerURL: opts.wsUrl,
      reconnectDelay: 3000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      debug: (msg) => console.log('[STOMP]', msg),
    });

    this.client.onConnect = () => {
      console.log('[STOMP] connected');
      console.log('[STOMP] subscribing to', opts.topic);

      this.client?.subscribe(opts.topic, (msg: IMessage) => {
        console.log('[WS RAW]', msg.body);

        try {
          const payload = JSON.parse(msg.body) as TruckLocationMsg;
          const id = String(payload.driverId ?? payload.truckCode ?? 'unknown');

          const current = new Map(this.trucksSubject.value);
          current.set(id, payload);
          this.trucksSubject.next(current);

          console.log('[TRUCK LIVE]', id, payload);
        } catch (e) {
          console.error('Invalid WS payload:', e, msg.body);
        }
      });
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

    this.client.onWebSocketError = (e) => {
      console.error('WebSocket error:', e);
    };

    this.client.onWebSocketClose = (e) => {
      console.warn('WebSocket closed:', e);
    };

    this.client.activate();
  }

  disconnect(): void {
    try {
      this.client?.deactivate();
    } catch {}

    this.client = undefined;
  }
}
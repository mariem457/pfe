import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { BehaviorSubject } from 'rxjs';
<<<<<<< HEAD
import { environment } from '../../environments/environment';
import { AlertDto, AlertService } from './alert.service';
=======
>>>>>>> origin/enleve-mqtt

export type TruckLocationMsg = {
  driverId?: number | string;
  truckCode?: string;
<<<<<<< HEAD
=======

>>>>>>> origin/enleve-mqtt
  lat?: number;
  lng?: number;
  latitude?: number;
  longitude?: number;
<<<<<<< HEAD
=======

>>>>>>> origin/enleve-mqtt
  speedKmh?: number;
  headingDeg?: number;
  speed?: number;
  heading?: number;
<<<<<<< HEAD
  timestamp?: string;
};

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private client?: Client;
  private connected = false;
=======

  timestamp?: string;
};

export type ConnectOpts = {
  wsUrl: string;   // مثال: ws://localhost:8081/ws
  topic: string;   // مثال: /topic/truck-locations
};

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private client?: Client;
>>>>>>> origin/enleve-mqtt

  private trucksSubject = new BehaviorSubject<Map<string, TruckLocationMsg>>(new Map());
  trucks$ = this.trucksSubject.asObservable();

<<<<<<< HEAD
  constructor(private alertService: AlertService) {}

  connectAll(): void {
    if (this.connected || this.client?.active) return;

    const api = environment.apiUrl.replace(/^http/, 'ws');
    const wsUrl = `${api}/ws`;

    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      debug: () => {},
    });

    this.client.onConnect = () => {
      this.connected = true;
      console.log('[STOMP] connected:', wsUrl);

      this.client?.subscribe('/topic/truck-locations', (msg: IMessage) => {
        try {
          const payload = JSON.parse(msg.body) as TruckLocationMsg;
=======
  connect(opts: ConnectOpts) {
    this.disconnect();

    this.client = new Client({
      brokerURL: opts.wsUrl,
      reconnectDelay: 2000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      debug: (msg) => console.log('[STOMP]', msg)
    });

    this.client.onConnect = () => {
      this.client?.subscribe(opts.topic, (msg: IMessage) => {
        try {
          const payload = JSON.parse(msg.body) as TruckLocationMsg;

>>>>>>> origin/enleve-mqtt
          const id = String(payload.driverId ?? payload.truckCode ?? 'unknown');
          const current = new Map(this.trucksSubject.value);
          current.set(id, payload);
          this.trucksSubject.next(current);
        } catch (e) {
<<<<<<< HEAD
          console.error('Invalid truck WS payload:', e, msg.body);
        }
      });

      this.client?.subscribe('/topic/alerts', (msg: IMessage) => {
        try {
          const alert = JSON.parse(msg.body) as AlertDto;
          this.alertService.pushRealtimeAlert(alert);
          console.log('[ALERT LIVE]', alert);
        } catch (e) {
          console.error('Invalid alert WS payload:', e, msg.body);
        }
      });

      this.client?.subscribe('/topic/alerts/resolved', (msg: IMessage) => {
        try {
          const alert = JSON.parse(msg.body) as AlertDto;
          this.alertService.pushRealtimeResolved(alert);
          console.log('[ALERT RESOLVED LIVE]', alert);
        } catch (e) {
          console.error('Invalid alert resolved WS payload:', e, msg.body);
=======
          console.error('Invalid WS payload:', e, msg.body);
>>>>>>> origin/enleve-mqtt
        }
      });
    };

<<<<<<< HEAD
   this.client.onWebSocketClose = (event) => {
  this.connected = false;
  console.error('[STOMP] websocket closed:', event);
};

this.client.onWebSocketError = (event) => {
  this.connected = false;
  console.error('[STOMP] websocket error:', event);
};

=======
>>>>>>> origin/enleve-mqtt
    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

<<<<<<< HEAD
    this.client.activate();
  }

  connect(opts: { wsUrl: string; topic: string }): void {
    this.connectAll();
  }

  disconnect(): void {
    try {
      this.client?.deactivate();
    } catch {}
    this.connected = false;
=======
    this.client.onWebSocketError = (e) => {
      console.error('WebSocket error:', e);
    };

    this.client.activate();
  }

  disconnect() {
    try { this.client?.deactivate(); } catch {}
>>>>>>> origin/enleve-mqtt
    this.client = undefined;
  }
}
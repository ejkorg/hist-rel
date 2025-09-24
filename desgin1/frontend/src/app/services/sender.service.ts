import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SenderService {
  private base = '/api/senders';
  constructor(private http: HttpClient) {}

  getQueue(senderId: number, status: string = 'NEW', limit: number = 100): Observable<any> {
    return this.http.get(`${this.base}/${senderId}/queue?status=${status}&limit=${limit}`);
  }

  runNow(senderId: number, limit: number = 100): Observable<any> {
    return this.http.post(`${this.base}/${senderId}/run?limit=${limit}`, {});
  }

  enqueue(senderId: number, payloads: string[], source: string = 'ui_submit'): Observable<any> {
    return this.http.post(`${this.base}/${senderId}/enqueue`, { senderId, payloadIds: payloads, source });
  }
}
import { Component, OnInit } from '@angular/core';
import { SenderService } from '../../services/sender.service';

@Component({
  selector: 'app-queue',
  templateUrl: './queue.component.html'
})
export class QueueComponent implements OnInit {
  items: any[] = [];
  senderId = 22;
  payloadText = ''; // multiline textarea input
  status = 'NEW';
  limit = 100;
  // UI fields to show enqueue results
  lastEnqueuedCount: number | null = null;
  skippedPayloads: string[] = [];
  constructor(private senderService: SenderService) {}
  ngOnInit() { this.refresh(); }

  refresh() {
    this.senderService.getQueue(this.senderId, this.status, this.limit).subscribe((data:any)=> this.items = data || []);
  }

  run() {
    this.senderService.runNow(this.senderId, this.limit).subscribe(()=> this.refresh());
  }

  submitPayloads() {
    // split textarea by newline, comma, or whitespace
    const payloads = this.payloadText.split(/\r?\n|,|;/).map(s => s.trim()).filter(s => s.length > 0);
    if (payloads.length === 0) {
      alert('No payloads provided');
      return;
    }
    this.senderService.enqueue(this.senderId, payloads).subscribe((res:any) => {
      // Expecting { enqueuedCount: number, skippedPayloads: string[] }
      this.lastEnqueuedCount = res?.enqueuedCount ?? null;
      this.skippedPayloads = res?.skippedPayloads ?? [];
      const msgParts: string[] = [];
      if (this.lastEnqueuedCount != null) msgParts.push(`${this.lastEnqueuedCount} enqueued`);
      if (this.skippedPayloads && this.skippedPayloads.length) msgParts.push(`${this.skippedPayloads.length} skipped`);
      // lightweight user feedback
      alert(msgParts.length ? msgParts.join(', ') : 'Submit complete');
      this.payloadText = '';
      this.refresh();
    }, err => {
      alert('Failed to submit: ' + (err?.message || err));
    });
  }
}
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
      alert(res);
      this.payloadText = '';
      this.refresh();
    }, err => {
      alert('Failed to submit: ' + (err?.message || err));
    });
  }
}
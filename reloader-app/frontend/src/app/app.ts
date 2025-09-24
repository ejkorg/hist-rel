import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { HttpClientModule } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
  FormsModule,
  CommonModule,
    MatToolbarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule
  ],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {

  sites: string[] = [];
  selectedSite: string = '';
  senderId: number | null = null;
  numToSend: number = 300;
  result: string = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadSites();
  }

  loadSites() {
    this.http.get<string[]>('http://localhost:8080/api/sites').subscribe(
      data => this.sites = data,
      error => console.error('Error loading sites', error)
    );
  }

  runNow() {
    const payload: any = {
      site: this.selectedSite,
      senderId: this.senderId ? String(this.senderId) : null,
      numberOfDataToSend: String(this.numToSend),
      listFile: '/tmp/list.txt',
      countLimitTrigger: String(600)
    };
    this.http.post('http://localhost:8080/api/reload', payload, { responseType: 'text' }).subscribe(
      data => this.result = data as string,
      error => this.result = 'Error: ' + (error.message || error.statusText)
    );
  }
}

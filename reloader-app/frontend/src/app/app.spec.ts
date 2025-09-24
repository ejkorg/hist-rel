import { TestBed, waitForAsync } from '@angular/core/testing';
import { App } from './app';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';

describe('App component', () => {
  let httpMock: HttpTestingController;
  let component: App;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [App]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    component = TestBed.inject(App);
  }));

  afterEach(() => {
    httpMock.verify();
  });

  it('should load sites on init', () => {
    component.ngOnInit();
    const req = httpMock.expectOne('http://localhost:8080/api/sites');
    expect(req.request.method).toBe('GET');
    req.flush(['A','B']);
    expect(component.sites.length).toBe(2);
  });

  it('runNow should post to /api/reload', () => {
    component.selectedSite = 'EXTERNAL';
    component.senderId = 1;
    component.numToSend = 5;
    component.runNow();
    const req = httpMock.expectOne('http://localhost:8080/api/reload');
    expect(req.request.method).toBe('POST');
    req.flush('OK');
    expect(component.result).toBe('OK');
  });
});
// ...existing tests above

import { TestBed } from '@angular/core/testing';
import { Title, Meta } from '@angular/platform-browser';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { DOCUMENT } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { SeoService } from './seo.service';
import { Subject } from 'rxjs';

describe('SeoService', () => {
  let service: SeoService;
  let titleSpy: jasmine.SpyObj<Title>;
  let metaSpy: jasmine.SpyObj<Meta>;
  let routerEvents$: Subject<any>;
  let routerSpy: any;
  let activatedRouteSpy: any;

  beforeEach(() => {
    titleSpy = jasmine.createSpyObj('Title', ['setTitle']);
    metaSpy = jasmine.createSpyObj('Meta', ['updateTag']);
    routerEvents$ = new Subject();

    routerSpy = {
      events: routerEvents$.asObservable(),
      url: '/test'
    };

    activatedRouteSpy = {
      firstChild: null,
      outlet: 'primary',
      data: new Subject()
    };

    TestBed.configureTestingModule({
      providers: [
        SeoService,
        { provide: Title, useValue: titleSpy },
        { provide: Meta, useValue: metaSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteSpy },
        { provide: DOCUMENT, useValue: document },
        { provide: PLATFORM_ID, useValue: 'browser' }
      ]
    });

    service = TestBed.inject(SeoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('updateSeoTags()', () => {
    it('should set default title and meta when called with empty config', () => {
      service.updateSeoTags();
      expect(titleSpy.setTitle).toHaveBeenCalledWith('OmniCharge — Instant Mobile Recharge');
      expect(metaSpy.updateTag).toHaveBeenCalledWith(jasmine.objectContaining({ name: 'description' }));
      expect(metaSpy.updateTag).toHaveBeenCalledWith(jasmine.objectContaining({ name: 'keywords' }));
    });

    it('should append " | OmniCharge" to custom title', () => {
      service.updateSeoTags({ title: 'Dashboard' });
      expect(titleSpy.setTitle).toHaveBeenCalledWith('Dashboard | OmniCharge');
    });

    it('should set Open Graph tags', () => {
      service.updateSeoTags({ title: 'OG Test', description: 'OG Desc' });
      expect(metaSpy.updateTag).toHaveBeenCalledWith(jasmine.objectContaining({ property: 'og:title', content: 'OG Test | OmniCharge' }));
      expect(metaSpy.updateTag).toHaveBeenCalledWith(jasmine.objectContaining({ property: 'og:description', content: 'OG Desc' }));
    });

    it('should set Twitter Card tags', () => {
      service.updateSeoTags({ title: 'TW Test' });
      expect(metaSpy.updateTag).toHaveBeenCalledWith(jasmine.objectContaining({ name: 'twitter:card', content: 'summary_large_image' }));
      expect(metaSpy.updateTag).toHaveBeenCalledWith(jasmine.objectContaining({ name: 'twitter:title', content: 'TW Test | OmniCharge' }));
    });

    it('should use custom image when provided', () => {
      service.updateSeoTags({ image: 'https://example.com/img.png' });
      expect(metaSpy.updateTag).toHaveBeenCalledWith(jasmine.objectContaining({ property: 'og:image', content: 'https://example.com/img.png' }));
    });
  });
});

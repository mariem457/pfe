import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HeadPagesComponent } from './head-pages.component';

describe('HeadPagesComponent', () => {
  let component: HeadPagesComponent;
  let fixture: ComponentFixture<HeadPagesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeadPagesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HeadPagesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

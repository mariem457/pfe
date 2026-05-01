import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TechnicianAlertsComponent } from './technician-alerts.component';

describe('TechnicianAlertsComponent', () => {
  let component: TechnicianAlertsComponent;
  let fixture: ComponentFixture<TechnicianAlertsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TechnicianAlertsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TechnicianAlertsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
